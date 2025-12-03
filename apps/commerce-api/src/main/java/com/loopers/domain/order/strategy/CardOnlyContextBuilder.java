package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentDomainService;
import com.loopers.domain.order.PaymentType;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.dto.PgPaymentRequest;
import com.loopers.infrastructure.pg.dto.PgPaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
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
    public void executePayment(PaymentContext context) {
        PgPaymentRequest request = PgPaymentRequest.builder()
                .orderId(context.orderId().toString())
                .cardType(context.cardType())
                .cardNo(context.cardNo())
                .amount(context.cardAmount())
                .callbackUrl("http://localhost:" + serverPort + "/api/v1/payments/callback")
                .build();

        PgPaymentResponse response = pgClient.requestPayment(context.userId(), request);

        paymentDomainService.updatePgTransactionId(context.paymentId(), response.getTransactionKey());
    }
}