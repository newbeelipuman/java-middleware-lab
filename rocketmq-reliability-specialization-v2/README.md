# RocketMQ Reliability Specialization V2

Independent Stage 2 project derived from the `rocketmq-notification-demo`
baseline. It uses a desensitized shift-change scenario and does not modify or
reuse the baseline runtime state.

## Reliability paths

- Default `outbox` mode commits `shift_change_request` and `outbox_event` in one
  MySQL transaction.
- The publisher claims rows with `FOR UPDATE SKIP LOCKED`, increments a fencing
  token, retries with exponential backoff, and moves exhausted rows to `DEAD`.
- Profile `transaction-message` sends a RocketMQ half message, executes the
  local database transaction, and checks `transaction_event_fact` during broker
  transaction checks.
- Consumers route by `eventType + schemaVersion`. Unsupported or malformed
  events enter `quarantined_message`.
- `SMS`, `EMAIL`, and `IN_APP` have independent tasks keyed by
  `eventId + channel`.
- The downstream simulator persists the same idempotency key before returning.
  `staffCode=TIMEOUT_ONCE` demonstrates a committed side effect followed by an
  uncertain response.

## Run

```powershell
docker compose up -d
mvn spring-boot:run
```

Ports: application `8091`, MySQL `3308`, NameServer `9877`, Broker `11911`.

```powershell
$body = '{"staffCode":"S-001","fromShift":"DAY","toShift":"NIGHT"}'
Invoke-RestMethod -Method Post http://127.0.0.1:8091/api/shift-changes `
  -ContentType application/json -Body $body
Invoke-RestMethod http://127.0.0.1:8091/api/admin/outbox
```

Transaction-message comparison:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=transaction-message"
```

## Admin API

```text
GET  /api/admin/outbox
GET  /api/admin/tasks
GET  /api/admin/quarantine
POST /api/admin/outbox/{id}/reactivate
POST /api/admin/tasks/{id}/reactivate
POST /api/admin/quarantine/{id}/replay
```

Errors contain `code`, `message`, `resource`, and `traceId`.

## Validation

```powershell
mvn test
.\failure-drills\broker-pause-recover.ps1
.\k6\run-three-rounds.ps1
```

The local single-broker topology demonstrates at-least-once processing,
recovery, and business idempotency. It does not demonstrate exact-once
delivery, broker high availability, production capacity, or an SLA.
