package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentType;

public interface PaymentStrategy {

    /**
     * 결제 가능 여부 검증
     */
    void validatePayment(PaymentContext context);

    /**
     * 결제 실행
     */
    void executePayment(PaymentContext context);

    /**
     * 결제 수단 반환
     */
    PaymentType getPaymentType();
}
