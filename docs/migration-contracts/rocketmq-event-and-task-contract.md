# RocketMQ event and task migration contract

## Event envelope

Required fields are `eventId`, `eventType`, `aggregateType`, `aggregateId`,
`occurredAt`, `schemaVersion`, `traceId`, and `payload`.

- Producers create immutable event IDs and aggregate IDs before publication.
- Consumers route on `eventType + schemaVersion`.
- Unknown versions and malformed JSON enter quarantine and are acknowledged.
- Message key is `eventId`; trace context belongs in message properties when
  tracing is added.

## Persistent states

- Outbox: `NEW/PROCESSING/SENT/FAILED/DEAD`.
- Notification task: `PROCESSING/SUCCESS/FAILED/DEAD`.
- Every ownership-sensitive completion includes `worker_id + fencing_token`.
- Timed-out ownership can be taken over only after `processing_deadline`.
- Retry delay is exponential and bounded; DEAD recovery is an explicit operator
  action.

The portable assets are the envelope, SQL state models, conditional updates,
metrics, scripts, and runbook. The shift-change teaching model is not portable.
