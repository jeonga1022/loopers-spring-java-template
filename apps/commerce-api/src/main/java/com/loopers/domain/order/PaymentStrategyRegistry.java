package com.loopers.domain.order;

import com.loopers.domain.order.strategy.PaymentStrategy;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyRegistry {

    private final Map<PaymentType, PaymentStrategy> strategies;

    public PaymentStrategyRegistry(List<PaymentStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                PaymentStrategy::getPaymentType,
                Function.identity()
            ));
    }

    public PaymentStrategy getStrategy(PaymentType paymentType) {
        PaymentStrategy strategy = strategies.get(paymentType);
        if (strategy == null) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "지원하지 않는 결제 방식입니다: " + paymentType);
        }
        return strategy;
    }
}
