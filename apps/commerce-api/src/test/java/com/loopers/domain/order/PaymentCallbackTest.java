package com.loopers.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment 콜백 처리")
class PaymentCallbackTest {

    @Mock
    PaymentRepository paymentRepository;

    @InjectMocks
    PaymentDomainService paymentDomainService;

    static final Long PAYMENT_ID = 100L;
    static final Long ORDER_ID = 1L;
    static final String USER_ID = "user123";
    static final String TRANSACTION_KEY = "20250816:TR:9577c5";

    @Test
    @DisplayName("PG 콜백으로 결제 성공 처리")
    void test1() {
        // arrange
        Payment payment = Payment.create(ORDER_ID, USER_ID, 10_000L, PaymentType.CARD_ONLY);
        payment.updatePgTransactionId(TRANSACTION_KEY);

        when(paymentRepository.findByPgTransactionId(TRANSACTION_KEY))
            .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
            .thenReturn(payment);

        // act
        paymentDomainService.markAsSuccessByTransactionKey(TRANSACTION_KEY);

        // assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("PG 콜백으로 결제 실패 처리")
    void test2() {
        // arrange
        Payment payment = Payment.create(ORDER_ID, USER_ID, 10_000L, PaymentType.CARD_ONLY);
        payment.updatePgTransactionId(TRANSACTION_KEY);

        when(paymentRepository.findByPgTransactionId(TRANSACTION_KEY))
            .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
            .thenReturn(payment);

        // act
        paymentDomainService.markAsFailedByTransactionKey(TRANSACTION_KEY, "한도 초과");

        // assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("한도 초과");
        verify(paymentRepository).save(any(Payment.class));
    }
}
