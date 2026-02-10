package com.back.shared.cash.event;

import com.back.shared.market.dto.OrderDto;
import com.back.standard.resultType.ResultType;

public record CashOrderPaymentFailedEvent(
        String resultCode,
        String msg,
        OrderDto order,
        long pgPaymentAmount,
        long shortfallAmount
) implements ResultType {
    @Override
    public String getResultCode() {
        return resultCode;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
