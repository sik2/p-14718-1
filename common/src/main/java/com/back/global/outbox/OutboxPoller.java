package com.back.global.outbox;

import com.back.global.outbox.entity.OutboxEvent;
import com.back.global.outbox.entity.OutboxStatus;
import com.back.global.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.poller.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${outbox.poller.batch-size:100}")
    private int batchSize;

    @Value("${outbox.poller.max-retry:5}")
    private int maxRetry;

    @Scheduled(fixedDelayString = "${outbox.poller.interval-ms:5000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreateDate(
            OutboxStatus.PENDING,
            PageRequest.of(0, batchSize)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            event.markAsProcessing();

            kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                .get(10, TimeUnit.SECONDS);

            event.markAsSent();
            log.debug("Published outbox event: id={}, topic={}", event.getId(), event.getTopic());

        } catch (Exception e) {
            log.error("Failed to publish outbox event: id={}, error={}", event.getId(), e.getMessage());
            event.markAsFailed(e.getMessage());
        }
    }

    @Scheduled(cron = "${outbox.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int deletedCount = outboxEventRepository.deleteSentEventsBefore(threshold);
        if (deletedCount > 0) {
            log.info("Cleaned up {} old sent outbox events", deletedCount);
        }
    }
}
