# Payment & Settlement System

포트원(PortOne) PG사와 연동한 결제 처리 및 파트너사 정산 자동화 시스템입니다.
결제 승인부터 취소, 일별 정산 집계까지의 흐름을 구현했습니다.

> 정산 기능은 현재 개발 중입니다.

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
    └─ 정산 (매일 자동 실행)  [개발 중]
            └─ 전일 결제 기준 파트너사별 금액 집계 → 정산 저장
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

### 3. 파트너사 정산 자동화 (개발 중)

매일 전일 결제 데이터를 기준으로 파트너사별 정산 금액을 집계하고 저장합니다.

---

## 프로젝트 구조

```
src/main/java/com/example/paymentsystem/
├── payment/
│   ├── config/       # RabbitMQ, Async 설정
│   ├── controller/   # REST API
│   ├── service/      # 결제 비즈니스 로직, 실패 핸들러, Slack
│   ├── messaging/    # Cancel Consumer, DLQ Consumer
│   ├── entity/       # Payment, PaymentCancelFailureHistory
│   ├── repository/
│   ├── util/         # PortOne API Client
│   └── dto/
└── settlement/       # 정산 (개발 중)
    ├── entity/       # Settlement
    ├── repository/
    └── service/      # 일일 정산 스케줄러
```

---

## API

### REST API

| Method | URL                             | 설명           |
|--------|---------------------------------|--------------|
| POST   | `/api/payment/portone`          | 포트원 결제 결과 저장 |
| GET    | `/api/payment/list`             | 결제 내역 조회     |
| DELETE | `/api/payment/cancel/{imp_uid}` | 결제 취소 요청     |

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

- [ ] 파트너사별 일일 자동 정산 완성
- [ ] 정산 상태 관리 (REQUESTED → PROCESSING → COMPLETED)
