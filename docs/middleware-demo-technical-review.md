# 中间件 Demo 技术复盘

## 复盘原则

这份复盘只回答三个问题：

1. Redis、RocketMQ、Elasticsearch、XXL-JOB、Prometheus 等分别验证到什么程度。
2. 每个中间件沉淀出来的可迁移能力是什么。
3. 哪些说法有证据，哪些说法当前不能写。

当前不继续扩张技术栈，不新增中间件，也不把本地 Demo 包装成生产经验。所有结论都以代码、测试、Docker/HTTP 联调和 `docs/implementation-log.md` 为准。

## 总览

| 中间件 | 当前验证程度 | 可迁移能力 | 证据强度 | 不能扩写成 |
| --- | --- | --- | --- | --- |
| Redis | 已验证缓存旁路、TTL、更新失效、空值缓存、本地互斥回源、TTL 抖动 | 缓存一致性边界、穿透/击穿/雪崩风险识别、缓存策略取舍 | 单元测试 + Redis 容器 HTTP 联调 | Redis 集群、高可用、压测优化结论 |
| RocketMQ | 已验证预约事件发布、异步消费、eventId 幂等、失败重试 | 异步解耦、消息可靠性边界、重复消费处理 | 单元测试 + RocketMQ 容器 HTTP 联调 + 故障注入 | 百万级吞吐、精确一次消费、生产集群高可用 |
| MySQL | 已验证预约和通知记录落库、`event_id` 唯一索引幂等、重启后数据可查 | 用数据库约束兜底业务幂等、把内存状态迁移为可恢复状态 | JDBC 测试 + MySQL 容器 + 重启验证 | MySQL 高可用、复杂事务调优、分库分表 |
| Elasticsearch | 已验证医生索引、关键词搜索、分页、删除索引后从 MySQL 重建 | 搜索索引和事实数据源分离、索引可重建设计 | 单元测试 + ES 容器 HTTP 联调 + 删除恢复演练 | ES 集群、高并发搜索、中文分词调优 |
| Nacos | 已验证本地服务注册、配置读取和动态刷新搜索默认分页大小 | 注册发现与配置中心的职责边界、动态配置落地方式 | 单元测试 + Nacos 容器 HTTP 查询 | 完整微服务拆分、生产治理、集群配置中心 |
| Sentinel | 已验证预约创建和医生搜索接口 QPS 限流，命中后返回 HTTP 429 | 接口保护、限流资源建模、明确降级响应 | 单元测试 + 快速请求触发 429 | 熔断治理、热点参数、集群流控、容量结论 |
| XXL-JOB | 已验证 ES 医生索引补偿 handler 和本地手工触发入口 | 补偿任务设计、任务幂等、主链路与补偿链路分工 | 单元测试 + 删除 ES 索引后补偿恢复 | Admin 调度、分片广播、执行器集群、线上任务治理 |
| Prometheus/Grafana | 已验证 Actuator/Micrometer 指标暴露、Prometheus 抓取、Grafana 登录入口 | 指标建模、用业务指标观察故障/限流/补偿结果 | 单元测试 + Prometheus target up + 指标变化记录 | 生产监控体系、告警治理、P95/P99、容量规划 |

## Redis

已验证到：

- 商品查询缓存旁路：先查 Redis，未命中再查模拟 repository，并回填缓存。
- 正常商品 TTL：基础 5 分钟，并增加 0-30 秒随机抖动。
- 更新失效：更新商品后删除对应缓存。
- 缓存穿透：不存在商品写入短 TTL 空值 `__MISSING__`。
- 缓存击穿：同一 JVM 内按商品 ID 做本地互斥回源，锁内二次检查缓存。
- 缓存雪崩：正常缓存 TTL 增加随机抖动，降低同一时间集中失效概率。

可迁移能力：

- 能解释缓存旁路为什么常用，以及它和数据库一致性的边界。
- 能把缓存风险拆成穿透、击穿、雪崩三类，并给出最小治理方案。
- 能说明本地锁、空值缓存、TTL 抖动分别解决什么问题，以及副作用是什么。

证据：

- `redis-cache-demo/src/main/java/study/middleware/rediscache/ProductService.java`
- `redis-cache-demo/src/main/java/study/middleware/rediscache/RedisProductCache.java`
- `redis-cache-demo/src/test/java/study/middleware/rediscache/ProductServiceTest.java`
- `redis-cache-demo/README.md`
- `docs/implementation-log.md` 中 Redis 两段记录。

边界：

- repository 仍是内存 Map，不是 MySQL。
- 本地互斥锁只保护单 JVM，多实例需要 Redis 分布式锁、逻辑过期或异步刷新。
- 没有压测，不能写吞吐量、P95/P99 或响应时间提升。

## RocketMQ

已验证到：

- 创建预约后发布 `AppointmentCreatedEvent`。
- Producer 使用 topic/tag 和 eventId key。
- Consumer 通过 `eventId` 做幂等处理。
- `patientId=500` 作为故障注入入口，第一次消费失败，后续由 RocketMQ 重试恢复。
- Stage 1 后通知记录落入 MySQL，`notification_records.event_id` 唯一索引成为幂等兜底。

可迁移能力：

- 能把 HTTP 主流程和通知发送解耦，避免把慢操作放在同步请求里。
- 能解释 producer 发送成功、broker 接收、consumer 成功、业务最终成功之间的区别。
- 能解释为什么消息消费必须按“至少一次”语义设计幂等。
- 能把幂等从内存判断升级为数据库唯一约束。

证据：

- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/AppointmentService.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/RocketMqAppointmentEventPublisher.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/NotificationConsumer.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/NotificationService.java`
- `rocketmq-notification-demo/src/main/resources/schema.sql`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/NotificationServiceTest.java`
- `docs/implementation-log.md` 中 RocketMQ 初始阶段和 Stage 1。

边界：

- 本地 Compose 是单 NameServer、单 Broker，不证明生产高可用。
- 当前证明的是“重复消费只写一条通知记录”，不是精确一次消费。
- 没有做消息堆积、消费延迟、吞吐量压测。

## Elasticsearch

已验证到：

- 建立医生搜索索引。
- 从 MySQL 医生主数据全量重建 ES 索引。
- 支持关键词搜索和分页。
- 删除 `doctors` 索引后，可以通过重建接口恢复搜索能力。

可迁移能力：

- 能区分事实数据源和搜索索引：MySQL 负责真实数据，ES 负责查询体验。
- 能解释索引为什么必须可重建。
- 能说清全量重建、增量同步、定时补偿适合的边界。

证据：

- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/DoctorSearchService.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/ElasticsearchDoctorSearchIndex.java`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/DoctorSearchServiceTest.java`
- `docs/implementation-log.md` 中 Stage 2。

边界：

- ES 是本地单节点。
- 当前是英文样例数据和默认分词，没有中文分词器调优。
- 样例数据只有 4 条，没有验证深分页、大数据量 bulk、索引别名切换。

## Nacos

已验证到：

- 应用启动后注册 `appointment-notification-service`。
- 通过 Nacos 配置 `app.search.default-page-size` 控制搜索默认分页大小。
- 修改配置后应用无需重启即可刷新配置。
- `/api/nacos/status` 能展示注册和配置状态。

可迁移能力：

- 能解释服务注册和配置中心分别解决什么问题。
- 能把动态配置落到真实业务参数，而不是只读一个配置样例。
- 能说明为什么当前先保留单体边界，而不是为了“微服务”过早拆模块。

证据：

- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/NacosRegistrationRunner.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/NacosConfigRunner.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/SearchSettings.java`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/NacosConfigRunnerTest.java`
- `docs/implementation-log.md` 中 Stage 3。

边界：

- 当前使用 Nacos 原生 client，不是完整 Spring Cloud Alibaba 微服务体系。
- 应用仍是单体，只在代码和注册元数据中表达 appointment、notification、search 边界。
- Nacos 是 standalone 容器，不证明生产集群。

## Sentinel

已验证到：

- `POST /api/appointments` 使用 `appointment.create` 资源限流，配置 2 QPS。
- `GET /api/search/doctors` 使用 `doctor.search` 资源限流，配置 3 QPS。
- 超限后返回 HTTP 429 和结构化错误响应。
- 快速请求演练中医生搜索接口出现 200、200、200、429、429。

可迁移能力：

- 能从真实业务入口抽象 Sentinel resource。
- 能解释限流和降级的区别：当前是拒绝保护，不是返回业务兜底结果。
- 能把接口保护结果通过 HTTP 状态码和响应体表达清楚。

证据：

- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/SentinelGuard.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/SentinelRuleInitializer.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/ApiExceptionHandler.java`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/SentinelGuardTest.java`
- `docs/implementation-log.md` 中 Stage 4。

边界：

- 当前只有本地内存 QPS 规则。
- 没有验证慢调用熔断、异常比例熔断、热点参数或集群流控。
- 快速请求只能证明规则触发，不能推出容量上限。

## XXL-JOB

已验证到：

- 实现 `doctorIndexCompensationJob`。
- 补偿任务调用 `DoctorSearchService.rebuildIndex()`，从 MySQL 重建 ES 医生索引。
- 提供本地手工入口 `POST /api/jobs/doctor-index-compensation/run`。
- 删除 ES 索引后触发补偿，搜索恢复。

可迁移能力：

- 能区分主链路和补偿链路：任务只能兜底恢复，不能替代主流程正确性。
- 能围绕已有状态设计补偿任务，而不是扫内存或凭空补偿。
- 能解释任务幂等为什么重要：同一补偿多次执行不应破坏数据。

证据：

- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/DoctorIndexCompensationJob.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/DoctorIndexCompensationService.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/CompensationJobController.java`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/DoctorIndexCompensationServiceTest.java`
- `docs/implementation-log.md` 中 Stage 5。

边界：

- 默认 `app.xxl-job.enabled=false`，本地学习时没有连接 XXL-JOB Admin。
- 没有验证 Admin 调度、失败重试、分片广播或执行器集群。
- 当前全量重建适合样例数据，不代表大数据量补偿方案。

## Prometheus/Grafana

已验证到：

- Spring Boot Actuator 暴露 `/actuator/prometheus`。
- Micrometer 记录业务指标：
  - `appointment_events_published_total`
  - `notification_consumption_total`
  - `compensation_runs_total`
  - `compensation_duration_seconds_count`
  - `sentinel_blocks_total`
- Prometheus 通过 `host.docker.internal:8081/actuator/prometheus` 抓取指标，target 为 up。
- 演练预约通知、ES 补偿、Sentinel 限流后，指标出现对应变化。
- Grafana 登录页可访问。

可迁移能力：

- 能把“业务是否正常”拆成可观测指标，而不只看日志。
- 能用指标回答具体排障问题：事件有没有发布、通知有没有消费、补偿有没有执行、限流有没有命中。
- 能区分 HTTP 指标、JVM 指标和业务指标。

证据：

- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/BusinessMetrics.java`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/BusinessMetricsTest.java`
- `rocketmq-notification-demo/src/main/resources/application.yml`
- `rocketmq-notification-demo/prometheus.yml`
- `rocketmq-notification-demo/compose.yaml`
- `docs/implementation-log.md` 中 Stage 6。

边界：

- Grafana 只验证登录入口，没有固化 dashboard JSON。
- 没有告警规则、容量规划或生产监控流程。
- 没有压测，不能写 P95/P99、吞吐量、平均响应时间优化。

## 面试时的统一表达

可以这样概括：

> 这组 Demo 不是为了堆中间件，而是围绕医疗预约通知和商品缓存两个场景，把每个中间件落到一个可验证问题上。Redis 负责缓存策略和风险处理；RocketMQ 负责预约后的异步通知；MySQL 把消费幂等从内存升级为唯一索引；Elasticsearch 负责医生搜索，但索引可以从 MySQL 重建；Nacos 做注册和动态配置；Sentinel 保护真实接口；XXL-JOB 做 ES 索引补偿；Prometheus 记录业务指标并验证故障演练中的变化。所有结论都基于本地 Docker Compose、单元测试和 HTTP 演练，不包装成生产高可用或压测结论。

## 禁写清单

不要写：

- 百万级并发、百万级消息吞吐。
- 生产高可用 RocketMQ、Redis、MySQL、ES、Nacos 或 Prometheus 集群。
- 精确一次消费。
- 线上调优经验。
- Kubernetes 弹性扩缩容。
- P95/P99、平均响应时间、吞吐量提升百分比等未经压测的数据。
- Grafana 生产监控大盘或告警治理经验。

可以保守写：

- 本地 Docker Compose 联调。
- 单元测试和 HTTP 演练。
- 故障注入和恢复验证。
- 本地 Prometheus 指标抓取。
- 对生产边界和后续演进方向的理解。
