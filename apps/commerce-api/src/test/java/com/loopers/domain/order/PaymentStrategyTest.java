package com.loopers.domain.order;

import com.loopers.domain.order.strategy.PaymentContext;
import com.loopers.domain.order.strategy.PaymentStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;


class PaymentStrategyTest {

    static final String USER_ID = "user123";
    static final long TOTAL_AMOUNT = 10_000L;

    @Nested
    class PaymentContextTest {

        @Test
        @DisplayName("포인트 전용 컨텍스트 생성")
        void contextTest1() {
            PaymentContext context = PaymentContext.forPointOnly(USER_ID, TOTAL_AMOUNT);

            assertThat(context.userId()).isEqualTo(USER_ID);
            assertThat(context.totalAmount()).isEqualTo(TOTAL_AMOUNT);
            assertThat(context.pointAmount()).isEqualTo(TOTAL_AMOUNT);
            assertThat(context.cardAmount()).isEqualTo(0);
        }

        @Test
        @DisplayName("카드 전용 컨텍스트 생성")
        void contextTest2() {
            PaymentContext context = PaymentContext.forCardOnly(USER_ID, TOTAL_AMOUNT);

            assertThat(context.userId()).isEqualTo(USER_ID);
            assertThat(context.totalAmount()).isEqualTo(TOTAL_AMOUNT);
            assertThat(context.pointAmount()).isEqualTo(0);
            assertThat(context.cardAmount()).isEqualTo(TOTAL_AMOUNT);
        }

        @Test
        @DisplayName("혼합 결제 컨텍스트 생성")
        void contextTest3() {
            long pointAmount = 6_000L;
            long cardAmount = 4_000L;

            PaymentContext context = PaymentContext.forMixed(USER_ID, TOTAL_AMOUNT, pointAmount, cardAmount);

            assertThat(context.userId()).isEqualTo(USER_ID);
            assertThat(context.totalAmount()).isEqualTo(TOTAL_AMOUNT);
            assertThat(context.pointAmount()).isEqualTo(pointAmount);
            assertThat(context.cardAmount()).isEqualTo(cardAmount);
        }

        @Test
        @DisplayName("PaymentContext는 불변이다")
        void contextTest4() {
            PaymentContext context = PaymentContext.forPointOnly(USER_ID, TOTAL_AMOUNT);

            assertThat(context).isInstanceOf(Record.class);
        }
    }

    @Nested
    class PaymentStrategyContractTest {

        @Test
        @DisplayName("PaymentStrategy는 검증, 실행, 타입 조회 메서드를 제공한다")
        void paymentStrategyContractTest1() {
            assertThat(PaymentStrategy.class.isInterface()).isTrue();

            var methods = PaymentStrategy.class.getDeclaredMethods();
            assertThat(methods).extracting(java.lang.reflect.Method::getName)
                .contains("validatePayment", "executePayment", "getPaymentType");
        }
    }
}
