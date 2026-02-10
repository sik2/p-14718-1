package com.back.global.global;

import com.back.global.eventPublisher.EventPublisher;
import com.back.standard.ut.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalConfig {
    @Getter
    private static EventPublisher eventPublisher;

    private static ObjectMapper objectMapper;

    @Autowired
    public void setEventPublisher(EventPublisher eventPublisher) {
        GlobalConfig.eventPublisher = eventPublisher;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        GlobalConfig.objectMapper = objectMapper;
    }

    @PostConstruct
    public void postConstruct() {
        Util.json.objectMapper = objectMapper;
    }
}
