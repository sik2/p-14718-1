package com.back.shared.cash.dto;

import java.time.LocalDateTime;

public record CashMemberDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String username,
        String nickname,
        int activityScore
) {}
