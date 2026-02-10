package com.back.global.security;

import com.back.boundedContext.member.app.MemberFacade;
import com.back.boundedContext.member.domain.Member;
import com.back.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Primary
@RequiredArgsConstructor
public class LocalAuthTokenValidator implements AuthTokenValidator {
    private final MemberFacade memberFacade;

    @Override
    public MemberDto validateToken(String accessToken) {
        Map<String, Object> payload = memberFacade.payload(accessToken);
        if (payload == null) return null;

        int id = (int) payload.get("id");
        return memberFacade.findById(id)
                .map(Member::toDto)
                .orElse(null);
    }

    @Override
    public MemberDto findByApiKey(String apiKey) {
        return memberFacade.findByApiKey(apiKey)
                .map(Member::toDto)
                .orElse(null);
    }

    @Override
    public String generateAccessToken(int memberId) {
        return memberFacade.findById(memberId)
                .map(memberFacade::genAccessToken)
                .orElse(null);
    }
}
