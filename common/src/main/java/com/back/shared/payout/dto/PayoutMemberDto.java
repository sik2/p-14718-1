package com.back.shared.payout.dto;

import java.time.LocalDateTime;

public record PayoutMemberDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String username,
        String nickname,
        int activityScore
) {}
