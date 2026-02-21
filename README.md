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
| 기타            | Spring Retry, Lombok |

---

## 시스템 개요

```
클라이언트
    │
    ├─ 결제 승인 (포트원 webhook)
    │       └─ 결제 내역 저장 (PAID)
    │
    ├─ 결제 취소 요청
    │       ├─ 성공 → CANCELED
    │       └─ 실패 → 실패 이력 저장 + MQ 후처리
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

결제 취소 요청 시 PG사 API를 호출하며, 실패한 경우 아래 흐름으로 후처리합니다.

```
PG API 호출 실패 (@Retryable, 2회)
    ├─ 실패 이력 DB 저장
    ├─ Slack 알림
    └─ RabbitMQ 재시도 큐 발행
             │
             ▼
   Consumer: 1회 재시도
             ├─ 성공 → CANCELED
             └─ 실패 → DLQ 라우팅
                          └─ Slack 긴급 알림 (수동 처리)
```

Consumer에서 실패 원인을 분류해 처리합니다.

- `RestClientException` : PG사 일시 장애 등 네트워크 계열 → `warn` 로그 후 DLQ
- 그 외 `Exception` : 코드 버그, 잘못된 데이터 등 영구 장애 → `error` 로그 후 DLQ

DLQ는 자동 재처리 없이 수동 개입 트리거로만 사용합니다.
원인 파악 후 수동으로 재발행하는 구조로, 무의미한 반복 실패를 방지합니다.

### 3. 파트너사 정산 자동화 (개발 중)

매일 전일 결제 데이터를 기준으로 파트너사별 정산 금액을 집계하고 저장합니다.

---

## 프로젝트 구조

```
src/main/java/com/example/paymentsystem/
├── payment/
│   ├── config/       # RabbitMQ, Async, Retry 설정
│   ├── controller/   # REST API
│   ├── service/      # 결제 비즈니스 로직, 실패 핸들러, Slack
│   ├── messaging/    # MQ Consumer (재시도, DLQ)
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
| GET    | `/api/payment/cancel/{imp_uid}` | 결제 취소 요청     |

### 화면

| URL               | 설명           |
|-------------------|--------------|
| `/payment`        | 결제 페이지       |
| `/payment/mypage` | 결제 내역 조회 페이지 |

---

## 로컬 실행

**요구사항:** Java 17, MySQL 8.0, RabbitMQ

`application.properties`에 아래 항목을 설정합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/payment
spring.datasource.username=
spring.datasource.password=
payment.imp-key=
payment.imp-secret=
slack.webhook.url=
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

DB 스키마는 Flyway가 애플리케이션 시작 시 자동으로 생성합니다.

---

## 개발 예정

- [ ] 파트너사별 일일 자동 정산 완성
- [ ] 정산 상태 관리 (REQUESTED → PROCESSING → COMPLETED)
