package com.back.boundedContext.member.app;

import com.back.boundedContext.member.domain.Member;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberFacade {
    private final MemberSupport memberSupport;
    private final MemberJoinUseCase memberJoinUseCase;
    private final MemberGetRandomSecureTipUseCase memberGetRandomSecureTipUseCase;
    private final MemberLoginUseCase memberLoginUseCase;
    private final MemberAuthTokenUseCase memberAuthTokenUseCase;

    @Transactional
    public RsData<Member> join(String username, String password, String nickname) {
        return memberJoinUseCase.join(username, password, nickname);
    }

    public String getRandomSecureTip() {
        return memberGetRandomSecureTipUseCase.getRandomSecureTip();
    }

    @Transactional(readOnly = true)
    public long count() {
        return memberSupport.count();
    }

    @Transactional(readOnly = true)
    public Optional<Member> findByUsername(String username) {
        return memberSupport.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<Member> findById(int id) {
        return memberSupport.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Member> findByApiKey(String apiKey) {
        return memberSupport.findByApiKey(apiKey);
    }

    @Transactional(readOnly = true)
    public Member login(String username, String password) {
        return memberLoginUseCase.login(username, password);
    }

    public String genAccessToken(Member member) {
        return memberAuthTokenUseCase.genAccessToken(member);
    }

    public Map<String, Object> payload(String accessToken) {
        return memberAuthTokenUseCase.payload(accessToken);
    }
}
