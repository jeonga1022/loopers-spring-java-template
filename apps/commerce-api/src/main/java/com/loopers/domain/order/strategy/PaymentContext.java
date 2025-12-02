package com.loopers.domain.order.strategy;

public record PaymentContext(
    String userId,
    long totalAmount,
    long pointAmount,
    long cardAmount
) {
    public static PaymentContext forPointOnly(String userId, long totalAmount) {
        return new PaymentContext(userId, totalAmount, totalAmount, 0);
    }

    public static PaymentContext forCardOnly(String userId, long totalAmount) {
        return new PaymentContext(userId, totalAmount, 0, totalAmount);
    }

    public static PaymentContext forMixed(String userId, long totalAmount, long pointAmount, long cardAmount) {
        return new PaymentContext(userId, totalAmount, pointAmount, cardAmount);
    }
}
