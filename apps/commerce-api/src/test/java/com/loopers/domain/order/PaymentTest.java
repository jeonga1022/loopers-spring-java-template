package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Payment 엔티티 테스트")
class PaymentTest {

    static final Long ORDER_ID = 1L;
    static final String USER_ID = "user123";
    static final Long AMOUNT = 10_000L;

    @Nested
    @DisplayName("Payment.create()")
    class CreatePaymentTest {

        @Test
        @DisplayName("정상적인 결제 생성")
        void test1() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);

            assertThat(payment).isNotNull();
            assertThat(payment.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(payment.getUserId()).isEqualTo(USER_ID);
            assertThat(payment.getAmount()).isEqualTo(AMOUNT);
            assertThat(payment.getPaymentType()).isEqualTo(PaymentType.POINT_ONLY);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("0원 결제는 생성 불가")
        void test2() {
            assertThatThrownBy(() -> Payment.create(ORDER_ID, USER_ID, 0L, PaymentType.POINT_ONLY))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("0보다 커야");
        }

        @Test
        @DisplayName("음수 금액은 생성 불가")
        void test3() {
            assertThatThrownBy(() -> Payment.create(ORDER_ID, USER_ID, -1000L, PaymentType.POINT_ONLY))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("0보다 커야");
        }

        @Test
        @DisplayName("null 금액은 생성 불가")
        void test4() {
            assertThatThrownBy(() -> Payment.create(ORDER_ID, USER_ID, null, PaymentType.POINT_ONLY))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("0보다 커야");
        }
    }

    @Nested
    @DisplayName("Payment 상태 전환 - markAsSuccess()")
    class MarkAsSuccessTest {

        @Test
        @DisplayName("PENDING 상태에서 SUCCESS로 전환 가능")
        void test1() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);

            payment.markAsSuccess("PG_TXN_12345");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getPgTransactionId()).isEqualTo("PG_TXN_12345");
        }

        @Test
        @DisplayName("SUCCESS 상태에서 다시 SUCCESS로 전환 불가")
        void test2() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);
            payment.markAsSuccess("PG_TXN_12345");

            assertThatThrownBy(() -> payment.markAsSuccess("PG_TXN_67890"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("PENDING 상태에서만");
        }

        @Test
        @DisplayName("FAILED 상태에서 SUCCESS로 전환 불가")
        void test3() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);
            payment.markAsFailed("잔액부족");

            assertThatThrownBy(() -> payment.markAsSuccess("PG_TXN_12345"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("PENDING 상태에서만");
        }
    }

    @Nested
    @DisplayName("Payment 상태 전환 - markAsFailed()")
    class MarkAsFailedTest {

        @Test
        @DisplayName("PENDING 상태에서 FAILED로 전환 가능")
        void test1() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);

            payment.markAsFailed("포인트 부족");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isEqualTo("포인트 부족");
        }

        @Test
        @DisplayName("SUCCESS 상태에서 FAILED로 전환 불가")
        void test2() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);
            payment.markAsSuccess("PG_TXN_12345");

            assertThatThrownBy(() -> payment.markAsFailed("포인트 부족"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("PENDING 상태에서만");
        }

        @Test
        @DisplayName("FAILED 상태에서 다시 FAILED로 전환 불가")
        void test3() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);
            payment.markAsFailed("포인트 부족");

            assertThatThrownBy(() -> payment.markAsFailed("다른 이유"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("PENDING 상태에서만");
        }
    }

    @Nested
    @DisplayName("Payment 결제 타입 테스트")
    class PaymentTypeTest {

        @Test
        @DisplayName("포인트 결제 생성")
        void test1() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);
            assertThat(payment.getPaymentType()).isEqualTo(PaymentType.POINT_ONLY);
        }

        @Test
        @DisplayName("카드 결제 생성")
        void test2() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.CARD_ONLY);
            assertThat(payment.getPaymentType()).isEqualTo(PaymentType.CARD_ONLY);
        }

        @Test
        @DisplayName("혼합 결제 생성")
        void test3() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.MIXED);
            assertThat(payment.getPaymentType()).isEqualTo(PaymentType.MIXED);
        }
    }

    @Nested
    @DisplayName("Payment pgTransactionId 관리")
    class PgTransactionIdTest {

        @Test
        @DisplayName("updatePgTransactionId로 거래 ID 설정 후, markAsSuccess에서 같은 값 전달 시 성공")
        void test1() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.CARD_ONLY);

            payment.updatePgTransactionId("TX-12345");
            payment.markAsSuccess("TX-12345");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getPgTransactionId()).isEqualTo("TX-12345");
        }

        @Test
        @DisplayName("updatePgTransactionId로 거래 ID 설정 후, markAsSuccess에서 다른 값 전달 시 예외")
        void test2() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.CARD_ONLY);

            payment.updatePgTransactionId("TX-12345");

            assertThatThrownBy(() -> payment.markAsSuccess("TX-99999"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("거래 ID가 일치하지 않습니다");
        }

        @Test
        @DisplayName("pgTransactionId가 null일 때 markAsSuccess로 설정 가능")
        void test3() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.CARD_ONLY);

            payment.markAsSuccess("TX-12345");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getPgTransactionId()).isEqualTo("TX-12345");
        }

        @Test
        @DisplayName("updatePgTransactionId 중복 호출 시 예외")
        void test4() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.CARD_ONLY);

            payment.updatePgTransactionId("TX-12345");

            assertThatThrownBy(() -> payment.updatePgTransactionId("TX-99999"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("이미 거래 ID가 설정되어 있습니다");
        }
    }
}
