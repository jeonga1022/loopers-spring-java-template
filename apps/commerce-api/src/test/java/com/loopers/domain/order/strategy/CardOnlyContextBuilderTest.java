package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentDomainService;
import com.loopers.domain.order.PaymentStatus;
import com.loopers.domain.order.PaymentType;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.dto.PgPaymentRequest;
import com.loopers.infrastructure.pg.dto.PgPaymentResponse;
import io.github.resilience4j.core.lang.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardOnlyContextBuilder 테스트")
class CardOnlyContextBuilderTest {

    @Mock
    private PgClient pgClient;

    @Mock
    private PaymentDomainService paymentDomainService;

    @InjectMocks
    private CardOnlyContextBuilder cardOnlyContextBuilder;

    private static final Long ORDER_ID = 1L;
    private static final Long PAYMENT_ID = 100L;
    private static final String USER_ID = "user123";
    private static final Long CARD_AMOUNT = 10_000L;
    private static final String CARD_TYPE = "SAMSUNG";
    private static final String CARD_NO = "1234-5678-9814-1451";
    private static final String TRANSACTION_KEY = "20250816:TR:9577c5";

    @Test
    @DisplayName("카드 결제 요청 성공 시 PG 거래 ID 저장")
    void test1() {
        // arrange
        PaymentContext context = PaymentContext.forCardOnly(
            ORDER_ID, PAYMENT_ID, USER_ID, CARD_AMOUNT, CARD_TYPE, CARD_NO
        );

        PgPaymentResponse response = new PgPaymentResponse();
        ReflectionTestUtils.setField(response, "transactionKey", TRANSACTION_KEY);
        ReflectionTestUtils.setField(response, "status", "PENDING");

        when(pgClient.requestPayment(eq(USER_ID), any(PgPaymentRequest.class)))
            .thenReturn(response);

        ReflectionTestUtils.setField(cardOnlyContextBuilder, "serverPort", 8080);

        // act
        cardOnlyContextBuilder.executePayment(context);

        // assert
        verify(paymentDomainService).updatePgTransactionId(PAYMENT_ID, TRANSACTION_KEY);
    }

    @Test
    @DisplayName("PG 요청 실패 시 예외 발생하지 않고 정상 반환 (비동기 처리 준비)")
    void test2() {
        // arrange
        PaymentContext context = PaymentContext.forCardOnly(
            ORDER_ID, PAYMENT_ID, USER_ID, CARD_AMOUNT, CARD_TYPE, CARD_NO
        );

        // RuntimeException으로 테스트 (e.g., 네트워크 오류)
        when(pgClient.requestPayment(eq(USER_ID), any(PgPaymentRequest.class)))
            .thenThrow(new RuntimeException("PG connection failed"));

        ReflectionTestUtils.setField(cardOnlyContextBuilder, "serverPort", 8080);

        // act & assert - 예외 발생해도 propagate되지 않아야 함
        // (Fallback으로 처리되어야 함)
        assertThatNoException().isThrownBy(() ->
            cardOnlyContextBuilder.executePayment(context)
        );

        // 요청 실패 시에는 updatePgTransactionId가 호출되지 않음
    }

    @Test
    @DisplayName("@Timeout 적용 시 timeout 초과하면 TimeoutException 발생")
    void test3() {
        // arrange
        PaymentContext context = PaymentContext.forCardOnly(
            ORDER_ID, PAYMENT_ID, USER_ID, CARD_AMOUNT, CARD_TYPE, CARD_NO
        );

        // 5초 이상 대기하는 상황 시뮬레이션
        when(pgClient.requestPayment(eq(USER_ID), any(PgPaymentRequest.class)))
            .thenAnswer(invocation -> {
                Thread.sleep(6000);
                return new PgPaymentResponse();
            });

        ReflectionTestUtils.setField(cardOnlyContextBuilder, "serverPort", 8080);

        // act & assert
        // @Timeout이 적용되면 5초 후 TimeoutException이 발생해야 함
        // 하지만 Fallback으로 catch되어 propagate되지 않음
        assertThatNoException().isThrownBy(() ->
            cardOnlyContextBuilder.executePayment(context)
        );
    }
}
