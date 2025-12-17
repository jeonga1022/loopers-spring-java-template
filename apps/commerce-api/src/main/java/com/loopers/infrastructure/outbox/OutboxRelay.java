package com.loopers.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int BATCH_SIZE = 100;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void relay() {
        List<Outbox> pendingList = outboxRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, BATCH_SIZE)
        );

        for (Outbox outbox : pendingList) {
            try {
                kafkaTemplate.send(outbox.getTopic(), outbox.getAggregateId(), outbox.getPayload())
                        .get();
                outbox.markProcessed();
                outboxRepository.save(outbox);
                log.info("Outbox 발행 성공: id={}, topic={}", outbox.getId(), outbox.getTopic());
            } catch (Exception e) {
                log.error("Outbox 발행 실패: id={}, topic={}", outbox.getId(), outbox.getTopic(), e);
            }
        }
    }
}
