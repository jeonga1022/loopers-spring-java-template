package com.loopers.domain.order;

import com.loopers.domain.order.strategy.PaymentContext;
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
class MixedPaymentStrategyTest {

    @Mock
    PointAccountDomainService pointAccountDomainService;

    @InjectMocks
    MixedPaymentStrategy strategy;

    static final String USER_ID = "user123";
    static final long TOTAL_AMOUNT = 10_000L;
    static final long POINT_AMOUNT = 6_000L;
    static final long CARD_AMOUNT = 4_000L;

    @Nested
    class ValidatePaymentTest {

        @Test
        @DisplayName("포인트 충분하고 금액 합계 맞으면 검증 성공")
        void validatePayment1() {
            PaymentContext context = PaymentContext.forMixed(USER_ID, TOTAL_AMOUNT, POINT_AMOUNT, CARD_AMOUNT);
            Point mockPoint = mock(Point.class);
            when(mockPoint.amount()).thenReturn(10_000L);
            when(pointAccountDomainService.getBalance(USER_ID)).thenReturn(mockPoint);

            strategy.validatePayment(context);

            verify(pointAccountDomainService).getBalance(USER_ID);
        }

        @Test
        @DisplayName("포인트 부족하면 예외 발생")
        void validatePayment2() {
            PaymentContext context = PaymentContext.forMixed(USER_ID, TOTAL_AMOUNT, POINT_AMOUNT, CARD_AMOUNT);
            Point mockPoint = mock(Point.class);
            when(mockPoint.amount()).thenReturn(5_000L);
            when(pointAccountDomainService.getBalance(USER_ID)).thenReturn(mockPoint);

            assertThatThrownBy(() -> strategy.validatePayment(context))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("포인트가 부족");
        }

        @Test
        @DisplayName("포인트와 카드 금액의 합이 총액과 맞지 않으면 예외 발생")
        void validatePayment3() {
            PaymentContext context = PaymentContext.forMixed(USER_ID, TOTAL_AMOUNT, 5_000L, 4_000L);
            Point mockPoint = mock(Point.class);
            when(mockPoint.amount()).thenReturn(10_000L);
            when(pointAccountDomainService.getBalance(USER_ID)).thenReturn(mockPoint);

            assertThatThrownBy(() -> strategy.validatePayment(context))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("합이 총액과 일치");
        }
    }

    @Nested
    class ExecutePaymentTest {

        @Test
        @DisplayName("포인트 차감 실행")
        void executePayment1() {
            PaymentContext context = PaymentContext.forMixed(USER_ID, TOTAL_AMOUNT, POINT_AMOUNT, CARD_AMOUNT);

            strategy.executePayment(context);

            verify(pointAccountDomainService).deduct(USER_ID, POINT_AMOUNT);
        }
    }

    @Nested
    class GetPaymentTypeTest {

        @Test
        @DisplayName("MIXED 반환")
        void getPaymentType1() {
            PaymentType type = strategy.getPaymentType();

            assertThat(type).isEqualTo(PaymentType.MIXED);
        }
    }
}
