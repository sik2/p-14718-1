package com.back.boundedContext.member.app;

import com.back.boundedContext.member.domain.Member;
import com.back.standard.ut.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MemberAuthTokenUseCase {
    @Value("${custom.jwt.secretKey}")
    private String jwtSecretKey;

    @Value("${custom.accessToken.expirationSeconds}")
    private int accessTokenExpirationSeconds;

    public String genAccessToken(Member member) {
        int id = member.getId();
        String username = member.getUsername();
        String nickname = member.getNickname();

        return Util.jwt.toString(
                jwtSecretKey,
                accessTokenExpirationSeconds,
                Map.of("id", id, "username", username, "nickname", nickname)
        );
    }

    public Map<String, Object> payload(String accessToken) {
        Map<String, Object> parsedPayload = Util.jwt.payload(jwtSecretKey, accessToken);

        if (parsedPayload == null) return null;

        int id = (int) parsedPayload.get("id");
        String username = (String) parsedPayload.get("username");
        String nickname = (String) parsedPayload.get("nickname");

        return Map.of("id", id, "username", username, "nickname", nickname);
    }
}
