package com.back.shared.member.dto;

import java.time.LocalDateTime;

public record MemberDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String username,
        String nickname,
        int activityScore
) {}
