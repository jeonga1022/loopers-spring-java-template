package com.loopers.domain.order.event;

import java.time.LocalDateTime;

public class PaymentFailedEvent {

    private final Long orderId;
    private final Long paymentId;
    private final String userId;
    private final String reason;
    private final LocalDateTime occurredAt;

    private PaymentFailedEvent(Long orderId, Long paymentId, String userId, String reason) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.userId = userId;
        this.reason = reason;
        this.occurredAt = LocalDateTime.now();
    }

    public static PaymentFailedEvent of(Long orderId, Long paymentId, String userId, String reason) {
        return new PaymentFailedEvent(orderId, paymentId, userId, reason);
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

    public String getReason() {
        return reason;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
