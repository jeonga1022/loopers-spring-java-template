package com.loopers.application.order;

import com.loopers.application.payment.PaymentCompensationService;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponDomainService;
import com.loopers.domain.coupon.event.CouponUsedEvent;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentDomainService;
import com.loopers.domain.order.PaymentType;
import com.loopers.domain.order.strategy.PaymentContext;
import com.loopers.domain.order.strategy.PaymentStrategy;
import com.loopers.domain.order.strategy.PaymentStrategyFactory;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.interfaces.api.order.OrderDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderDomainService orderDomainService;
    private final ProductDomainService productDomainService;
    private final ProductCacheService productCacheService;
    private final PaymentDomainService paymentDomainService;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final PaymentCompensationService paymentCompensationService;
    private final CouponDomainService couponDomainService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderInfo createOrder(OrderCreateCommand command) {
        validateItem(command.items());

        List<OrderDto.OrderItemRequest> sortedItems = command.items().stream()
                .sorted(Comparator.comparing(OrderDto.OrderItemRequest::productId))
                .toList();

        long totalAmount = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderDto.OrderItemRequest itemRequest : sortedItems) {
            Product product = productDomainService.decreaseStock(
                    itemRequest.productId(),
                    itemRequest.quantity()
            );

            productCacheService.deleteProductDetail(itemRequest.productId());

            totalAmount += product.getPrice() * itemRequest.quantity();

            orderItems.add(OrderItem.create(
                    product.getId(),
                    product.getName(),
                    itemRequest.quantity(),
                    product.getPrice()
            ));
        }

        long discountAmount = 0;
        if (command.hasCoupon()) {
            Coupon coupon = couponDomainService.validateAndGetCoupon(command.userId(), command.couponId());
            discountAmount = Math.min(coupon.getDiscountAmount(), totalAmount);
        }

        Order order = orderDomainService.createOrder(
                command.userId(), orderItems, totalAmount, command.couponId(), discountAmount);

        long paymentAmount = order.getPaymentAmount();

        if (paymentAmount == 0) {
            executeFreeOrder(command, order, discountAmount);
            return OrderInfo.from(order);
        }

        PaymentType paymentType = command.isCardPayment() ? PaymentType.CARD_ONLY : PaymentType.POINT_ONLY;

        Payment payment = paymentDomainService.createPayment(
                order.getId(),
                command.userId(),
                paymentAmount,
                paymentType
        );

        order.startPayment();

        if (paymentType == PaymentType.POINT_ONLY) {
            executePointPayment(command, order, payment, paymentAmount, discountAmount);
        } else {
            scheduleCardPayment(command, order, payment, paymentAmount, discountAmount);
        }

        return OrderInfo.from(order);
    }

    private void executeFreeOrder(OrderCreateCommand command, Order order, long discountAmount) {
        order.startPayment();
        order.confirm();
        publishCouponUsedEvent(command, order.getId(), discountAmount);
        publishOrderCompletedEvent(order);
    }

    private void executePointPayment(OrderCreateCommand command, Order order, Payment payment,
                                      long paymentAmount, long discountAmount) {
        PaymentContext context = PaymentContext.builder()
                .orderId(order.getId())
                .paymentId(payment.getId())
                .userId(command.userId())
                .totalAmount(paymentAmount)
                .pointAmount(paymentAmount)
                .cardAmount(0)
                .build();

        PaymentStrategy strategy = paymentStrategyFactory.create(PaymentType.POINT_ONLY);
        strategy.executePayment(context);

        order.confirm();
        paymentDomainService.markAsSuccess(payment.getId(), "internal-payment");

        publishCouponUsedEvent(command, order.getId(), discountAmount);
        publishOrderCompletedEvent(order);
    }

    private void scheduleCardPayment(OrderCreateCommand command, Order order, Payment payment,
                                      long paymentAmount, long discountAmount) {
        Long orderId = order.getId();
        Long paymentId = payment.getId();
        String userId = command.userId();
        CardInfo cardInfo = command.cardInfo();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                PaymentContext context = PaymentContext.builder()
                        .orderId(orderId)
                        .paymentId(paymentId)
                        .userId(userId)
                        .totalAmount(paymentAmount)
                        .pointAmount(0)
                        .cardAmount(paymentAmount)
                        .cardType(cardInfo.cardType())
                        .cardNo(cardInfo.cardNo())
                        .build();

                try {
                    PaymentStrategy strategy = paymentStrategyFactory.create(PaymentType.CARD_ONLY);
                    strategy.executePayment(context);

                    if (command.hasCoupon()) {
                        eventPublisher.publishEvent(CouponUsedEvent.of(
                                command.couponId(), orderId, userId, discountAmount));
                    }
                } catch (Exception e) {
                    log.error("PG payment failed. orderId: {}, error: {}", orderId, e.getMessage(), e);
                    paymentCompensationService.compensateFailedPayment(userId, orderId, paymentId, e.getMessage());
                }
            }
        });
    }

    private void publishCouponUsedEvent(OrderCreateCommand command, Long orderId, long discountAmount) {
        if (command.hasCoupon()) {
            Long couponId = command.couponId();
            String userId = command.userId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(CouponUsedEvent.of(couponId, orderId, userId, discountAmount));
                }
            });
        }
    }

    private void publishOrderCompletedEvent(Order order) {
        OrderCompletedEvent event = OrderCompletedEvent.from(order);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(event);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> getOrders(String userId) {
        List<Order> orders = orderDomainService.getOrders(userId);
        return orders.stream()
                .map(OrderInfo::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderInfo getOrder(String userId, Long orderId) {
        Order order = orderDomainService.getOrder(userId, orderId);
        return OrderInfo.from(order);
    }

    @Transactional
    public void handlePaymentFailure(String userId, Long orderId) {
        Order order = orderDomainService.getOrder(userId, orderId);

        if (order.getStatus() != OrderStatus.PAYING) {
            log.warn("Order is not in PAYING status. orderId: {}, status: {}", orderId, order.getStatus());
            return;
        }

        List<OrderItem> items = new ArrayList<>(order.getOrderItems());
        items.sort(Comparator.comparing(OrderItem::getProductId).reversed());

        for (OrderItem item : items) {
            productDomainService.increaseStock(item.getProductId(), item.getQuantity());
            productCacheService.deleteProductDetail(item.getProductId());
        }

        order.fail();
    }

    private static void validateItem(List<OrderDto.OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "하나 이상의 상품을 주문해야 합니다.");
        }
    }
}
