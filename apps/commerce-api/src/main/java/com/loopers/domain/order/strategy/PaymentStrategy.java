package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentType;

public interface PaymentStrategy {

    boolean supports(PaymentType paymentType);

    PaymentContext build(String userId, long totalAmount);

    void executePayment(PaymentContext context);
}