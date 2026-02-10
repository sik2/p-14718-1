package com.back.boundedContext.post.app;

import com.back.boundedContext.post.domain.PostMember;
import com.back.boundedContext.post.out.PostMemberRepository;
import com.back.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostSyncMemberUseCase {
    private final PostMemberRepository postMemberRepository;

    public PostMember syncMember(MemberDto member) {
        PostMember postMember = new PostMember(
                member.id(),
                member.createDate(),
                member.modifyDate(),
                member.username(),
                "",
                member.nickname(),
                member.activityScore()
        );

        return postMemberRepository.save(postMember);
    }
}
