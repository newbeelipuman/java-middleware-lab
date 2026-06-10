# 中间件项目简历证据清单

## 使用原则

本文件只整理仓库中已经验证过的事实，用于后续改简历和准备面试。不要把这里的内容扩写成未经证明的生产经验、性能指标或高可用结论。

当前可证明的范围：

- Redis 缓存旁路、缓存穿透、缓存击穿、缓存雪崩最小处理。
- RocketMQ 预约异步通知、消费幂等、失败重试。
- MySQL 幂等落库和唯一索引。
- Elasticsearch 医生搜索和索引重建。
- Nacos 服务注册和配置刷新。
- Sentinel 接口限流。
- XXL-JOB ES 索引补偿任务。
- Prometheus/Grafana 本地指标观测。

## 可写项目表述

### 表述 1：预约异步通知主链路

可写：

> 基于 Spring Boot 3 和 RocketMQ 5 实现医疗预约创建后的异步通知流程，使用 topic/tag 区分预约事件，通过 eventId 和 MySQL 唯一索引保证消费幂等，并通过故障注入验证消费失败后可由 RocketMQ 重试恢复。

证据：

- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/AppointmentService.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/NotificationService.java`
- `rocketmq-notification-demo/src/main/resources/schema.sql`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/NotificationServiceTest.java`
- `docs/implementation-log.md` 中 RocketMQ 初始阶段和 Stage 1 记录。

可被追问：

- 为什么不在创建预约的 HTTP 请求里直接发送通知？
- RocketMQ producer 同步发送成功代表什么，不代表什么？
- 为什么 consumer 必须幂等？
- eventId 唯一索引解决什么问题？
- 消费失败时为什么要抛异常，而不是记录日志后返回成功？

必须讲清的边界：

- 当前是本地单 NameServer、单 Broker Docker Compose，不证明 RocketMQ 生产高可用。
- 幂等语义是“重复消费只写一条通知记录”，不是精确一次消费。
- 没有做消息堆积压测和吞吐量评估。

### 表述 2：搜索索引与补偿恢复

可写：

> 在预约通知 Demo 中扩展医生搜索能力，以 MySQL 作为事实数据源、Elasticsearch 作为可重建搜索索引；实现医生索引全量重建接口，并通过 XXL-JOB handler 和手工触发入口完成 ES 索引缺失后的补偿恢复演练。

证据：

- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/DoctorSearchService.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/ElasticsearchDoctorSearchIndex.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/DoctorIndexCompensationJob.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/DoctorIndexCompensationService.java`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/DoctorSearchServiceTest.java`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/DoctorIndexCompensationServiceTest.java`
- `docs/implementation-log.md` 中 Stage 2 和 Stage 5 记录。

可被追问：

- 为什么 MySQL 是事实数据源，ES 不是？
- 删除 ES 索引后为什么还能恢复？
- 全量重建和增量同步分别适合什么场景？
- XXL-JOB 在这里解决什么问题，不能替代什么？
- 补偿任务如何保证幂等？

必须讲清的边界：

- 当前只验证了本地 ES 单节点和全量重建，不证明 ES 集群高可用。
- XXL-JOB 只验证 handler 和本地手工触发入口，没有接入 Admin 调度和分片执行。
- 当前样例数据只有 4 条，没有验证大数据量分批、别名切换或零停机重建。

### 表述 3：服务治理与接口保护

可写：

> 基于 Nacos 原生 client 实现本地服务注册和配置刷新，用动态配置控制医生搜索默认分页大小；在预约创建和医生搜索接口接入 Sentinel 本地 QPS 规则，限流命中时返回 HTTP 429 和可解释错误响应。

证据：

- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/NacosRegistrationRunner.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/NacosConfigRunner.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/SentinelGuard.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/SentinelRuleInitializer.java`
- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/ApiExceptionHandler.java`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/NacosConfigRunnerTest.java`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/SentinelGuardTest.java`
- `docs/implementation-log.md` 中 Stage 3 和 Stage 4 记录。

可被追问：

- 为什么当前没有直接拆成多个 Maven 服务？
- Nacos 注册发现和配置中心分别解决什么问题？
- Sentinel 限流和业务降级有什么区别？
- 为什么搜索接口适合限流？
- 429 响应体里应该包含哪些信息？

必须讲清的边界：

- 当前仍是单体 Demo，只是在注册元数据和代码边界中表达 appointment、notification、search。
- Nacos 是 standalone，本地学习环境，不证明微服务生产治理。
- Sentinel 使用本地内存 QPS 规则，没有接入控制台规则下发、熔断或热点参数规则。

### 表述 4：本地可观测闭环

可写：

> 使用 Spring Boot Actuator 和 Micrometer Prometheus 暴露 HTTP 与业务指标，记录预约事件发布、通知消费、ES 补偿任务和 Sentinel 限流次数；通过 Prometheus 抓取本地指标并验证限流和补偿演练中的指标变化。

证据：

- `rocketmq-notification-demo/src/main/java/study/middleware/rocketmqnotification/BusinessMetrics.java`
- `rocketmq-notification-demo/src/test/java/study/middleware/rocketmqnotification/BusinessMetricsTest.java`
- `rocketmq-notification-demo/src/main/resources/application.yml`
- `rocketmq-notification-demo/prometheus.yml`
- `rocketmq-notification-demo/compose.yaml`
- `docs/implementation-log.md` 中 Stage 6 记录。

可被追问：

- 为什么监控指标要服务排障问题？
- HTTP 指标和业务指标分别看什么？
- `sentinel_blocks_total` 能帮助定位什么？
- 任务补偿为什么需要记录次数、结果和耗时？
- Prometheus pull 模式的本地 target 是怎么配置的？

必须讲清的边界：

- Grafana 只验证登录入口可访问，没有固化 dashboard JSON。
- 没有配置告警规则。
- 没有进行压测，因此不能写 P95/P99、吞吐量或容量结论。

### 表述 5：Redis 缓存风险处理

可写：

> 基于 Spring Boot Data Redis 实现商品查询缓存旁路模式，更新商品后主动删除缓存；进一步补充缓存穿透、击穿和雪崩的最小处理方案，包括短 TTL 空值缓存、热点 key 本地互斥回源和 TTL 随机抖动，并通过单元测试和本地 Redis 联调验证。

证据：

- `redis-cache-demo/src/main/java/study/middleware/rediscache/ProductService.java`
- `redis-cache-demo/src/main/java/study/middleware/rediscache/RedisProductCache.java`
- `redis-cache-demo/src/test/java/study/middleware/rediscache/ProductServiceTest.java`
- `redis-cache-demo/src/main/resources/application.yml`
- `redis-cache-demo/README.md`
- `docs/implementation-log.md` 中 Redis 缓存旁路和 Redis 风险补强记录。

可被追问：

- 什么是缓存旁路模式？
- 为什么更新商品后删除缓存，而不是更新缓存？
- 缓存穿透、击穿、雪崩分别是什么？
- 空值缓存的 TTL 为什么要短？
- 本地互斥锁为什么只能保护单实例？
- TTL 抖动能解决什么，不能解决什么？

必须讲清的边界：

- 当前 repository 是内存 Map，不是 MySQL。
- 本地互斥锁不适合多实例部署，多实例需要分布式锁、逻辑过期或异步刷新。
- 没有做压测，不能写缓存优化后的吞吐量或 P95/P99。

## 一分钟口述稿

### RocketMQ 项目口述

这个项目是基于医疗预约创建后的异步通知场景做的中间件练习。主链路里，预约接口只保存业务数据并发布 RocketMQ 事件，通知发送放到 consumer 异步处理。为了处理重复消费，我把通知记录落到 MySQL，并用 eventId 做唯一索引，单元测试覆盖了重复消息只写一条记录；为了验证失败重试，我用指定 patientId 做故障注入，第一次消费抛异常，第二次重试成功。后续在这个业务上继续加了医生搜索、Nacos 配置、Sentinel 限流、XXL-JOB 补偿和 Prometheus 指标，但我会明确说明这些都是本地 Docker Compose 学习环境，不代表生产高可用或性能压测结论。

### Redis 项目口述

Redis 这个 Demo 是商品查询缓存旁路模式。查询时先查 Redis，未命中再查模拟数据库并回填缓存；更新商品后删除缓存，避免读到旧值。后面补了三个典型风险：不存在商品会缓存短 TTL 空值，减少缓存穿透；热点 key 未命中时用本地锁让同一个 JVM 内只回源一次，降低击穿风险；正常缓存 TTL 加随机抖动，减少同一时间大批 key 过期。这里我也会说明边界：当前只是单实例 Demo，本地锁不能覆盖多实例，且没有压测数据。

## 禁写清单

不要写：

- 百万级并发、百万级消息吞吐。
- 生产高可用 RocketMQ、Redis、MySQL、ES、Nacos 或 Prometheus 集群。
- 精确一次消费。
- 线上调优经验。
- Kubernetes 弹性扩缩容。
- P95/P99、平均响应时间、吞吐量提升百分比等未经压测的数据。
- Grafana 生产监控大盘或告警治理经验。

可以写但要保守：

- 本地 Docker Compose 联调。
- 单元测试和 HTTP 演练。
- 故障注入和恢复验证。
- 本地 Prometheus 指标抓取。
- 对生产边界的理解。

## 面试前自测问题

1. 预约创建为什么要和通知发送解耦？
2. RocketMQ 发送成功、消费成功、业务最终成功之间有什么区别？
3. eventId 幂等为什么最终要落到数据库唯一索引？
4. MySQL 和 Elasticsearch 的职责边界是什么？
5. ES 索引删除后如何从 MySQL 恢复？
6. Nacos 配置刷新在项目里影响了哪个业务参数？
7. Sentinel 限流命中后为什么返回 429？
8. XXL-JOB 补偿任务为什么不能替代主链路？
9. Prometheus 指标里哪些是 HTTP 指标，哪些是业务指标？
10. 缓存穿透、击穿、雪崩分别用什么手段缓解？
11. 空值缓存有什么副作用？
12. 本地互斥锁为什么不适合多实例？
13. 哪些内容你现在不能写进简历？为什么？

