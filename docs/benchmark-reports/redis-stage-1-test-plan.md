# Redis Stage 1 benchmark plan

Status: scripts implemented; a k6 smoke run is required before commit, while the
full comparable three-round matrix remains unmeasured.

## Toolchain smoke run 2026-06-11

- Policy: `CACHE_ASIDE`; fixed schedule ID `1`.
- Load: `5` VUs for `5s`, with the script's separate 20-request warm-up.
- Result: `328` HTTP requests, `0.00%` failed requests, median `19.22 ms`,
  P95 `102.88 ms`, maximum `266.55 ms`.
- Purpose: verify Docker k6 1.8.0 can reach the local application and emit the
  expected metrics. This short, single-policy run is not a benchmark and cannot
  support a performance comparison.

## Fixed inputs

- Java 17 and Spring Boot 3.5.7.
- MySQL 8.4, Redis 7.4, Redisson Community 4.5.0, k6 1.8.0.
- Flyway fixed schedule dataset.
- Same application build, JVM options, connection pools, and container limits for
  every compared run.

## Scenarios

- `NONE` database-only baseline.
- Cold and warm `CACHE_ASIDE` reads.
- Random missing IDs.
- Single hot key expiry under `LOCAL_MUTEX` and `REDISSON_LOCK`.
- Multiple hot keys expiring together.
- `LOGICAL_EXPIRE` stale read and single background rebuild.
- Redis stop and recovery with bounded database fallback.
- Two application instances competing for the same hot key.

Each measured scenario requires a separate warm-up and at least three rounds.
Record throughput, P50/P95/P99, error rate, database load count, cache result
counters, lock wait time, and host/container resources. Do not publish improvement
claims until comparable measured runs are appended to this report.
