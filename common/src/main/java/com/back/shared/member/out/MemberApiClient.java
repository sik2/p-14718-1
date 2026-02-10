package com.back.shared.member.out;

import com.back.global.auth.SystemAuthTokenProvider;
import com.back.global.security.AuthTokenValidator;
import com.back.shared.member.dto.MemberDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class MemberApiClient implements AuthTokenValidator {
    private final RestClient restClient;
    private final SystemAuthTokenProvider systemAuthTokenProvider;

    public MemberApiClient(
            @Value("${custom.services.member-url:http://localhost:8080}") String memberServiceUrl,
            SystemAuthTokenProvider systemAuthTokenProvider
    ) {
        this.systemAuthTokenProvider = systemAuthTokenProvider;
        this.restClient = RestClient.builder()
                .baseUrl(memberServiceUrl + "/api/v1/member/members")
                .build();
    }

    public String getRandomSecureTip() {
        return restClient.get()
                .uri("/randomSecureTip")
                .header("Authorization", systemAuthTokenProvider.getAuthorizationHeader())
                .retrieve()
                .body(String.class);
    }

    @Override
    public MemberDto validateToken(String accessToken) {
        try {
            return restClient.post()
                    .uri("/validate-token")
                    .header("Authorization", systemAuthTokenProvider.getAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("accessToken", accessToken))
                    .retrieve()
                    .body(MemberDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public MemberDto findByApiKey(String apiKey) {
        try {
            return restClient.get()
                    .uri("/by-apikey/" + apiKey)
                    .header("Authorization", systemAuthTokenProvider.getAuthorizationHeader())
                    .retrieve()
                    .body(MemberDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    public MemberDto findById(int id) {
        try {
            return restClient.get()
                    .uri("/" + id)
                    .header("Authorization", systemAuthTokenProvider.getAuthorizationHeader())
                    .retrieve()
                    .body(MemberDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String generateAccessToken(int memberId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/" + memberId + "/access-token")
                    .header("Authorization", systemAuthTokenProvider.getAuthorizationHeader())
                    .retrieve()
                    .body(Map.class);
            return response != null ? (String) response.get("data") : null;
        } catch (Exception e) {
            return null;
        }
    }
}
