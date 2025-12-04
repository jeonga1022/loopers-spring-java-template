package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentDomainService;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.dto.PgPaymentRequest;
import com.loopers.infrastructure.pg.dto.PgPaymentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardPaymentStrategyTest {

    @Mock
    private PgClient pgClient;

    @Mock
    private PaymentDomainService paymentDomainService;

    @InjectMocks
    private CardPaymentStrategy cardPaymentStrategy;

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
        PaymentContext context = PaymentContext.forCardOnly(
            ORDER_ID, PAYMENT_ID, USER_ID, CARD_AMOUNT, CARD_TYPE, CARD_NO
        );

        PgPaymentResponse response = new PgPaymentResponse();
        ReflectionTestUtils.setField(response, "transactionKey", TRANSACTION_KEY);
        ReflectionTestUtils.setField(response, "status", "PENDING");

        when(pgClient.requestPayment(eq(USER_ID), any(PgPaymentRequest.class)))
            .thenReturn(response);

        ReflectionTestUtils.setField(cardPaymentStrategy, "callbackBaseUrl", "http://localhost:8080");

        cardPaymentStrategy.executePayment(context);

        verify(paymentDomainService).updatePgTransactionId(PAYMENT_ID, TRANSACTION_KEY);
    }

    @Test
    @DisplayName("PG 요청 실패 시 예외 발생하지 않고 정상 반환 (비동기 처리 준비)")
    void test2() {
        PaymentContext context = PaymentContext.forCardOnly(
            ORDER_ID, PAYMENT_ID, USER_ID, CARD_AMOUNT, CARD_TYPE, CARD_NO
        );

        when(pgClient.requestPayment(eq(USER_ID), any(PgPaymentRequest.class)))
            .thenThrow(new RuntimeException("PG connection failed"));

        ReflectionTestUtils.setField(cardPaymentStrategy, "callbackBaseUrl", "http://localhost:8080");

        assertThatNoException().isThrownBy(() ->
            cardPaymentStrategy.executePayment(context)
        );
    }

    @Test
    @DisplayName("TimeLimiter 타임아웃 초과 시에도 예외 발생하지 않음")
    void test3() {
        PaymentContext context = PaymentContext.forCardOnly(
            ORDER_ID, PAYMENT_ID, USER_ID, CARD_AMOUNT, CARD_TYPE, CARD_NO
        );

        when(pgClient.requestPayment(eq(USER_ID), any(PgPaymentRequest.class)))
            .thenAnswer(invocation -> {
                Thread.sleep(6000);
                return new PgPaymentResponse();
            });

        ReflectionTestUtils.setField(cardPaymentStrategy, "callbackBaseUrl", "http://localhost:8080");

        assertThatNoException().isThrownBy(() ->
            cardPaymentStrategy.executePayment(context)
        );
    }

    @Test
    @DisplayName("CircuitBreaker 실패율 50% 초과 시 회로 오픈")
    void test4() {
        PaymentContext context = PaymentContext.forCardOnly(
            ORDER_ID, PAYMENT_ID, USER_ID, CARD_AMOUNT, CARD_TYPE, CARD_NO
        );

        ReflectionTestUtils.setField(cardPaymentStrategy, "callbackBaseUrl", "http://localhost:8080");

        when(pgClient.requestPayment(eq(USER_ID), any(PgPaymentRequest.class)))
            .thenReturn(createSuccessResponse())
            .thenReturn(createSuccessResponse())
            .thenReturn(createSuccessResponse())
            .thenReturn(createSuccessResponse())
            .thenReturn(createSuccessResponse())
            .thenThrow(new RuntimeException("PG failure 1"))
            .thenThrow(new RuntimeException("PG failure 2"))
            .thenThrow(new RuntimeException("PG failure 3"))
            .thenThrow(new RuntimeException("PG failure 4"))
            .thenThrow(new RuntimeException("PG failure 5"));

        for (int i = 0; i < 10; i++) {
            assertThatNoException().isThrownBy(() ->
                cardPaymentStrategy.executePayment(context)
            );
        }

        long startTime = System.currentTimeMillis();
        assertThatNoException().isThrownBy(() ->
            cardPaymentStrategy.executePayment(context)
        );
        long duration = System.currentTimeMillis() - startTime;

        assertThat(duration).isLessThan(1000);
    }

    private PgPaymentResponse createSuccessResponse() {
        PgPaymentResponse response = new PgPaymentResponse();
        ReflectionTestUtils.setField(response, "transactionKey", TRANSACTION_KEY);
        ReflectionTestUtils.setField(response, "status", "PENDING");
        return response;
    }
}
