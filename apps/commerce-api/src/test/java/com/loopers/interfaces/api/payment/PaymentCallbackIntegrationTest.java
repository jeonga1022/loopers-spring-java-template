package com.loopers.interfaces.api.payment;

import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentStatus;
import com.loopers.infrastructure.persistence.order.PaymentRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("결제 콜백 통합 테스트")
class PaymentCallbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final String TRANSACTION_KEY = "20250816:TR:9577c5";

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("PG 성공 콜백을 받으면 결제 상태가 SUCCESS로 변경된다")
    void test1() throws Exception {
        // arrange - 결제 데이터 먼저 생성
        Payment payment = Payment.create(1L, "user1", 10000L, com.loopers.domain.order.PaymentType.CARD_ONLY);
        payment.updatePgTransactionId(TRANSACTION_KEY);
        paymentRepository.save(payment);

        String requestBody = """
                {
                    "transactionKey": "%s",
                    "status": "SUCCESS",
                    "reason": null
                }
                """.formatted(TRANSACTION_KEY);

        // act
        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // assert - 실제 데이터베이스에서 조회해서 검증
        Payment savedPayment = paymentRepository.findByPgTransactionId(TRANSACTION_KEY).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("PG 실패 콜백을 받으면 결제 상태가 FAILED로 변경되고 실패 사유가 저장된다")
    void test2() throws Exception {
        // arrange
        Payment payment = Payment.create(1L, "user1", 10000L, com.loopers.domain.order.PaymentType.CARD_ONLY);
        payment.updatePgTransactionId(TRANSACTION_KEY);
        paymentRepository.save(payment);

        String requestBody = """
                {
                    "transactionKey": "%s",
                    "status": "FAILED",
                    "reason": "한도 초과"
                }
                """.formatted(TRANSACTION_KEY);

        // act
        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // assert
        Payment savedPayment = paymentRepository.findByPgTransactionId(TRANSACTION_KEY).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getFailureReason()).isEqualTo("한도 초과");
    }
}
