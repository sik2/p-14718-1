package com.back.boundedContext.post.in;

import com.back.boundedContext.post.app.PostFacade;
import com.back.global.kafka.KafkaTopics;
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
public class PostKafkaListener {
    private final PostFacade postFacade;

    @KafkaListener(topics = KafkaTopics.MEMBER_JOINED, groupId = "post-service")
    @Transactional
    public void handleMemberJoined(MemberJoinedEvent event) {
        log.info("Received MemberJoinedEvent via Kafka: memberId={}", event.member().id());
        postFacade.syncMember(event.member());
    }

    @KafkaListener(topics = KafkaTopics.MEMBER_MODIFIED, groupId = "post-service")
    @Transactional
    public void handleMemberModified(MemberModifiedEvent event) {
        log.info("Received MemberModifiedEvent via Kafka: memberId={}", event.member().id());
        postFacade.syncMember(event.member());
    }
}
