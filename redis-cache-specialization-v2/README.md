# Redis Cache Specialization V2

Stage 1 specialization project for Redis cache correctness, concurrency,
degradation, benchmarks, Sentinel failover, and Redis Cluster experiments.

## Baseline and boundary

Source baseline: `redis-cache-demo/` at tag `demo-baseline-v1`.

The baseline remains unchanged as the L1 learning demo. This V2 project uses an
independent application, ports, Compose resources, tests, scripts, and evidence.
It uses a small desensitized staff schedule model only to exercise middleware
behavior; the model is not a hospital production design.

## Implemented behavior

- MySQL 8.4 is the fact source; Flyway creates and seeds a fixed schedule dataset.
- One query service selects one of five `CachePolicy` implementations:
  - `NONE`: MySQL only.
  - `CACHE_ASIDE`: physical TTL, jitter, and short-lived missing entries.
  - `LOCAL_MUTEX`: one JVM mutex with a cache double-check.
  - `REDISSON_LOCK`: cross-instance Redisson lock with wait and lease settings.
  - `LOGICAL_EXPIRE`: bounded stale reads and one background rebuild lock.
- A schedule update and `cache_invalidation_task` insert commit in one transaction.
- Cache deletion retries use bounded exponential backoff and end in `DEAD`.
- Redis access failures use bounded MySQL fallback; saturated fallback returns 503.
- Micrometer exposes cache result, database load, lock wait, invalidation, and
  degradation metrics.

## API

```text
GET /api/schedules/{id}?policy=CACHE_ASIDE
PUT /api/schedules/{id}
GET /api/status
GET /actuator/prometheus
```

Update body:

```json
{"department":"Emergency","shiftType":"NIGHT"}
```

Errors use `code`, `message`, `resource`, and `traceId` fields.

## Daily environment

```powershell
cd C:\Users\PC\Desktop\中间件学习\redis-cache-specialization-v2
docker compose up -d
mvn spring-boot:run
```

Ports are isolated from the baseline:

| Resource | Baseline | V2 |
| --- | --- | --- |
| Application | `8080` | `8090` |
| Redis | `6379` | `6380` |
| MySQL | not used | `3307` |

## Tests

```powershell
mvn test
```

The suite includes unit, concurrency, stable race reproduction, context, and
MySQL/Redis Testcontainers tests. The integration test is skipped only when a
Docker engine is unavailable; final evidence must use a real MySQL container.

## Benchmarks

Run one parameterized scenario:

```powershell
docker compose --profile benchmark run --rm `
  -e CACHE_POLICY=LOCAL_MUTEX `
  -e VUS=20 `
  -e DURATION=30s `
  redis-v2-k6
```

Run the five-policy matrix for three rounds:

```powershell
.\k6\run-matrix.ps1 -Rounds 3 -Vus 20 -Duration 30s
```

Separate warm-up from measured runs. Record hardware, container resources,
dataset, configuration, throughput, P50/P95/P99, errors, database loads, lock
wait, and cache metrics in `docs/benchmark-reports/`. The repository currently
contains the scripts and test plan, not publishable performance conclusions.

## Redis failure drill

```powershell
.\failure-drills\redis-stop-recover.ps1
```

The application uses short Redis timeouts and bounded MySQL fallback. A 503 with
`CACHE_FALLBACK_SATURATED` is an explicit overload response, not a successful
cache result.

## Sentinel experiment

```powershell
docker compose -f compose.sentinel.yaml up -d
docker compose -f compose.sentinel.yaml exec redis-sentinel-1 `
  redis-cli -p 26379 SENTINEL get-master-addr-by-name redis-v2-master
$masterId = docker compose -f compose.sentinel.yaml ps -q redis-sentinel-master
docker pause $masterId
```

Pausing retains the master's Docker DNS identity, unlike stopping and removing
the container. The verified local drill promoted a replica from
`redis-sentinel-master:6379` to `172.23.0.4:6379`; this topology result does not
prove a production SLA or application client error window.

## Cluster experiment

```powershell
docker compose -f compose.cluster.yaml up -d
docker compose -f compose.cluster.yaml exec redis-cluster-1 redis-cli -c cluster info
docker compose -f compose.cluster.yaml exec redis-cluster-1 redis-cli -c cluster slots
docker compose -f compose.cluster.yaml exec redis-cluster-1 `
  redis-cli -c MSET '{schedule}:1' a '{schedule}:2' b
```

Use the cluster network for redirect and multi-key experiments. Cross-slot
multi-key commands require keys with a shared hash tag. This local six-node
topology does not prove production cluster capacity.

## Redisson ownership and renewal

The default explicit lease is `3s`; work that exceeds it loses the lock. Set
`app.cache.lock-lease=0s` to use Redisson watchdog renewal instead. Unlock is
guarded by `isHeldByCurrentThread()`, and logical-expiration rebuild locks are
acquired and released inside the same asynchronous worker thread.

## Evidence

- `docs/architecture-decisions/0001-redis-v2-policy-boundaries.md`
- `docs/migration-contracts/redis-cache-policy-contract.md`
- `docs/benchmark-reports/redis-stage-1-test-plan.md`
- `docs/failure-drills/redis-stage-1-drills.md`

Until comparable measured reports and application-level Sentinel/Cluster client
drills are appended, do not claim Redis performance improvement, production high
availability, SLA, or capacity.
