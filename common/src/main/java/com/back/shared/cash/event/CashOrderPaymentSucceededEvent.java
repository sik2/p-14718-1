package com.back.shared.cash.event;

import com.back.shared.market.dto.OrderDto;

public record CashOrderPaymentSucceededEvent(OrderDto order, long pgPaymentAmount) {}
