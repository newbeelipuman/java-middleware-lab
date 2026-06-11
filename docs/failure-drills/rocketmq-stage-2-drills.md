# RocketMQ Stage 2 failure drills

## Automated evidence

- MySQL 8.4 Testcontainers verifies atomic business/Outbox commit.
- Two workers use `FOR UPDATE SKIP LOCKED` to claim different rows.
- An expired worker is fenced after takeover and cannot mark the event sent.
- A downstream delivery committed before a simulated timeout is not repeated.
- Transaction checks map committed facts to `COMMIT` and database failures to
  `UNKNOWN`.

## Reproducible Compose drills

- Pause the broker with `failure-drills/broker-pause-recover.ps1`; creation still
  commits to MySQL, Outbox publication fails and retries after broker recovery.
- Stop the application after events accumulate, restart it, and observe consumer
  backlog recovery through task and metric endpoints.
- Submit `staffCode=TIMEOUT_ONCE` to verify downstream idempotency.
- Submit `staffCode=POISON` to drive broker retries and eventual DLQ quarantine.
- Run the `transaction-message` profile, pause MySQL during transaction checks,
  and verify `UNKNOWN` rather than an invented commit decision.

Exact timings and DLQ arrival depend on local RocketMQ retry settings. Record
observed values before making any recovery-time claim.

## Verified 2026-06-11

- Pausing the Broker left the new Outbox row in `FAILED` with `retry_count=1`;
  after unpause it reached `SENT` with fencing token incremented from 1 to 2.
- `TIMEOUT_ONCE` left EMAIL `FAILED` after the downstream insert. RocketMQ
  redelivery changed it to `SUCCESS` with `mq_redelivery_count=1`; the downstream
  idempotency row count remained 1.
- The transaction-message profile stored a real event ID and aggregate ID with
  `COMMITTED`, then completed all three notification channels.
- `POISON` failed initial delivery plus two redeliveries and was consumed from
  `%DLQ%shift-notification-consumer` into quarantine with reason `ROCKETMQ_DLQ`.
- The first Broker startup with a mounted store volume failed because the image
  could not initialize its store. The Compose file now avoids that permission
  trap and checks the actual Broker JVM process.
