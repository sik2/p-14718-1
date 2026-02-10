package com.back.global.security;

import com.back.shared.member.dto.MemberDto;

public interface AuthTokenValidator {
    MemberDto validateToken(String accessToken);
    MemberDto findByApiKey(String apiKey);
    String generateAccessToken(int memberId);
}
