package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderDomainService;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderDomainService orderDomainService;
    private final ProductDomainService productDomainService;
    private final ProductCacheService productCacheService;
    private final PaymentDomainService paymentDomainService;
    private final PaymentStrategyFactory paymentStrategyFactory;

    @Transactional
    public OrderInfo createOrder(String userId, List<OrderDto.OrderItemRequest> itemRequests) {
        return createOrder(userId, itemRequests, PaymentType.POINT_ONLY);
    }

    @Transactional
    public OrderInfo createOrder(
        String userId,
        List<OrderDto.OrderItemRequest> itemRequests,
        PaymentType paymentType
    ) {
        validateItem(itemRequests);

        // 락 순서
        List<OrderDto.OrderItemRequest> sortedItems = itemRequests.stream()
                .sorted(Comparator.comparing(OrderDto.OrderItemRequest::productId))
                .toList();

        // 상품 재고 차감
        long totalAmount = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderDto.OrderItemRequest itemRequest : sortedItems) {

            Product product = productDomainService.decreaseStock(
                    itemRequest.productId(),
                    itemRequest.quantity()
            );

            // 재고 변경되었으니 해당 상품의 detail 캐시 무효화
            productCacheService.deleteProductDetail(itemRequest.productId());

            totalAmount += product.getPrice() * itemRequest.quantity();

            orderItems.add(OrderItem.create(
                    product.getId(),
                    product.getName(),
                    itemRequest.quantity(),
                    product.getPrice()
            ));
        }

        // 주문 생성
        Order order = orderDomainService.createOrder(userId, orderItems, totalAmount);

        // Payment 생성
        Payment payment = paymentDomainService.createPayment(
            order.getId(),
            userId,
            totalAmount,
            paymentType
        );

        // 결제 실행
        order.startPayment();

        PaymentStrategy strategy = paymentStrategyFactory.create(paymentType);
        PaymentContext context = strategy.build(userId, totalAmount);

        try {
            strategy.executePayment(context);
            payment.markAsSuccess("internal-payment");
            order.confirm();
        } catch (CoreException e) {
            payment.markAsFailed(e.getMessage());
            throw e;
        }

        return OrderInfo.from(order);
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

    private static void validateItem(List<OrderDto.OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "하나 이상의 상품을 주문해야 합니다."
            );
        }
    }
}
