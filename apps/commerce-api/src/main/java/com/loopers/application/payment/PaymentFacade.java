package com.loopers.application.payment;

import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {

    private final PaymentDomainService paymentDomainService;
    private final OrderDomainService orderDomainService;

    @Transactional
    public void completePaymentByCallback(String pgTransactionId) {
        log.info("Completing payment by callback. pgTransactionId: {}", pgTransactionId);

        // Payment 완료
        Payment payment = paymentDomainService.getPaymentByPgTransactionId(pgTransactionId);
        paymentDomainService.markAsSuccess(payment.getId(), pgTransactionId);

        // Order 완료
        orderDomainService.confirmOrder(payment.getUserId(), payment.getOrderId());

        log.info("Payment and Order completed. orderId: {}", payment.getOrderId());
    }

    @Transactional
    public void failPaymentByCallback(String pgTransactionId, String reason) {
        log.info("Failing payment by callback. pgTransactionId: {}, reason: {}",
                pgTransactionId, reason);

        // Payment 실패
        Payment payment = paymentDomainService.getPaymentByPgTransactionId(pgTransactionId);
        paymentDomainService.markAsFailed(payment.getId(), reason);

        // Order 실패
        orderDomainService.failOrder(payment.getUserId(), payment.getOrderId());

        log.info("Payment and Order failed. orderId: {}", payment.getOrderId());
    }
}
