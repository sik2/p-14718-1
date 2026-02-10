package com.back.shared.cash.dto;

import java.time.LocalDateTime;

public record WalletDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        int holderId,
        String holderName,
        long balance
) {}
