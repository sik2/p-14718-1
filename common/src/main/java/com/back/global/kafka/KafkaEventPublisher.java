package com.back.global.kafka;

import com.back.shared.cash.event.CashOrderPaymentFailedEvent;
import com.back.shared.cash.event.CashOrderPaymentSucceededEvent;
import com.back.shared.market.event.MarketOrderPaymentCompletedEvent;
import com.back.shared.market.event.MarketOrderPaymentRequestedEvent;
import com.back.shared.member.event.MemberJoinedEvent;
import com.back.shared.member.event.MemberModifiedEvent;
import com.back.shared.payout.event.PayoutCompletedEvent;
import com.back.shared.post.event.PostCommentCreatedEvent;
import com.back.shared.post.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(Object event) {
        String topic = resolveTopicName(event);

        if (topic == null) {
            log.debug("No Kafka topic for event: {}", event.getClass().getSimpleName());
            return;
        }

        kafkaTemplate.send(topic, event);
        log.info("Published event to Kafka topic [{}]: {}", topic, event.getClass().getSimpleName());
    }

    private String resolveTopicName(Object event) {
        return switch (event) {
            case MemberJoinedEvent e -> KafkaTopics.MEMBER_JOINED;
            case MemberModifiedEvent e -> KafkaTopics.MEMBER_MODIFIED;
            case PostCreatedEvent e -> KafkaTopics.POST_CREATED;
            case PostCommentCreatedEvent e -> KafkaTopics.POST_COMMENT_CREATED;
            case MarketOrderPaymentRequestedEvent e -> KafkaTopics.MARKET_ORDER_PAYMENT_REQUESTED;
            case MarketOrderPaymentCompletedEvent e -> KafkaTopics.MARKET_ORDER_PAYMENT_COMPLETED;
            case CashOrderPaymentSucceededEvent e -> KafkaTopics.CASH_ORDER_PAYMENT_SUCCEEDED;
            case CashOrderPaymentFailedEvent e -> KafkaTopics.CASH_ORDER_PAYMENT_FAILED;
            case PayoutCompletedEvent e -> KafkaTopics.PAYOUT_COMPLETED;
            default -> null;
        };
    }
}
