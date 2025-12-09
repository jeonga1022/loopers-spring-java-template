package com.loopers.domain.order.event;

import com.loopers.domain.order.Payment;

import java.time.LocalDateTime;

public class PaymentFailedEvent {

    private final Long orderId;
    private final Long paymentId;
    private final String userId;
    private final String reason;
    private final LocalDateTime occurredAt;

    private PaymentFailedEvent(Payment payment, String reason) {
        this.orderId = payment.getOrderId();
        this.paymentId = payment.getId();
        this.userId = payment.getUserId();
        this.reason = reason;
        this.occurredAt = LocalDateTime.now();
    }

    public static PaymentFailedEvent from(Payment payment, String reason) {
        return new PaymentFailedEvent(payment, reason);
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
