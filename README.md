# Payment & Settlement System

포트원(PortOne) PG사와 연동한 결제 처리 및 파트너사 정산 자동화 시스템입니다.
결제 승인부터 취소, 일별 정산 집계까지의 흐름을 구현했습니다.

---

## 기술 스택

| 분류            | 기술                   |
|---------------|----------------------|
| Language      | Java 17              |
| Framework     | Spring Boot 3.4      |
| ORM           | Spring Data JPA      |
| Database      | MySQL 8.0            |
| Migration     | Flyway               |
| Message Queue | RabbitMQ             |
| 알림            | Slack Webhook        |
| 분산 잠금         | ShedLock             |
| 기타            | Lombok               |

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
                                          ├─ 성공 → CANCELED
                                          └─ 실패 → Spring AMQP Retry (3회, 1초 간격)
                                                         └─ 재시도 소진 → DLQ 자동 라우팅
                                                                    └─ CANCEL_FAILED 저장
                                                                       실패 이력 저장
                                                                       Slack 긴급 알림
```

**RabbitMQ Retry 설정**

Spring AMQP의 리스너 레벨 재시도를 사용합니다. 애플리케이션 코드에서 별도의 재시도 로직 없이 설정만으로 동작합니다.

| 항목 | 값 |
|------|---|
| 최대 재시도 횟수 | 3회 |
| 재시도 간격 | 1초 |
| 실패 시 처리 | DLQ 자동 라우팅 (requeue 없음) |
| DLQ 메시지 | `RepublishMessageRecoverer`를 통해 예외 메시지(`x-exception-message`) 포함 |

DLQ는 자동 재처리 없이 수동 개입 트리거로만 사용합니다.
원인 파악 후 수동으로 재발행하는 구조로, 무의미한 반복 실패를 방지합니다.

**취소 결과 수신 (폴링)**

취소가 비동기로 처리되므로 프론트에서 폴링으로 최종 상태를 확인합니다.

| 항목 | 값 |
|------|---|
| 조회 간격 | 2초 |
| 최대 시도 횟수 | 10회 (20초) |
| 종료 조건 | `CANCELED` 또는 `CANCEL_FAILED` 수신 시 중단 |

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

**ShedLock (분산 환경 중복 실행 방지)**

다중 인스턴스 환경에서 스케줄러가 중복 실행되지 않도록 ShedLock을 적용합니다.
DB 기반 잠금(`shedlock` 테이블)을 사용하며, 잠금 유지 시간은 최소 30초 / 최대 30초입니다.

| 항목 | 값 |
|------|---|
| 운영 스케줄 | 매일 새벽 1시 (`0 0 1 * * ?`) |
| 테스트 스케줄 | 매 1분 (`0 * * * * ?`) ← 현재 설정 |
| 잠금 유지 시간 | 최소 30초 / 최대 30초 |
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

| Method | URL                              | 설명            |
|--------|----------------------------------|---------------|
| POST   | `/api/payment/portone`           | 포트원 결제 결과 저장          |
| GET    | `/api/payment/list`              | 결제 내역 조회               |
| GET    | `/api/payment/{imp_uid}/status`  | 결제 상태 조회               |
| DELETE | `/api/payment/cancel/{imp_uid}`  | 결제 취소 요청               |
| POST   | `/api/payment/dummy/{count}`     | 더미 결제 데이터 생성 (테스트용)  |

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

## 개발 예정

- [ ] 취소 결과 수신 방식 SSE(Server-Sent Events)로 개선 (현재 폴링)
- [ ] 정산 스케줄러 Spring Batch로 전환 (청크 처리, 실패 재시작, Job 이력 관리, 정산 상태 관리)
- [ ] 운영 배포 시 정산 스케줄 cron `0 0 1 * * ?`으로 변경
