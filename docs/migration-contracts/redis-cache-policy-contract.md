# Redis cache policy migration contract

Source project: `redis-cache-specialization-v2/`.

## Interface

`CachePolicy` exposes a policy type and a schedule lookup. Applications select a
policy explicitly; they do not duplicate repository or controller logic for each
cache strategy.

## Key and value contract

- Key: `middleware:v2:schedule:{scheduleId}`.
- Regular entry: schedule payload, cache timestamp, and no logical expiry.
- Missing entry: `missing=true`, null payload, short physical TTL.
- Logical entry: schedule payload, cache timestamp, and `logicalExpiresAt`.
- Physical TTL for a logical entry covers logical freshness plus the maximum
  permitted stale window.

## Invalidation contract

The database update and `cache_invalidation_task` insert commit together. Tasks
move through `PENDING -> PROCESSING -> SUCCESS`, or retry as `FAILED` and end in
`DEAD` after the configured limit.

## Operational boundary

Migrate the interface, key format, task table, metrics, tests, and runbook. Do not
migrate the teaching schedule records as hospital production data.
