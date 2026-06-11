# Redis Stage 1 failure drills

## Implemented and automated

- Stable unit reproduction of an old database read being written to cache after an
  updater commits and deletes the cache.
- Redis access failure falling back to MySQL through a bounded semaphore.
- Persistent invalidation task success path.
- MySQL 8.4 and Redis 7.4 Testcontainers integration for read, update,
  invalidation, and reload.

## Reproducible manual drills

- `failure-drills/redis-stop-recover.ps1`: stop Redis, exercise degraded reads,
  restart Redis, and verify recovery.
- `compose.sentinel.yaml`: three Sentinels, one master, and two replicas for
  failover observation.
- `compose.cluster.yaml`: three masters and three replicas for slot, redirect, and
  multi-key experiments.

## Verified 2026-06-11

### Redis stop and recovery

- A `CACHE_ASIDE` read succeeded through bounded MySQL fallback in `126 ms` while
  Redis was stopped.
- An update committed in MySQL while Redis was unavailable; its invalidation task
  moved to `FAILED` with `retry_count=1`.
- After Redis restarted, the scheduled retry moved the task to `SUCCESS`, and the
  next read returned the updated version.
- Metrics recorded one `database_fallback`, one failed invalidation attempt, and
  the eventual invalidation success.

### Sentinel topology

- Environment: Redis 7.4, one master, two replicas, and three Sentinels.
- Fault injection used `docker pause` so the master hostname remained resolvable.
- `SENTINEL get-master-addr-by-name` changed from
  `redis-sentinel-master:6379` to `172.23.0.4:6379` during the drill.
- This verifies local Sentinel detection and promotion only. The application was
  not connected through a Sentinel profile, so no client error window or SLA is
  claimed.

### Cluster topology

- The six-node topology reported `cluster_state:ok`, all `16384` slots healthy,
  six known nodes, and three masters.
- `MSET key1 a key2 b` returned `CROSSSLOT`, confirming the multi-key constraint.
- `MSET {schedule}:1 a {schedule}:2 b` followed by `MGET` returned `OK`, `a`, and
  `b`, confirming that shared hash tags route related keys to one slot.
- Application-level Cluster redirects remain a later client-profile drill.

## Remaining evidence gaps

- Comparable k6 benchmark rounds and resource snapshots are not yet recorded.
- Two live application instances have not yet competed for the same Redisson lock.
- Sentinel and Cluster behavior has not yet been measured through the Spring
  application clients.
