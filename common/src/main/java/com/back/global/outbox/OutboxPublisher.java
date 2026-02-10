package com.back.global.outbox;

import com.back.global.kafka.KafkaTopics;
import com.back.global.outbox.entity.OutboxEvent;
import com.back.global.outbox.repository.OutboxEventRepository;
import com.back.shared.cash.event.CashOrderPaymentFailedEvent;
import com.back.shared.cash.event.CashOrderPaymentSucceededEvent;
import com.back.shared.market.event.MarketOrderPaymentCompletedEvent;
import com.back.shared.market.event.MarketOrderPaymentRequestedEvent;
import com.back.shared.member.event.MemberJoinedEvent;
import com.back.shared.member.event.MemberModifiedEvent;
import com.back.shared.payout.event.PayoutCompletedEvent;
import com.back.shared.post.event.PostCommentCreatedEvent;
import com.back.shared.post.event.PostCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveToOutbox(Object event) {
        OutboxEventMetadata metadata = resolveMetadata(event);

        if (metadata == null) {
            log.debug("No outbox metadata for event: {}", event.getClass().getSimpleName());
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.create(
                metadata.aggregateType(),
                metadata.aggregateId(),
                event.getClass().getSimpleName(),
                metadata.topic(),
                payload
            );

            outboxEventRepository.save(outboxEvent);
            log.debug("Saved event to outbox: type={}, aggregateId={}",
                metadata.aggregateType(), metadata.aggregateId());

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event to JSON", e);
        }
    }

    private OutboxEventMetadata resolveMetadata(Object event) {
        return switch (event) {
            case MemberJoinedEvent e -> new OutboxEventMetadata(
                "Member", String.valueOf(e.member().id()), KafkaTopics.MEMBER_JOINED);
            case MemberModifiedEvent e -> new OutboxEventMetadata(
                "Member", String.valueOf(e.member().id()), KafkaTopics.MEMBER_MODIFIED);
            case PostCreatedEvent e -> new OutboxEventMetadata(
                "Post", String.valueOf(e.post().id()), KafkaTopics.POST_CREATED);
            case PostCommentCreatedEvent e -> new OutboxEventMetadata(
                "PostComment", String.valueOf(e.postComment().id()), KafkaTopics.POST_COMMENT_CREATED);
            case MarketOrderPaymentRequestedEvent e -> new OutboxEventMetadata(
                "Order", String.valueOf(e.order().id()), KafkaTopics.MARKET_ORDER_PAYMENT_REQUESTED);
            case MarketOrderPaymentCompletedEvent e -> new OutboxEventMetadata(
                "Order", String.valueOf(e.order().id()), KafkaTopics.MARKET_ORDER_PAYMENT_COMPLETED);
            case CashOrderPaymentSucceededEvent e -> new OutboxEventMetadata(
                "Order", String.valueOf(e.order().id()), KafkaTopics.CASH_ORDER_PAYMENT_SUCCEEDED);
            case CashOrderPaymentFailedEvent e -> new OutboxEventMetadata(
                "Order", String.valueOf(e.order().id()), KafkaTopics.CASH_ORDER_PAYMENT_FAILED);
            case PayoutCompletedEvent e -> new OutboxEventMetadata(
                "Payout", String.valueOf(e.payout().id()), KafkaTopics.PAYOUT_COMPLETED);
            default -> null;
        };
    }

    private record OutboxEventMetadata(String aggregateType, String aggregateId, String topic) {}
}
