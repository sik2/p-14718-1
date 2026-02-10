package com.back.shared.payout.event;

import com.back.shared.payout.dto.PayoutDto;

public record PayoutCompletedEvent(PayoutDto payout) {}
