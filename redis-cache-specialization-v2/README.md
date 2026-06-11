# Redis Cache Specialization V2

This project is the Stage 1 specialization workspace for Redis cache, concurrency,
benchmark, and recovery experiments.

## Baseline

Source baseline: `redis-cache-demo/` at tag `demo-baseline-v1`.

The baseline remains a reproducible L1 learning demo. This V2 project is separate
so ports, Compose resources, tests, benchmark scripts, and evidence cannot be
confused with the completed baseline.

## Current Scope

This initial commit only creates the independent Spring Boot skeleton and public
cache policy contract. It does not yet implement Redis pressure testing,
Redisson locking, logical expiration, Sentinel failover, Redis Cluster, or k6
reports.

Planned `CachePolicy` values:

- `NONE`
- `CACHE_ASIDE`
- `LOCAL_MUTEX`
- `REDISSON_LOCK`
- `LOGICAL_EXPIRE`

## Local Commands

```powershell
cd C:\Users\PC\Desktop\中间件学习\redis-cache-specialization-v2
mvn test
```

Optional local dependencies for later Stage 1 work:

```powershell
docker compose up -d
```

Ports are intentionally different from the baseline:

| Resource | Baseline | V2 |
| --- | --- | --- |
| App | `8080` | `8090` |
| Redis | `6379` | `6380` |
| MySQL | not used | `3307` |

## Difference From Baseline

- Baseline uses an in-memory repository and focuses on cache-aside mechanics.
- V2 will use MySQL and Flyway for a fixed desensitized schedule dataset.
- Baseline has no reproducible pressure test reports.
- V2 will add k6 scripts, benchmark summaries, failure drills, and migration
  contracts before any capability is treated as L2/L3 evidence.
