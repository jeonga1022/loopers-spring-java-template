package com.loopers.domain.order.event;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.OrderDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderDomainService orderDomainService;
    private final OrderFacade orderFacade;

    @EventListener
    public void handlePaymentSucceeded(PaymentSucceededEvent event) {
        log.info("결제 성공 이벤트 수신: orderId={}, paymentId={}", event.getOrderId(), event.getPaymentId());
        orderDomainService.confirmOrder(event.getUserId(), event.getOrderId());
        log.info("주문 확정 완료: orderId={}", event.getOrderId());
    }

    @EventListener
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("결제 실패 이벤트 수신: orderId={}, reason={}", event.getOrderId(), event.getReason());
        orderFacade.handlePaymentFailure(event.getUserId(), event.getOrderId());
        log.info("주문 실패 처리 완료 (재고 복구 포함): orderId={}", event.getOrderId());
    }
}
