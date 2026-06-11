# ADR 0002: Outbox as the default publication path

## Decision

Use the transactional Outbox as the default migration pattern. Keep RocketMQ
transaction messages in a separate Spring profile for API and recovery
comparison, not as a second production path.

## Reasons

- Outbox recovery state is visible in MySQL and can be operated without relying
  on broker transaction-check timing.
- The application already requires persistent retry, DEAD recovery, ownership,
  deadlines, and fencing for other background work.
- Transaction messages remain valuable when the local transaction boundary is
  small and the team can operate broker checks. Their check callback must use
  database facts and return `UNKNOWN` when that fact cannot be read.

Neither option removes consumer idempotency. Both target eventual consistency,
not exact-once delivery.
