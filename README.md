# Payment & Settlement System

포트원(PortOne) PG사와 연동한 결제 처리 및 파트너사 정산 자동화 시스템입니다.
결제 승인부터 취소, 일별 정산 집계까지의 흐름을 구현했습니다.

---

## 기술 스택

| 분류            | 기술              |
|---------------|-----------------|
| Language      | Java 17         |
| Framework     | Spring Boot 3.4 |
| ORM           | Spring Data JPA |
| Database      | MySQL 8.0       |
| Migration     | Flyway          |
| Message Queue | RabbitMQ        |
| 알림            | Slack Webhook   |
| 분산 잠금         | ShedLock        |
| 기타            | Lombok          |

---

## 시스템 개요

```
클라이언트
    │
    ├─ 결제 승인 (포트원 webhook)
    │       └─ 결제 내역 저장 (PAID)
    │
    ├─ 결제 취소 요청
    │       └─ CANCEL_REQUESTED 저장 → MQ 발행 (즉시 응답)
    │               └─ Consumer: PG API 호출
    │                       ├─ 성공 → CANCELED
    │                       └─ 실패 (3회) → DLQ
    │                               └─ 실패 이력 저장 + CANCEL_FAILED + Slack 긴급 알림
    │
    └─ 정산 (매일 자동 실행)
            └─ 전일 PAID 결제 기준 파트너사별 금액 집계 → 중복 방지 후 정산 저장
```

---

## 주요 기능

### 1. 결제 처리

포트원 PG사로부터 결제 완료 webhook을 수신하여 결제 내역을 저장합니다.
PAID 상태의 결제만 유효한 것으로 처리하며, 이외의 상태는 예외로 처리합니다.

**결제 상태 흐름**

```
PAID → CANCEL_REQUESTED → CANCELED
                        → CANCEL_FAILED
```

### 2. 결제 취소 및 실패 후처리

결제 취소 요청이 들어오면 즉시 `CANCEL_REQUESTED` 상태로 저장하고 MQ에 발행합니다.
PG API 통신에는 연결 타임아웃 3초, 읽기 타임아웃 5초를 설정해 무한 대기를 방지합니다.

```
취소 요청 → CANCEL_REQUESTED 저장 → MQ 발행 (즉시 응답)
                                          │
                                          ▼
                                    Cancel Consumer
                                    PG API 취소 호출
                                          ├─ HTTP 401 → 즉시 DLQ (인증 오류)
                                          ├─ code != 0 → 즉시 DLQ (PG사 비즈니스 거절)
                                          ├─ 성공 (code == 0) → CANCELED
                                          └─ 일시적 장애 → Spring AMQP Retry (3회, 1초 간격)
                                                         └─ 재시도 소진 → DLQ 자동 라우팅
                                                                    └─ CANCEL_FAILED 저장
                                                                       실패 이력 저장
                                                                       Slack 긴급 알림
```

**RabbitMQ Retry 설정**

Spring AMQP의 리스너 레벨 재시도를 사용합니다. 애플리케이션 코드에서 별도의 재시도 로직 없이 설정만으로 동작합니다.

| 항목        | 값                                                                |
|-----------|------------------------------------------------------------------|
| 최대 재시도 횟수 | 3회                                                               |
| 재시도 간격    | 1초                                                               |
| 실패 시 처리   | DLQ 자동 라우팅 (requeue 없음)                                          |
| DLQ 메시지   | `RepublishMessageRecoverer`를 통해 예외 메시지(`x-exception-message`) 포함 |

DLQ는 자동 재처리 없이 수동 개입 트리거로만 사용합니다.
원인 파악 후 수동으로 재발행하는 구조로, 무의미한 반복 실패를 방지합니다.

**취소 결과 수신 (폴링)**

취소가 비동기로 처리되므로 프론트에서 폴링으로 최종 상태를 확인합니다.

| 항목       | 값                                     |
|----------|---------------------------------------|
| 조회 간격    | 2초                                    |
| 최대 시도 횟수 | 10회 (20초)                             |
| 종료 조건    | `CANCELED` 또는 `CANCEL_FAILED` 수신 시 중단 |

> 추후 SSE(Server-Sent Events) 방식으로 개선 예정입니다.

### 3. 파트너사 일별 정산 자동화

매일 전일 `PAID` 결제 데이터를 기준으로 파트너사별 정산 금액을 집계하고 저장합니다.

**정산 흐름**

```
스케줄러 실행 (매일 새벽 1시)
    └─ 전일 00:00 ~ 23:59:59 범위 PAID 결제 조회
            └─ 파트너사별 합산
                    └─ 중복 정산 여부 확인 (partner_id + settlement_date UNIQUE)
                            └─ 신규 건만 settlements 테이블에 COMPLETED 상태로 저장
```

**정산 서비스 구조**

스케줄러(`SettlementScheduledTasks`)와 비즈니스 로직(`SettlementService`)을 분리하여 SRP를 적용합니다.

| 클래스                        | 역할                                          |
|----------------------------|---------------------------------------------|
| `SettlementScheduledTasks` | 스케줄 트리거만 담당, `SettlementService`에 위임        |
| `SettlementService`        | 결제 조회 → 파트너사별 집계 → 정산 저장 (`@Transactional`) |

중복 정산 여부 확인은 파트너 수만큼 개별 쿼리를 날리는 대신, 해당 날짜에 이미 정산된 파트너 ID를 한 번에 조회(`IN` 쿼리) 후 메모리에서 필터링합니다.

**ShedLock (분산 환경 중복 실행 방지)**

다중 인스턴스 환경에서 스케줄러가 중복 실행되지 않도록 ShedLock을 적용합니다.
DB 기반 잠금(`shedlock` 테이블)을 사용하며, 잠금 유지 시간은 최소 30초 / 최대 30초입니다.

| 항목       | 값                                                |
|----------|--------------------------------------------------|
| 운영 스케줄   | 매일 새벽 1시 (`0 0 1 * * ?`) ← 현재 설정               |
| 잠금 유지 시간 | 최소 30초 / 최대 30초                                  |
| 중복 정산 방지 | `partner_id + settlement_date` UNIQUE 제약으로 이중 보호 |

> 운영 배포 시 스케줄 cron을 `0 0 1 * * ?`으로 변경해야 합니다.

---

## 프로젝트 구조

```
src/main/java/com/example/paymentsystem/
├── common/
│   └── config/       # RabbitMQ, Async, RestClient, ShedLock 설정
├── payment/
│   ├── controller/   # REST API
│   ├── service/      # 결제 비즈니스 로직, 실패 핸들러, Slack
│   ├── messaging/    # Cancel Consumer, DLQ Consumer
│   ├── entity/       # Payment, PaymentCancelFailureHistory
│   ├── repository/
│   ├── util/         # PortOne API Client
│   └── dto/
└── settlement/
    ├── entity/       # Settlement
    ├── repository/
    └── service/      # 일일 정산 스케줄러
```

---

## API

### REST API

| Method | URL                             | 설명                  |
|--------|---------------------------------|---------------------|
| POST   | `/api/payment/portone`          | 포트원 결제 결과 저장        |
| GET    | `/api/payment/list`             | 결제 내역 조회            |
| GET    | `/api/payment/{imp_uid}/status` | 결제 상태 조회            |
| DELETE | `/api/payment/cancel/{imp_uid}` | 결제 취소 요청            |
| POST   | `/api/payment/dummy/{count}`    | 더미 결제 데이터 생성 (테스트용) |

### 화면

| URL               | 설명           |
|-------------------|--------------|
| `/payment`        | 결제 페이지       |
| `/payment/mypage` | 결제 내역 조회 페이지 |

---

## 로컬 실행

**요구사항:** Java 17, MySQL 8.0, RabbitMQ

`src/main/resources/application-local.yml` 파일을 생성하고 아래 항목을 설정합니다.

```yaml
DB_URL: jdbc:mysql://localhost:3306/payment
DB_USERNAME:
DB_PASSWORD:

IMP_CODE:
IMP_KEY:
IMP_SECRET:

USER_EMAIL:
USER_NAME:

SLACK_WEBHOOK_URL:

RABBITMQ_HOST: localhost
RABBITMQ_PORT: 5672
RABBITMQ_USERNAME: guest
RABBITMQ_PASSWORD: guest
```

> `application-local.yml`은 `.gitignore`에 포함되어 있습니다.

DB 스키마는 Flyway가 애플리케이션 시작 시 자동으로 생성합니다.

---

## 트러블슈팅

### 1. 정산 N+1 쿼리 문제

**문제점 및 개선 포인트**

파트너사별 중복 정산 여부를 확인할 때, 파트너사 수만큼 DB 쿼리가 개별 호출되고 있었습니다.

**원인 분석**

`stream().filter()` 내부에서 `existsByPartnerIdAndSettlementDate()`를 호출하는 구조로, 파트너사가 100개면 100번의 SELECT 쿼리가 발생했습니다.

**개선 방안 및 시도 과정**

해당 날짜에 이미 정산된 파트너 ID 전체를 `findPartnerIdsBySettlementDate()`로 한 번에 조회한 뒤, 메모리에서 `Set.contains()`로 필터링하는 방식으로 변경했습니다.

- 개선 전: `existsByPartnerIdAndSettlementDate()` → 파트너사 N개 = 쿼리 N번
- 개선 후: `findPartnerIdsBySettlementDate()` → 쿼리 1번 + 메모리 필터링

**결과**

파트너사 N개 기준 N+1번 쿼리 → 2번(중복 확인 1번 + saveAll 1번)으로 감소했습니다.

---

### 2. 결제 취소 시 PG API 응답 지연으로 인한 클라이언트 블로킹

**문제점 및 개선 포인트**

결제 취소 요청 시 PG API를 동기로 호출하고 있어, PG사 응답이 지연되면 클라이언트가 그대로 블로킹되는 문제가 있었습니다. 네트워크 지연이나 PG사 장애 상황에서 사용자
경험이 크게 저하될 수 있었습니다.

**원인 분석**

취소 API가 PG API 호출 → 결과 저장까지 하나의 동기 흐름으로 묶여 있었습니다. PG API의 타임아웃(연결 3초, 읽기 5초)이 발생하면 클라이언트도 그만큼 대기해야
했습니다.

**개선 방안 및 시도 과정**

취소 요청을 즉시 `CANCEL_REQUESTED` 상태로 저장하고 MQ에 발행한 뒤 클라이언트에 즉시 응답하는 방식으로 전환했습니다. PG API 호출은 Consumer가
비동기로 처리합니다.

```
취소 요청 수신
    → CANCEL_REQUESTED 저장 (즉시 응답)
    → MQ 발행
        → Consumer: PG API 호출
            ├─ 성공 → CANCELED
            └─ 실패 → retry 3회 → DLQ → CANCEL_FAILED + Slack 알림
```

취소 결과는 프론트에서 2초 간격 폴링(최대 10회)으로 확인합니다.

**결과**

클라이언트 응답 시간이 PG API 응답 시간에 종속되지 않게 되었습니다. PG사 장애 상황에서도 취소 요청 자체는 정상 접수되며, 실패 시 DLQ와 Slack 알림으로 운영자가
인지할 수 있습니다.

---

### 3. 취소 실패 시 단순 재시도로 인한 불필요한 DLQ 적재

**문제점 및 개선 포인트**

취소 실패 시 모든 예외를 동일하게 처리하면, 영구적인 오류(잘못된 요청 등)도 재시도를 반복하다 DLQ로 이동하게 됩니다. 재시도가 의미 없는 케이스에 리소스가 낭비되는 문제가
있었습니다.

**원인 분석**

실패 원인을 구분하지 않고 모든 예외에 동일한 retry 정책을 적용하고 있었습니다. 일시적 장애(네트워크 오류)는 재시도가 유효하지만, 영구적 장애(잘못된 파라미터 등)는
재시도해도 동일하게 실패합니다.

**개선 방안 및 시도 과정**

1차로 예외 종류에 따라 처리 방식을 분리했습니다.

- `RestClientException` (일시적 장애): Spring AMQP retry에 위임 (3회, 1초 간격)
- `HttpClientErrorException.Unauthorized` (HTTP 401): 인증 오류는 재시도해도 의미 없으므로 즉시 DLQ (`RestClientException` 하위 클래스이므로 먼저 catch)
- `Exception` (영구적 장애): `AmqpRejectAndDontRequeueException`으로 즉시 DLQ 라우팅

그러나 실제 동작을 확인하자 영구적 장애도 3회 재시도 후 DLQ로 이동하는 문제가 있었습니다.

`AmqpRejectAndDontRequeueException`은 메시지 requeue를 방지하는 역할이지, Spring AMQP 리스너 레벨 retry를 건너뛰지는 않습니다. retry 인터셉터는 `consume()` 메서드 전체를 감싸고 있어, `AmqpRejectAndDontRequeueException`도 `Exception`의 하위 클래스이므로 `SimpleRetryPolicy` 기본 설정에서 재시도 대상이 됩니다.

이를 해결하기 위해 프로퍼티 기반 retry 설정을 제거하고, `SimpleRabbitListenerContainerFactory`를 직접 빈으로 등록해 `SimpleRetryPolicy`에 `AmqpRejectAndDontRequeueException → non-retryable`을 명시했습니다. 미분류 예외는 기본적으로 retry 대상으로 유지했습니다.

또한 `RepublishMessageRecoverer`를 적용해 DLQ로 이동하는 메시지에 예외 정보(`x-exception-message`)를 포함시켜 원인 파악이 가능하도록 했습니다.

**결과**

일시적 장애는 3회 재시도 후 DLQ로 이동하고, `AmqpRejectAndDontRequeueException`으로 분류된 영구적 장애는 재시도 없이 즉시 DLQ로 라우팅됩니다.

---

### 4. 다중 인스턴스 환경에서 정산 스케줄러 중복 실행

**문제점 및 개선 포인트**

스케줄러가 여러 인스턴스에서 동시에 실행되면 같은 날짜에 대해 정산이 중복으로 처리될 수 있었습니다.

**원인 분석**

Spring의 `@Scheduled`는 각 인스턴스에서 독립적으로 실행됩니다. 인스턴스가 2개라면 스케줄러도 2번 실행되어 동일한 정산 데이터가 중복 저장될 수 있었습니다.

**개선 방안 및 시도 과정**

두 가지 방어 레이어를 적용했습니다.

- **ShedLock**: `@SchedulerLock`으로 DB 기반 분산 잠금을 적용해 하나의 인스턴스만 스케줄러를 실행하도록 보장
- **UNIQUE 제약**: `settlements` 테이블에 `(partner_id, settlement_date)` UNIQUE KEY 추가로, ShedLock 장애 시에도 DB 레벨에서 중복 정산을 차단

**결과**

ShedLock으로 정상 운영 시 중복 실행을 원천 차단하고, UNIQUE 제약이 최후 안전망으로 이중 보호 구조를 갖추었습니다.

---

## 개발 예정

- [ ] 취소 결과 수신 방식 SSE(Server-Sent Events)로 개선 (현재 폴링)
- [ ] 정산 스케줄러 Spring Batch로 전환 (청크 처리, 실패 재시작, Job 이력 관리, 정산 상태 관리)
- [ ] 운영 배포 시 정산 스케줄 cron `0 0 1 * * ?`으로 변경
