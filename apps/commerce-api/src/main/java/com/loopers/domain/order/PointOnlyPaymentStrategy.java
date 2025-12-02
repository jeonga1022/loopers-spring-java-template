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
public class PointOnlyPaymentStrategy implements PaymentStrategy {

    private final PointAccountDomainService pointAccountDomainService;

    @Override
    public void validatePayment(PaymentContext context) {
        var balance = pointAccountDomainService.getBalance(context.userId());
        if (balance == null || balance.amount() < context.totalAmount()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다");
        }
    }

    @Override
    public void executePayment(PaymentContext context) {
        pointAccountDomainService.deduct(context.userId(), context.totalAmount());
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.POINT_ONLY;
    }
}
