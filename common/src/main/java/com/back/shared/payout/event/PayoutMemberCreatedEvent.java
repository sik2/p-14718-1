package com.back.shared.payout.event;

import com.back.shared.payout.dto.PayoutMemberDto;

public record PayoutMemberCreatedEvent(PayoutMemberDto member) {}
