# Outbox Pattern 기반 마이크로서비스

## 개요
Spring Boot 4.0.1 + Kafka 기반 마이크로서비스에 Outbox 패턴을 적용하여 DB 트랜잭션과 메시지 발행의 원자성을 보장합니다.

## 서비스 구성
| 서비스 | 포트 | 설명 |
|--------|------|------|
| member-service | 8080 | 회원 관리 |
| post-service | 8081 | 게시글 관리 |
| payout-service | 8082 | 정산 관리 |
| cash-service | 8083 | 지갑/결제 관리 |
| market-service | 8084 | 상품/주문 관리 |

---

# 0001 - Outbox 엔티티 및 Repository

## 개요
Outbox 패턴의 핵심 데이터 모델 구현

## OutboxStatus
```java
public enum OutboxStatus {
    PENDING,      // 대기: 생성됨, 발송 전
    PROCESSING,   // 처리중: 발송 시도 중
    SENT,         // 성공: 발송 완료
    FAILED        // 실패: 발송 실패
}
```

### 상태 흐름
```
PENDING → PROCESSING → SENT
                     ↘ FAILED
```

## OutboxEvent 엔티티
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| aggregateType | String | 집합체 타입 (Order, Member 등) |
| aggregateId | String | 집합체 ID |
| eventType | String | 이벤트 클래스명 |
| topic | String | Kafka 토픽 |
| payload | LONGTEXT | JSON 페이로드 |
| status | OutboxStatus | PENDING/PROCESSING/SENT/FAILED |
| createDate | LocalDateTime | 생성 시각 |
| sentDate | LocalDateTime | 발송 완료 시각 |
| retryCount | int | 재시도 횟수 |
| lastErrorMessage | String | 마지막 에러 메시지 |
| version | Long | 낙관적 락 |

### 인덱스 전략
- `idx_outbox_status_created`: 폴링 시 PENDING 상태 조회 최적화
- `idx_outbox_aggregate`: 특정 Aggregate 이벤트 조회

## 변경 파일
```
common/src/main/java/com/back/global/outbox/
├── entity/
│   ├── OutboxEvent.java
│   └── OutboxStatus.java
└── repository/
    └── OutboxEventRepository.java
```

---

# 0002 - OutboxPublisher

## 개요
이벤트를 Outbox 테이블에 저장하는 컴포넌트

## 핵심 로직
```java
@Transactional(propagation = Propagation.MANDATORY)
public void saveToOutbox(Object event) {
    // 1. 이벤트에서 메타데이터 추출 (aggregateType, aggregateId, topic)
    // 2. JSON 직렬화
    // 3. OutboxEvent 생성 및 저장
}
```

### Propagation.MANDATORY
- 기존 트랜잭션이 **반드시** 존재해야 함
- 트랜잭션 없이 호출 시 예외 발생
- 도메인 로직과 Outbox 저장이 같은 트랜잭션에서 실행됨을 보장

## 이벤트 → 메타데이터 매핑
| 이벤트 | aggregateType | topic |
|--------|---------------|-------|
| MemberJoinedEvent | Member | member.joined |
| MemberModifiedEvent | Member | member.modified |
| PostCreatedEvent | Post | post.created |
| PostCommentCreatedEvent | PostComment | post.comment.created |
| MarketOrderPaymentRequestedEvent | Order | market.order.payment.requested |
| MarketOrderPaymentCompletedEvent | Order | market.order.payment.completed |
| CashOrderPaymentSucceededEvent | Order | cash.order.payment.succeeded |
| CashOrderPaymentFailedEvent | Order | cash.order.payment.failed |
| PayoutCompletedEvent | Payout | payout.completed |

## 변경 파일
```
common/src/main/java/com/back/global/outbox/
└── OutboxPublisher.java
```
