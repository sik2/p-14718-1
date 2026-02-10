package com.back.boundedContext.payout.in;

import com.back.boundedContext.payout.app.PayoutFacade;
import com.back.global.kafka.KafkaTopics;
import com.back.shared.market.event.MarketOrderPaymentCompletedEvent;
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
public class PayoutKafkaListener {
    private final PayoutFacade payoutFacade;

    @KafkaListener(topics = KafkaTopics.MEMBER_JOINED, groupId = "payout-service")
    @Transactional
    public void handleMemberJoined(MemberJoinedEvent event) {
        log.info("Received MemberJoinedEvent via Kafka: memberId={}", event.member().id());
        payoutFacade.syncMember(event.member());
    }

    @KafkaListener(topics = KafkaTopics.MEMBER_MODIFIED, groupId = "payout-service")
    @Transactional
    public void handleMemberModified(MemberModifiedEvent event) {
        log.info("Received MemberModifiedEvent via Kafka: memberId={}", event.member().id());
        payoutFacade.syncMember(event.member());
    }

    @KafkaListener(topics = KafkaTopics.MARKET_ORDER_PAYMENT_COMPLETED, groupId = "payout-service")
    @Transactional
    public void handleMarketOrderPaymentCompleted(MarketOrderPaymentCompletedEvent event) {
        log.info("Received MarketOrderPaymentCompletedEvent via Kafka: orderId={}", event.order().id());
        payoutFacade.addPayoutCandidateItems(event.order());
    }
}
