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
    //    - Kafka로 발행
    //    - 성공 시 SENT, 실패 시 PENDING (재시도) 또는 FAILED
}
```

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

---

# 0004 - EventPublisher 및 KafkaConfig 수정

## 개요
기존 EventPublisher를 Outbox 패턴과 통합하고, 병행 운영 모드 지원

## EventPublisher 변경
```java
@Value("${outbox.enabled:false}")
private boolean outboxEnabled;

public void publish(Object event) {
    // 로컬 이벤트 발행 (동일 서비스 내 리스너)
    applicationEventPublisher.publishEvent(event);

    // 원격 이벤트 발행
    if (outboxEnabled) {
        outboxPublisher.saveToOutbox(event);  // Outbox 패턴
    } else {
        kafkaEventPublisher.publish(event);   // 기존 방식
    }
}
```

## 병행 운영 모드
- `outbox.enabled=false`: 기존 Kafka 직접 발행 (기본값)
- `outbox.enabled=true`: Outbox 패턴 사용

## KafkaConfig 변경
Outbox용 KafkaTemplate 추가:
```java
@Bean
public KafkaTemplate<String, String> outboxKafkaTemplate() {
    // acks=all: 모든 복제본에 기록 확인
    // enable.idempotence=true: 멱등성 보장
    // retries=3: 재시도 횟수
}
```

## 변경 파일
```
common/src/main/java/com/back/global/
├── eventPublisher/
│   └── EventPublisher.java
└── kafka/
    └── KafkaConfig.java
```

---

# 0005 - cash, market 서비스 Outbox 적용

## 개요
실시간 결제 흐름을 담당하는 cash, market 서비스에 Outbox 패턴 우선 적용

## 결제 흐름
```
market-service                    cash-service
     │                                 │
     │ MarketOrderPaymentRequested     │
     │ ─────────────────────────────>  │
     │                                 │ (결제 처리)
     │ CashOrderPaymentSucceeded       │
     │ <─────────────────────────────  │
     │                                 │
     │ MarketOrderPaymentCompleted     │
     │ ─────────────────────────────>  │ (→ payout-service)
```

## 적용 이유
- **결제/정산**: 메시지 유실 시 금전적 손해 발생
- **트랜잭션 원자성**: 결제 처리와 이벤트 발행의 일관성 필수
- **재시도 보장**: 네트워크 장애 시 자동 재시도

## 설정
```yaml
# cash-service, market-service
outbox:
  enabled: true
  poller:
    enabled: true
    interval-ms: 5000
    batch-size: 100
    max-retry: 5
```

## 변경 파일
```
cash-service/src/main/resources/
└── application.yml

market-service/src/main/resources/
└── application.yml
```

---

# 0006 - payout 서비스 Outbox 적용

## 개요
배치 정산 흐름을 담당하는 payout 서비스에 Outbox 패턴 적용

## 정산 흐름
```
market-service                    payout-service                   cash-service
     │                                 │                                │
     │ MarketOrderPaymentCompleted     │                                │
     │ ─────────────────────────────>  │                                │
     │                                 │ (정산 후보 등록)                 │
     │                                 │                                │
     │                                 │ (배치: 정산 집계/완료)           │
     │                                 │                                │
     │                                 │ PayoutCompletedEvent           │
     │                                 │ ─────────────────────────────> │
     │                                 │                                │ (지갑 입금)
```

## 적용 이유
- **정산 완료 이벤트**: 금액 정산 결과 전달, 유실 불가
- **배치 처리**: 대량 정산 후 이벤트 발행의 원자성 필요

## 설정
```yaml
# payout-service
outbox:
  enabled: true
  poller:
    enabled: true
    interval-ms: 5000
    batch-size: 100
    max-retry: 5
```

## 변경 파일
```
payout-service/src/main/resources/
└── application.yml
```

---

# 0007 - member, post 서비스 Outbox 적용

## 개요
나머지 서비스(member, post)에 Outbox 패턴 적용 완료

## 이벤트 흐름
```
member-service                    post-service / 기타 서비스
     │                                 │
     │ MemberJoinedEvent              │
     │ ─────────────────────────────> │ (회원 정보 동기화)
     │                                 │
     │ MemberModifiedEvent            │
     │ ─────────────────────────────> │ (회원 정보 업데이트)

post-service                      payout-service
     │                                 │
     │ PostCreatedEvent               │
     │ ─────────────────────────────> │ (작성자 활동 추적)
     │                                 │
     │ PostCommentCreatedEvent        │
     │ ─────────────────────────────> │ (댓글 활동 추적)
```

## 적용 이유
- **전체 서비스 일관성**: 모든 서비스에서 동일한 이벤트 발행 패턴 사용
- **데이터 동기화**: 회원/게시글 정보 변경의 안정적인 전파
- **운영 단순화**: 단일 메시징 패턴으로 모니터링/디버깅 용이

## 설정
```yaml
# member-service, post-service
outbox:
  enabled: true
  poller:
    enabled: true
    interval-ms: 5000
    batch-size: 100
    max-retry: 5
```

## 변경 파일
```
member-service/src/main/resources/
└── application.yml

post-service/src/main/resources/
└── application.yml
```

---

# 전체 Outbox 적용 완료

## 서비스별 Outbox 상태
| 서비스 | Outbox 적용 | 주요 이벤트 |
|--------|-------------|-------------|
| member-service | ✅ | MemberJoined, MemberModified |
| post-service | ✅ | PostCreated, PostCommentCreated |
| payout-service | ✅ | PayoutCompleted |
| cash-service | ✅ | CashOrderPaymentSucceeded/Failed |
| market-service | ✅ | MarketOrderPaymentRequested/Completed |

## 아키텍처 다이어그램
```
┌─────────────────────────────────────────────────────────────────┐
│                        각 마이크로서비스                          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │ 도메인 로직  │───>│ Outbox 저장 │───>│ outbox_event 테이블 │  │
│  └─────────────┘    └─────────────┘    └─────────────────────┘  │
│         │                                        │              │
│         │ 같은 트랜잭션                           │ 폴링         │
│         ▼                                        ▼              │
│  ┌─────────────┐                       ┌─────────────────────┐  │
│  │  DB 커밋    │                       │   OutboxPoller      │  │
│  └─────────────┘                       └─────────────────────┘  │
│                                                  │              │
└──────────────────────────────────────────────────│──────────────┘
                                                   │
                                                   ▼
                                          ┌───────────────┐
                                          │     Kafka     │
                                          └───────────────┘
```

## 핵심 보장
1. **원자성**: DB 트랜잭션과 메시지 발행이 함께 성공/실패
2. **최소 1회 전달**: 재시도로 메시지 유실 방지
3. **순서 보장**: 동일 Aggregate의 이벤트는 생성 순서대로 발행
4. **멱등성 권장**: Consumer는 중복 메시지 처리 대비 필요

---

# 0008 - Debezium 인프라 설정

## 개요
CDC(Change Data Capture) 기반 Outbox 패턴을 위한 Debezium 인프라 구성

## Polling vs CDC 비교
| 방식 | 지연시간 | DB 부하 | 인프라 |
|------|----------|---------|--------|
| Polling | 5초 | 주기적 쿼리 | 없음 |
| CDC | ~ms | binlog 읽기 | Debezium |

## 아키텍처
```
┌─────────────┐     ┌─────────────┐     ┌──────────────┐     ┌─────────┐
│ Application │────>│   MySQL     │────>│   Debezium   │────>│  Kafka  │
│             │     │  (binlog)   │     │  (Connector) │     │         │
└─────────────┘     └─────────────┘     └──────────────┘     └─────────┘
       │                                        │
       │ INSERT INTO outbox_event              │ binlog 감지
       └────────────────────────────────────────┘
```

## MySQL binlog 설정
```yaml
# docker-compose.yml
mysql-service:
  command: --server-id=1 --log-bin=mysql-bin --binlog-format=ROW --binlog-row-image=FULL
```

## Debezium 컨테이너
```yaml
# docker-compose.yml
debezium:
  profiles:
    - cdc
  image: debezium/connect:2.5
  ports:
    - "8083:8083"
  environment:
    BOOTSTRAP_SERVERS: redpanda:29092
```

## Connector 등록
```bash
./script/register-debezium-connector.sh
```

## 변경 파일
```
docker-compose.yml                          # Debezium 컨테이너 추가
script/register-debezium-connector.sh       # Connector 등록 스크립트
```

---

# 0009 - CDC 모드 전환

## 개요
CDC 사용 시 OutboxPoller 비활성화 방법

## 모드 비교
| 모드 | Poller | 이벤트 발행 |
|------|--------|-------------|
| Polling | `enabled: true` | OutboxPoller (5초마다) |
| CDC | `enabled: false` | Debezium (binlog 즉시) |

## OutboxPoller 조건부 생성
```java
@ConditionalOnProperty(name = "outbox.poller.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPoller {
```
- `outbox.poller.enabled=true` → Poller 빈 생성
- `outbox.poller.enabled=false` → Poller 빈 생성 안됨

## CDC 모드 전환
```yaml
# application.yml
outbox:
  poller:
    enabled: false  # CDC 사용 시 false로 변경
```

## 실행 방법
```bash
# 1. Debezium 실행
docker-compose --profile cdc up -d

# 2. Connector 등록
./script/register-debezium-connector.sh

# 3. 서비스 실행
./gradlew :member-service:bootRun
```
