package com.back.shared.market.event;

import com.back.shared.market.dto.OrderDto;

public record MarketOrderPaymentRequestedEvent(OrderDto order, long pgPaymentAmount) {}
