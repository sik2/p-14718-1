package com.back.global.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SystemAuthTokenProvider {
    @Value("${custom.system.apiKey}")
    private String systemApiKey;

    public String getSystemApiKey() {
        return systemApiKey;
    }

    public String getAuthorizationHeader() {
        return "Bearer " + systemApiKey + " empty";
    }
}
