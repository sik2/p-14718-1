package com.back.shared.market.out;

import com.back.global.auth.SystemAuthTokenProvider;
import com.back.shared.market.dto.OrderItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class MarketApiClient {
    private final RestClient restClient;
    private final SystemAuthTokenProvider systemAuthTokenProvider;

    public MarketApiClient(
            @Value("${custom.services.market-url:http://localhost:8084}") String marketServiceUrl,
            SystemAuthTokenProvider systemAuthTokenProvider
    ) {
        this.systemAuthTokenProvider = systemAuthTokenProvider;
        this.restClient = RestClient.builder()
                .baseUrl(marketServiceUrl + "/api/v1/market")
                .build();
    }

    public List<OrderItemDto> getOrderItems(int id) {
        return restClient
                .get()
                .uri("/orders/%d/items".formatted(id))
                .header("Authorization", systemAuthTokenProvider.getAuthorizationHeader())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
