package com.loopers.infrastructure.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Mock
    private SendResult<Object, Object> sendResult;

    private OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(outboxRepository, kafkaTemplate);
    }

    @Test
    @DisplayName("PENDING 상태의 Outbox를 Kafka로 발행하고 PROCESSED로 변경한다")
    void relayTest1() {
        Outbox outbox = Outbox.create("PRODUCT", "1", "ProductLikedEvent", "catalog-events", "{\"productId\":1}");
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(outbox));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(sendResult));

        outboxRelay.relay();

        verify(kafkaTemplate).send(eq("catalog-events"), eq("1"), eq("{\"productId\":1}"));
        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        verify(outboxRepository).save(outbox);
    }

    @Test
    @DisplayName("PENDING 상태의 Outbox가 없으면 아무것도 하지 않는다")
    void relayTest2() {
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of());

        outboxRelay.relay();

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Kafka 발행 실패 시 상태를 변경하지 않는다")
    void relayTest3() {
        Outbox outbox = Outbox.create("PRODUCT", "1", "ProductLikedEvent", "catalog-events", "{\"productId\":1}");
        when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(outbox));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka error")));

        outboxRelay.relay();

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(outboxRepository, never()).save(any());
    }
}
