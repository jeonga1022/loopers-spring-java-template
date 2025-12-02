package com.loopers.domain.order;

import com.loopers.domain.order.strategy.PaymentContext;
import com.loopers.domain.order.strategy.PaymentStrategy;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointAccountDomainService;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointOnlyPaymentStrategyTest {

    @Mock
    PointAccountDomainService pointAccountDomainService;

    @InjectMocks
    PointOnlyPaymentStrategy strategy;

    static final String USER_ID = "user123";
    static final long AMOUNT = 10_000L;

    @Nested
    class ValidatePaymentTest {

        @Test
        @DisplayName("포인트 충분하면 검증 성공")
        void validatePayment1() {
            PaymentContext context = PaymentContext.forPointOnly(USER_ID, AMOUNT);
            Point mockPoint = mock(Point.class);
            when(mockPoint.amount()).thenReturn(20_000L);
            when(pointAccountDomainService.getBalance(USER_ID)).thenReturn(mockPoint);

            strategy.validatePayment(context);

            verify(pointAccountDomainService).getBalance(USER_ID);
        }

        @Test
        @DisplayName("포인트 부족하면 예외 발생")
        void validatePayment2() {
            PaymentContext context = PaymentContext.forPointOnly(USER_ID, AMOUNT);
            Point mockPoint = mock(Point.class);
            when(mockPoint.amount()).thenReturn(5_000L);
            when(pointAccountDomainService.getBalance(USER_ID)).thenReturn(mockPoint);

            assertThatThrownBy(() -> strategy.validatePayment(context))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("포인트가 부족");
        }
    }

    @Nested
    @DisplayName("executePayment()")
    class ExecutePaymentTest {

        @Test
        @DisplayName("포인트 차감 실행")
        void executePayment3() {
            PaymentContext context = PaymentContext.forPointOnly(USER_ID, AMOUNT);

            strategy.executePayment(context);

            verify(pointAccountDomainService).deduct(USER_ID, AMOUNT);
        }
    }

    @Nested
    class GetPaymentTypeTest {

        @Test
        @DisplayName("POINT_ONLY 반환")
        void getPaymentType4() {
            PaymentType type = strategy.getPaymentType();

            assertThat(type).isEqualTo(PaymentType.POINT_ONLY);
        }
    }
}
