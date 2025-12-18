package com.loopers.infrastructure.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.infrastructure.idempotent.EventHandled;
import com.loopers.infrastructure.idempotent.EventHandledRepository;
import com.loopers.infrastructure.metrics.ProductMetrics;
import com.loopers.infrastructure.metrics.ProductMetricsRepository;
import com.loopers.infrastructure.outbox.OutboxRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventConsumer {

    private final EventHandledRepository eventHandledRepository;
    private final ProductMetricsRepository productMetricsRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "catalog-events", groupId = "catalog-consumer")
    @Transactional
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String eventId = extractEventId(record);
        if (eventId == null) {
            log.warn("outbox-id 헤더가 없는 메시지 수신: {}", record);
            acknowledgment.acknowledge();
            return;
        }

        if (eventHandledRepository.existsByEventId(eventId)) {
            log.info("이미 처리된 이벤트 무시: eventId={}", eventId);
            acknowledgment.acknowledge();
            return;
        }

        try {
            processEvent(record.value());
            eventHandledRepository.save(EventHandled.create(eventId));
            acknowledgment.acknowledge();
            log.info("이벤트 처리 완료: eventId={}", eventId);
        } catch (Exception e) {
            log.error("이벤트 처리 실패, 재처리 예정: eventId={}", eventId, e);
        }
    }

    private String extractEventId(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(OutboxRelay.HEADER_OUTBOX_ID);
        if (header == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private void processEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long productId = node.get("productId").asLong();
            boolean liked = node.get("liked").asBoolean();
            LocalDateTime occurredAt = LocalDateTime.parse(node.get("occurredAt").asText());

            ProductMetrics metrics = productMetricsRepository.findByProductId(productId)
                    .orElseGet(() -> ProductMetrics.create(productId));

            if (metrics.updateLikeIfNewer(liked, occurredAt)) {
                productMetricsRepository.save(metrics);
            }
        } catch (Exception e) {
            throw new RuntimeException("이벤트 파싱 실패", e);
        }
    }
}
