package com.back.boundedContext.payout.app;

import com.back.boundedContext.payout.domain.PayoutMember;
import com.back.boundedContext.payout.out.PayoutMemberRepository;
import com.back.global.eventPublisher.EventPublisher;
import com.back.shared.member.dto.MemberDto;
import com.back.shared.payout.event.PayoutMemberCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayoutSyncMemberUseCase {
    private final PayoutMemberRepository payoutMemberRepository;
    private final EventPublisher eventPublisher;

    public PayoutMember syncMember(MemberDto member) {
        boolean isNew = !payoutMemberRepository.existsById(member.id());

        PayoutMember _member = payoutMemberRepository.save(
                new PayoutMember(
                        member.id(),
                        member.createDate(),
                        member.modifyDate(),
                        member.username(),
                        "",
                        member.nickname(),
                        member.activityScore()
                )
        );

        if (isNew) {
            eventPublisher.publish(
                    new PayoutMemberCreatedEvent(
                            _member.toDto()
                    )
            );
        }

        return _member;
    }
}
