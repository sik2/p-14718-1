package com.back.global.outbox.entity;

public enum OutboxStatus {
    PENDING,      // 대기: 생성됨, 발송 전
    PROCESSING,   // 처리중: 발송 시도 중
    SENT,         // 성공: 발송 완료
    FAILED        // 실패: 발송 실패
}
