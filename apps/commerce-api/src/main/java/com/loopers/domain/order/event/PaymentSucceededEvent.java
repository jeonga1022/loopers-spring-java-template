package com.loopers.domain.order.event;

import java.time.LocalDateTime;

public class PaymentSucceededEvent {

    private final Long orderId;
    private final Long paymentId;
    private final String userId;
    private final String pgTransactionId;
    private final LocalDateTime occurredAt;

    private PaymentSucceededEvent(Long orderId, Long paymentId, String userId, String pgTransactionId) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.userId = userId;
        this.pgTransactionId = pgTransactionId;
        this.occurredAt = LocalDateTime.now();
    }

    public static PaymentSucceededEvent of(Long orderId, Long paymentId, String userId, String pgTransactionId) {
        return new PaymentSucceededEvent(orderId, paymentId, userId, pgTransactionId);
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
