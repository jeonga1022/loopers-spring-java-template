package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentDomainService;
import com.loopers.domain.order.PaymentType;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.dto.PgPaymentRequest;
import com.loopers.infrastructure.pg.dto.PgPaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardOnlyContextBuilder implements PaymentStrategy {

    private final PgClient pgClient;
    private final PaymentDomainService paymentDomainService;

    @Value("${server.port:8080}")
    private int serverPort;

    @Override
    public boolean supports(PaymentType paymentType) {
        return paymentType == PaymentType.CARD_ONLY;
    }

    @Override
    @Retry(name = "pgRetry")
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallbackRequestPayment")
    @TimeLimiter(name = "pgTimeLimit", fallbackMethod = "fallbackRequestPayment")
    public void executePayment(PaymentContext context) {
        try {
            PgPaymentRequest request = PgPaymentRequest.builder()
                    .orderId(context.orderId().toString())
                    .cardType(context.cardType())
                    .cardNo(context.cardNo())
                    .amount(context.cardAmount())
                    .callbackUrl("http://localhost:" + serverPort + "/api/v1/payments/callback")
                    .build();

            PgPaymentResponse response = pgClient.requestPayment(context.userId(), request);
            paymentDomainService.updatePgTransactionId(context.paymentId(), response.getTransactionKey());
        } catch (Exception e) {
            log.warn("PG request failed for orderId: {}, paymentId: {}. Treating as async (PENDING)",
                    context.orderId(), context.paymentId(), e);
            // Fallback: Payment 상태는 이미 PENDING으로 저장되어 있으므로
            // 예외를 catch하고 정상 반환. 나중에 콜백으로 결과를 받음.
        }
    }

    public void fallbackRequestPayment(PaymentContext context, Throwable throwable) {
        log.warn("PG request timeout/failure fallback. orderId: {}, paymentId: {}, reason: {}",
                context.orderId(), context.paymentId(), throwable.getClass().getSimpleName());
        // Payment 상태는 이미 PENDING으로 저장되어 있음
        // Fallback에서 추가 처리 불필요
    }
}