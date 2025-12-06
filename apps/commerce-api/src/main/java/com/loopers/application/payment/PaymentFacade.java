package com.loopers.application.payment;

import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentDomainService;
import com.loopers.domain.order.PaymentStatus;
import com.loopers.domain.order.event.PaymentFailedEvent;
import com.loopers.domain.order.event.PaymentSucceededEvent;
import com.loopers.infrastructure.pg.PgStatus;
import com.loopers.infrastructure.pg.PgTransactionService;
import com.loopers.infrastructure.pg.dto.PgTransactionDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {

    private final PaymentDomainService paymentDomainService;
    private final OrderDomainService orderDomainService;
    private final PgTransactionService pgTransactionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void completePaymentByCallback(String pgTransactionId) {
        log.info("Completing payment by callback. pgTransactionId: {}", pgTransactionId);

        Payment payment = paymentDomainService.getPaymentByPgTransactionId(pgTransactionId);
        paymentDomainService.markAsSuccess(payment.getId(), pgTransactionId);

        eventPublisher.publishEvent(PaymentSucceededEvent.of(
                payment.getOrderId(),
                payment.getId(),
                payment.getUserId(),
                pgTransactionId
        ));

        log.info("Payment completed, event published. orderId: {}", payment.getOrderId());
    }

    @Transactional
    public void failPaymentByCallback(String pgTransactionId, String reason) {
        log.info("Failing payment by callback. pgTransactionId: {}, reason: {}",
                pgTransactionId, reason);

        Payment payment = paymentDomainService.getPaymentByPgTransactionId(pgTransactionId);
        paymentDomainService.markAsFailed(payment.getId(), reason);

        eventPublisher.publishEvent(PaymentFailedEvent.of(
                payment.getOrderId(),
                payment.getId(),
                payment.getUserId(),
                reason
        ));

        log.info("Payment failed, event published. orderId: {}", payment.getOrderId());
    }

    @Transactional(readOnly = true)
    public Payment getPaymentStatus(String userId, Long orderId) {
        orderDomainService.getOrder(userId, orderId);
        return paymentDomainService.getPaymentByOrderId(orderId);
    }

    @Transactional
    public Payment syncPaymentStatusWithPG(String userId, Long orderId) {
        Payment payment = paymentDomainService.getPaymentByOrderId(orderId);

        // Payment가 PENDING이 아니면 동기화할 필요 없음
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment;
        }

        try {
            log.info("Syncing payment status with PG. pgTransactionId: {}", payment.getPgTransactionId());
            PgTransactionDetail detail = pgTransactionService.getTransaction(userId, payment.getPgTransactionId());

            PgStatus pgStatus = PgStatus.from(detail.getStatus());

            if (pgStatus.isSuccess()) {
                paymentDomainService.markAsSuccess(payment.getId(), payment.getPgTransactionId());
                eventPublisher.publishEvent(PaymentSucceededEvent.of(
                        payment.getOrderId(),
                        payment.getId(),
                        userId,
                        payment.getPgTransactionId()
                ));
                log.info("Payment synced to SUCCESS. orderId: {}", orderId);
            } else if (pgStatus.isFailed()) {
                paymentDomainService.markAsFailed(payment.getId(), detail.getReason());
                eventPublisher.publishEvent(PaymentFailedEvent.of(
                        payment.getOrderId(),
                        payment.getId(),
                        userId,
                        detail.getReason()
                ));
                log.info("Payment synced to FAILED. orderId: {}", orderId);
            }

            return paymentDomainService.getPaymentByOrderId(orderId);
        } catch (Exception e) {
            log.warn("Failed to sync payment status with PG. pgTransactionId: {}, error: {}",
                    payment.getPgTransactionId(), e.getMessage());
            // PG 조회 실패해도 기존 상태 반환 (에러 확산 방지)
            return payment;
        }
    }
}
