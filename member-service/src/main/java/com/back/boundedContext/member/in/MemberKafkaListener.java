package com.back.boundedContext.member.in;

import com.back.boundedContext.member.app.MemberFacade;
import com.back.boundedContext.member.domain.Member;
import com.back.global.kafka.KafkaTopics;
import com.back.shared.post.event.PostCommentCreatedEvent;
import com.back.shared.post.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberKafkaListener {
    private final MemberFacade memberFacade;

    @KafkaListener(topics = KafkaTopics.POST_CREATED, groupId = "member-service")
    @Transactional
    public void handlePostCreated(PostCreatedEvent event) {
        log.info("Received PostCreatedEvent via Kafka: postId={}", event.post().id());
        Member member = memberFacade.findById(event.post().authorId()).get();
        member.increaseActivityScore(3);
    }

    @KafkaListener(topics = KafkaTopics.POST_COMMENT_CREATED, groupId = "member-service")
    @Transactional
    public void handlePostCommentCreated(PostCommentCreatedEvent event) {
        log.info("Received PostCommentCreatedEvent via Kafka: commentId={}", event.postComment().id());
        Member member = memberFacade.findById(event.postComment().authorId()).get();
        member.increaseActivityScore(1);
    }
}
