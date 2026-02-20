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
    // 3. OutboxEvent 생성 및 저장 (eventType = FQCN)
}
```

### eventType에 FQCN 사용
- **FQCN** (Fully Qualified Class Name): 패키지 경로를 포함한 전체 클래스명
- 예: `com.back.shared.member.event.MemberJoinedEvent`
- Consumer의 `JsonDeserializer`가 `__TypeId__` 헤더에서 FQCN을 읽어 역직렬화 타입 결정
- `getSimpleName()` (단순 클래스명) 사용 시 Consumer에서 타입 매핑 불가

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

---

# 0003 - OutboxPoller

## 개요
주기적으로 Outbox 테이블을 폴링하여 Kafka로 발행하는 스케줄러

## 핵심 로직
```java
@Scheduled(fixedDelayString = "${outbox.poller.interval-ms:5000}")
@Transactional
public void pollAndPublish() {
    // 1. PENDING 상태 이벤트 조회 (배치 크기만큼)
    // 2. 각 이벤트에 대해:
    //    - PROCESSING으로 상태 변경
    //    - ProducerRecord에 __TypeId__ 헤더 추가 (FQCN)
    //    - Kafka로 발행
    //    - 성공 시 SENT, 실패 시 PENDING (재시도) 또는 FAILED
}
```

### __TypeId__ 헤더
- Consumer의 `JsonDeserializer`가 `USE_TYPE_INFO_HEADERS = true`일 때 이 헤더로 역직렬화 타입 결정
- OutboxPublisher에서 저장한 FQCN을 헤더에 추가
- 예: `__TypeId__: com.back.shared.member.event.MemberJoinedEvent`

## 설정
```yaml
outbox:
  poller:
    enabled: true          # 폴러 활성화
    interval-ms: 5000      # 폴링 주기 (5초)
    batch-size: 100        # 배치 크기
    max-retry: 5           # 최대 재시도 횟수
  cleanup:
    cron: "0 0 3 * * *"    # 정리 스케줄 (매일 새벽 3시)
```

## 재시도 전략
- 발행 실패 시 `retryCount` 증가
- `retryCount < maxRetry`: PENDING으로 복귀 (다음 폴링에서 재시도)
- `retryCount >= maxRetry`: FAILED로 변경 (수동 처리 필요)

## 정리 스케줄
- 매일 새벽 3시 실행
- 7일 이상 지난 SENT 이벤트 삭제

## 변경 파일
```
common/src/main/java/com/back/global/outbox/
└── OutboxPoller.java
```
