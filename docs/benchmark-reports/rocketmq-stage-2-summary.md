# RocketMQ Stage 2 benchmark summary

Status: one comparable local smoke matrix completed on 2026-06-11.

Fixed endpoint: `POST /api/shift-changes`, fixed payload shape, Outbox profile,
MySQL 8.4, RocketMQ 5.3.2, and the same JVM/container configuration.

Record each round's request throughput, HTTP P50/P95/P99, error rate, Outbox send
latency, notification completion latency, backlog, retries, and host/container
resources. The short local run validates the test path and does not establish
production capacity.

## Local result

Environment: Windows Docker Desktop with 7.8 GB assigned memory, Java 17,
MySQL 8.4, RocketMQ 5.3.2 single Broker, Outbox profile, k6 1.8.0, 3 VUs for
5 seconds per round.

| Round | Requests | Requests/s | Median | P95 | Max | Errors |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 148 | 29.12 | 46.99 ms | 89.01 ms | 145.87 ms | 0% |
| 2 | 140 | 27.66 | 50.16 ms | 95.53 ms | 213.96 ms | 0% |
| 3 | 140 | 27.73 | 48.09 ms | 100.07 ms | 235.12 ms | 0% |

This is a short toolchain and stability check. It does not include controlled
CPU/memory limits or long backlog observation and is not a capacity baseline.
