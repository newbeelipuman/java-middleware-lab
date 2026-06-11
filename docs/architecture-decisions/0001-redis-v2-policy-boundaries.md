# ADR 0001: Redis V2 policy boundaries

## Context

The L1 baseline already demonstrates cache-aside, short-lived missing values, a
local mutex, and TTL jitter. It does not prove multi-instance coordination,
logical expiration, persistent invalidation retries, controlled degradation, or
performance differences.

## Decision

Use one MySQL-backed schedule query and select one of five `CachePolicy`
implementations per request: `NONE`, `CACHE_ASIDE`, `LOCAL_MUTEX`,
`REDISSON_LOCK`, or `LOGICAL_EXPIRE`.

Updates commit the schedule change and an invalidation task in the same MySQL
transaction. Cache deletion is attempted after commit and retried with bounded
exponential backoff. Redis failures use a bounded database fallback rather than
unlimited direct reads.

## Consequences

- The policy comparison uses the same repository and data model.
- Local mutex results apply only to one JVM.
- Redisson lock mode uses an explicit lease by default. Setting
  `app.cache.lock-lease=0s` selects Redisson watchdog renewal for the lock policy.
- Logical expiration may return stale data only until the configured stale limit.
- Database constraints and transactions remain the correctness boundary; Redis
  locks do not replace them.
