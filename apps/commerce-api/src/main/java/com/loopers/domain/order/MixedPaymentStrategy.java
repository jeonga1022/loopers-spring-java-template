package com.loopers.domain.order;

import com.loopers.domain.order.strategy.PaymentContext;
import com.loopers.domain.order.strategy.PaymentStrategy;
import com.loopers.domain.point.PointAccountDomainService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MixedPaymentStrategy implements PaymentStrategy {

    private final PointAccountDomainService pointAccountDomainService;

    @Override
    public void validatePayment(PaymentContext context) {
        validatePointAmount(context);
        validatePaymentAmountSum(context);
    }

    private void validatePointAmount(PaymentContext context) {
        var balance = pointAccountDomainService.getBalance(context.userId());
        if (balance == null || balance.amount() < context.pointAmount()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다");
        }
    }

    private void validatePaymentAmountSum(PaymentContext context) {
        if (context.pointAmount() + context.cardAmount() != context.totalAmount()) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "포인트와 카드 결제 금액의 합이 총액과 일치하지 않습니다");
        }

        if (context.pointAmount() < 0 || context.cardAmount() < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "결제 금액은 0 이상이어야 합니다");
        }
    }

    @Override
    public void executePayment(PaymentContext context) {
        pointAccountDomainService.deduct(context.userId(), context.pointAmount());
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.MIXED;
    }
}
