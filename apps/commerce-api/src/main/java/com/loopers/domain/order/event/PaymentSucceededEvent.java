package com.loopers.domain.order.event;

import com.loopers.domain.order.Payment;

import java.time.LocalDateTime;

public class PaymentSucceededEvent {

    private final Long orderId;
    private final Long paymentId;
    private final String userId;
    private final String pgTransactionId;
    private final LocalDateTime occurredAt;

    private PaymentSucceededEvent(Payment payment, String pgTransactionId) {
        this.orderId = payment.getOrderId();
        this.paymentId = payment.getId();
        this.userId = payment.getUserId();
        this.pgTransactionId = pgTransactionId;
        this.occurredAt = LocalDateTime.now();
    }

    public static PaymentSucceededEvent from(Payment payment, String pgTransactionId) {
        return new PaymentSucceededEvent(payment, pgTransactionId);
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getUserId() {
        return userId;
    }

    public String getPgTransactionId() {
        return pgTransactionId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
