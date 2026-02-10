package com.back.boundedContext.cash.app;

import com.back.boundedContext.cash.domain.CashMember;
import com.back.boundedContext.cash.out.CashMemberRepository;
import com.back.global.eventPublisher.EventPublisher;
import com.back.shared.cash.event.CashMemberCreatedEvent;
import com.back.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashSyncMemberUseCase {
    private final CashMemberRepository cashMemberRepository;
    private final EventPublisher eventPublisher;

    public CashMember syncMember(MemberDto member) {
        boolean isNew = !cashMemberRepository.existsById(member.id());

        CashMember _member = cashMemberRepository.save(
                new CashMember(
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
                    new CashMemberCreatedEvent(
                            _member.toDto()
                    )
            );
        }

        return _member;
    }
}