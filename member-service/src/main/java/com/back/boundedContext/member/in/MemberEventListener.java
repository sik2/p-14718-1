package com.back.boundedContext.member.in;

import com.back.boundedContext.member.app.MemberFacade;
import com.back.boundedContext.member.domain.Member;
import com.back.shared.post.event.PostCommentCreatedEvent;
import com.back.shared.post.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
public class MemberEventListener {
    private final MemberFacade memberFacade;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(PostCreatedEvent event) {
        Member member = memberFacade.findById(event.post().authorId()).get();

        member.increaseActivityScore(3);
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional(propagation = REQUIRES_NEW)
    public void handle(PostCommentCreatedEvent event) {
        Member member = memberFacade.findById(event.postComment().authorId()).get();

        member.increaseActivityScore(1);
    }
}
