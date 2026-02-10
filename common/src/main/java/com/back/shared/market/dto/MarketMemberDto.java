package com.back.shared.market.dto;

import java.time.LocalDateTime;

public record MarketMemberDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String username,
        String nickname,
        int activityScore
) {}
