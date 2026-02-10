package com.back.shared.post.out;

import com.back.global.auth.SystemAuthTokenProvider;
import com.back.shared.post.dto.PostDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class PostApiClient {
    private final RestClient restClient;
    private final SystemAuthTokenProvider systemAuthTokenProvider;

    public PostApiClient(
            @Value("${custom.services.post-url:http://localhost:8081}") String postServiceUrl,
            SystemAuthTokenProvider systemAuthTokenProvider
    ) {
        this.systemAuthTokenProvider = systemAuthTokenProvider;
        this.restClient = RestClient.builder()
                .baseUrl(postServiceUrl + "/api/v1/post")
                .build();
    }

    public List<PostDto> getItems() {
        return restClient.get()
                .uri("/posts")
                .header("Authorization", systemAuthTokenProvider.getAuthorizationHeader())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public PostDto getItem(int id) {
        return restClient.get()
                .uri("/posts/%d".formatted(id))
                .header("Authorization", systemAuthTokenProvider.getAuthorizationHeader())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
