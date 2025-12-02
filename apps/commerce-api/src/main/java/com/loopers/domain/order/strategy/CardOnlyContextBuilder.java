package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentType;
import org.springframework.stereotype.Component;

@Component
public class CardOnlyContextBuilder implements PaymentStrategy {

    @Override
    public boolean supports(PaymentType paymentType) {
        return paymentType == PaymentType.CARD_ONLY;
    }

    @Override
    public PaymentContext build(String userId, long totalAmount) {
        return PaymentContext.forCardOnly(userId, totalAmount);
    }

    @Override
    public void executePayment(PaymentContext context) {
        // 외부 PG를 통한 카드 결제 구현
        // 현재는 성공 처리
    }
}