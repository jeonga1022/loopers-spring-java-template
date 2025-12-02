package com.loopers.domain.order;

import com.loopers.domain.order.strategy.PaymentStrategy;
import com.loopers.domain.point.PointAccountDomainService;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaymentStrategyRegistryTest {

    @Mock
    PointAccountDomainService pointAccountDomainService;

    @Nested
    class GetStrategyTest {

        @Test
        @DisplayName("등록된 전략을 올바르게 조회한다")
        void getStrategyTest1() {
            PointOnlyPaymentStrategy pointStrategy = new PointOnlyPaymentStrategy(pointAccountDomainService);
            MixedPaymentStrategy mixedStrategy = new MixedPaymentStrategy(pointAccountDomainService);

            PaymentStrategyRegistry registry = new PaymentStrategyRegistry(List.of(pointStrategy, mixedStrategy));

            PaymentStrategy retrieved = registry.getStrategy(PaymentType.POINT_ONLY);
            assertThat(retrieved).isEqualTo(pointStrategy);

            retrieved = registry.getStrategy(PaymentType.MIXED);
            assertThat(retrieved).isEqualTo(mixedStrategy);
        }

        @Test
        @DisplayName("등록되지 않은 전략 조회 시 예외 발생")
        void getStrategyTest2() {
            PointOnlyPaymentStrategy pointStrategy = new PointOnlyPaymentStrategy(pointAccountDomainService);

            PaymentStrategyRegistry registry = new PaymentStrategyRegistry(List.of(pointStrategy));

            assertThatThrownBy(() -> registry.getStrategy(PaymentType.CARD_ONLY))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("지원하지 않는 결제 방식");
        }
    }
}
