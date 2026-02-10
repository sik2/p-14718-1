# Outbox Pattern 구현

## 개요
Spring Boot 4.0.1 + Kafka 기반 마이크로서비스 아키텍처에 Outbox 패턴을 적용하여 DB 트랜잭션과 메시지 발행의 원자성을 보장합니다.

## 기술 스택
- Java 25
- Spring Boot 4.0.1
- Spring Data JPA
- Spring Kafka (Redpanda)
- MySQL / H2

## 서비스 구성
| 서비스 | 포트 | 설명 |
|--------|------|------|
| member-service | 8080 | 회원 관리 |
| post-service | 8081 | 게시글 관리 |
| payout-service | 8082 | 정산 관리 |
| cash-service | 8083 | 지갑/결제 관리 |
| market-service | 8084 | 상품/주문 관리 |

## Outbox 패턴 구조

### 기존 문제
```
도메인 → EventPublisher → Kafka 직접 발행
                              ↑
                        원자성 보장 안됨
                        (DB 커밋 후 Kafka 실패 시 메시지 유실)
```

### Outbox 패턴 적용 후
```
도메인 → EventPublisher → OutboxPublisher → OUTBOX_EVENT 테이블 (같은 트랜잭션)
                                                    ↓
                              OutboxPoller (5초 폴링) → Kafka
```

### 핵심 컴포넌트
| 컴포넌트 | 역할 |
|----------|------|
| OutboxEvent | Outbox 테이블 엔티티 |
| OutboxPublisher | 이벤트를 Outbox 테이블에 저장 |
| OutboxPoller | 주기적으로 Outbox 조회 후 Kafka 발행 |

## 실행 방법

### 로컬 개발
```bash
# Kafka(Redpanda) 실행
docker-compose up -d

# 서비스 실행
./gradlew :member-service:bootRun
./gradlew :post-service:bootRun
./gradlew :cash-service:bootRun
./gradlew :market-service:bootRun
./gradlew :payout-service:bootRun
```

### Kubernetes 배포
```bash
kubectl apply -k k8s/
```

## 설정

### Outbox 설정 (application.yml)
```yaml
outbox:
  enabled: true
  poller:
    interval-ms: 5000      # 폴링 주기
    batch-size: 100        # 배치 크기
    max-retry: 5           # 최대 재시도
```