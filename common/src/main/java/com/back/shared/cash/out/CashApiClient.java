package com.back.shared.cash.out;

import com.back.global.auth.SystemAuthTokenProvider;
import com.back.shared.cash.dto.WalletDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CashApiClient {
    private final RestClient restClient;
    private final SystemAuthTokenProvider systemAuthTokenProvider;

    public CashApiClient(
            @Value("${custom.services.cash-url}") String cashServiceUrl,
            SystemAuthTokenProvider systemAuthTokenProvider
    ) {
        this.systemAuthTokenProvider = systemAuthTokenProvider;
        this.restClient = RestClient.builder()
                .baseUrl(cashServiceUrl + "/api/v1/cash")
                .build();
    }

    public WalletDto getItemByHolderId(int holderId) {
        return restClient.get()
                .uri("/wallets/by-holder/" + holderId)
                .header("Authorization", systemAuthTokenProvider.getAuthorizationHeader())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public long getBalanceByHolderId(int holderId) {
        WalletDto walletDto = getItemByHolderId(holderId);
        return walletDto.balance();
    }
}
