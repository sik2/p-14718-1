package com.back.global.eventPublisher;

import com.back.global.kafka.KafkaEventPublisher;
import com.back.global.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final OutboxPublisher outboxPublisher;

    @Value("${outbox.enabled:false}")
    private boolean outboxEnabled;

    public void publish(Object event) {
        // Local event for same-service listeners
        applicationEventPublisher.publishEvent(event);

        // Cross-service communication
        if (outboxEnabled) {
            // Outbox 패턴: 트랜잭션 내에서 Outbox 테이블에 저장
            outboxPublisher.saveToOutbox(event);
        } else {
            // 기존 방식: Kafka 직접 발행
            kafkaEventPublisher.publish(event);
        }
    }
}
