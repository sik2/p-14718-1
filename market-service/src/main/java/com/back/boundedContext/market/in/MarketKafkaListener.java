package com.back.boundedContext.market.in;

import com.back.boundedContext.market.app.MarketFacade;
import com.back.global.kafka.KafkaTopics;
import com.back.shared.cash.event.CashOrderPaymentFailedEvent;
import com.back.shared.cash.event.CashOrderPaymentSucceededEvent;
import com.back.shared.member.event.MemberJoinedEvent;
import com.back.shared.member.event.MemberModifiedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketKafkaListener {
    private final MarketFacade marketFacade;

    @KafkaListener(topics = KafkaTopics.MEMBER_JOINED, groupId = "market-service")
    @Transactional
    public void handleMemberJoined(MemberJoinedEvent event) {
        log.info("Received MemberJoinedEvent via Kafka: memberId={}", event.member().id());
        marketFacade.syncMember(event.member());
    }

    @KafkaListener(topics = KafkaTopics.MEMBER_MODIFIED, groupId = "market-service")
    @Transactional
    public void handleMemberModified(MemberModifiedEvent event) {
        log.info("Received MemberModifiedEvent via Kafka: memberId={}", event.member().id());
        marketFacade.syncMember(event.member());
    }

    @KafkaListener(topics = KafkaTopics.CASH_ORDER_PAYMENT_SUCCEEDED, groupId = "market-service")
    @Transactional
    public void handleCashOrderPaymentSucceeded(CashOrderPaymentSucceededEvent event) {
        log.info("Received CashOrderPaymentSucceededEvent via Kafka: orderId={}", event.order().id());
        int orderId = event.order().id();
        marketFacade.completeOrderPayment(orderId);
    }

    @KafkaListener(topics = KafkaTopics.CASH_ORDER_PAYMENT_FAILED, groupId = "market-service")
    @Transactional
    public void handleCashOrderPaymentFailed(CashOrderPaymentFailedEvent event) {
        log.info("Received CashOrderPaymentFailedEvent via Kafka: orderId={}", event.order().id());
        int orderId = event.order().id();
        marketFacade.cancelOrderRequestPayment(orderId);
    }
}
