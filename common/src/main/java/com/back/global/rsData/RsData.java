package com.back.global.rsData;

import com.back.standard.resultType.ResultType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RsData<T> implements ResultType {
    private final String resultCode;
    private final String msg;
    private final T data;

    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }
}
