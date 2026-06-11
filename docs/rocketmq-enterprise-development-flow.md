# RocketMQ 企业级学习开发流程

## 目标

基于医疗预约通知场景，把 RocketMQ 从“会调用 API”提升到“能解释业务边界和可靠性风险”：

1. 预约创建接口只负责保存业务数据和发布事件。
2. 通知发送由消费者异步处理。
3. 消费端用业务事件 ID 做幂等，能承受重复消息。
4. 消费失败时抛出异常，让 RocketMQ 重试，而不是吞掉错误。
5. 每一步都有代码、测试、运行命令和日志记录。

## 开发步骤

1. 新建 `rocketmq-notification-demo`，保持和 Redis Demo 同级，避免多个中间件混在一个练习里。
2. 增加 `compose.yaml` 和 `broker.conf`，按官方单节点 RocketMQ 拓扑启动 NameServer 与 Broker。
3. 定义预约业务模型：`Appointment`、`CreateAppointmentRequest`、`AppointmentRepository`。
4. 定义消息模型：`AppointmentCreatedEvent`，字段包含 `eventId`、预约 ID、患者 ID、医生 ID、预约时间和事件时间。
5. 抽象 `AppointmentEventPublisher`，让业务服务不直接依赖 RocketMQ SDK，方便单元测试。
6. 用 `RocketMQTemplate.syncSend` 实现生产者，destination 使用 `topic:tag`，消息 header 设置 `KEYS=eventId`。
7. 用 `@RocketMQMessageListener` 实现消费者入口，只做转发，核心逻辑放入 `NotificationService`。
8. 在 `NotificationService` 中先查 `eventId` 是否已处理，已处理则直接返回；未处理再执行发送通知并保存记录。
9. 用 `patientId=500` 作为稳定故障入口，第一次消费抛出异常，第二次消费成功，用于演示 RocketMQ 重试。
10. 写单元测试覆盖发布事件、重复消息幂等、失败后重试成功。
11. 更新 README、根 README 和实现日志，保存复现命令与已验证结论。

## 验收命令

自动化测试：

```powershell
cd C:\Users\PC\Desktop\中间件学习\rocketmq-notification-demo
mvn test
```

本地联调：

```powershell
cd C:\Users\PC\Desktop\中间件学习\rocketmq-notification-demo
docker compose up -d
mvn spring-boot:run
```

正常消息：

```powershell
$body = '{"patientId":10,"doctorId":20,"appointmentTime":"2026-06-04T09:00:00Z"}'
$appointment = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/appointments -ContentType application/json -Body $body
Start-Sleep -Seconds 3
Invoke-RestMethod http://localhost:8081/api/appointments/$($appointment.id)/notifications
```

重试消息：

```powershell
$body = '{"patientId":500,"doctorId":20,"appointmentTime":"2026-06-04T10:00:00Z"}'
$appointment = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/appointments -ContentType application/json -Body $body
Start-Sleep -Seconds 20
Invoke-RestMethod http://localhost:8081/api/appointments/$($appointment.id)/notifications
```

## 学习时必须能回答的问题

1. 为什么不能在创建预约的 HTTP 请求里直接发送通知？
2. RocketMQ Producer 同步发送成功代表什么，不代表什么？
3. 为什么 Consumer 必须幂等？
4. 这个 Demo 的幂等为什么只适合学习，不适合生产？
5. `topic`、`tag`、`KEYS` 分别解决什么问题？
6. 为什么消费失败时要抛异常，而不是记录日志后返回？
7. 重试可能造成什么副作用？
8. 如何把内存 `NotificationRecordStore` 改造成 MySQL 可靠记录？
9. 单 Broker Compose 为什么不能证明高可用？
10. 这段经历能写进简历的边界是什么？

## 可写入简历的边界

在完成 `mvn test` 和本地联调后，可以写：

> 基于 Spring Boot 3 和 RocketMQ 5 实现预约创建后的异步通知流程，使用 topic/tag 区分预约事件，通过 eventId 做消费幂等，并用故障注入验证失败重试后可恢复；保留 Docker Compose、单元测试和 HTTP 联调命令作为复现证据。

不能写：

- 百万级消息吞吐。
- 生产高可用 RocketMQ 集群。
- 精确一次消费。
- 线上调优经验。

## 后续中间件路线与权重

当前推荐顺序：

```text
MySQL 幂等落库 -> Elasticsearch 搜索 -> Nacos 拆服务 -> Sentinel 接口保护 -> XXL-JOB 补偿任务 -> Prometheus/Grafana 观测
```

这个顺序的核心原则是：先补业务可靠性，再补查询体验，然后再拆服务和加治理，最后做任务补偿与可观测性。不要为了简历关键词提前堆中间件；每一步都必须留下代码、测试、复现命令和实现日志。

| 顺序 | 中间件/能力 | 权重 | 推荐场景 | 抉择点 | 验收证据 |
| --- | --- | --- | --- | --- | --- |
| 1 | MySQL 幂等落库 | 30% | 把预约和通知记录从内存 Map 改为真实表 | RocketMQ 的幂等不能只靠内存；`eventId` 应落库并加唯一索引 | 表结构、唯一索引、重复消息测试、重启后数据不丢 |
| 2 | Elasticsearch 搜索 | 22% | 药品、医生、科室搜索 | 等核心数据先落 MySQL 后再做索引；不要让 ES 成为唯一数据源 | mapping、分页搜索、同步脚本/事件、索引重建说明 |
| 3 | Nacos 拆服务 | 16% | 预约服务、通知服务、搜索服务拆分 | 单体 Demo 足够清楚前不要拆；拆服务是为边界和治理服务 | 服务注册、配置中心、动态配置刷新、Compose 复现 |
| 4 | Sentinel 接口保护 | 14% | 预约创建、搜索、通知查询接口保护 | 有真实接口和调用链后再加限流；规则要能解释业务损失与降级结果 | 限流/熔断/降级规则、压测触发、返回码和日志 |
| 5 | XXL-JOB 补偿任务 | 10% | 扫描失败通知、补偿未同步索引、清理过期预约 | 先有可靠落库状态，再做补偿；不要用定时任务掩盖主流程错误 | 任务注册、失败重试、执行日志、幂等补偿测试 |
| 6 | Prometheus/Grafana 观测 | 8% | 看 QPS、延迟、错误率、消费失败次数 | 功能闭环稳定后再观测；指标必须服务排障和容量判断 | 指标端点、Grafana 面板、一次故障演练截图/日志 |

### 1. MySQL 幂等落库

优先级最高，Stage 1 已完成。RocketMQ Demo 已将预约表、通知记录表落到 MySQL，并给通知记录的 `event_id` 建唯一索引；重复消费的最终幂等边界已经从内存查询升级为数据库约束。

验收标准：

- `appointments` 表保存预约主数据。
- `notification_records` 表保存消费结果。
- `event_id` 唯一索引能拦截重复消费。
- 重复调用 `NotificationService.handle` 或重复投递同一事件时只写入一条记录。
- 重启应用后还能查询已创建预约和通知记录。
- 已记录 `mvn test`、Docker + HTTP 联调和应用重启验证输出。

### 2. Elasticsearch 搜索

MySQL 落库后再加 ES。ES 负责搜索体验，MySQL 仍是事实数据源。适合做医生、科室、药品的关键词搜索、分页和索引重建。

抉择点：

- 如果只是按 ID 查询，不需要 ES。
- 如果要分词、模糊搜索、排序、分页和搜索体验，再引入 ES。
- 必须写清楚 MySQL 到 ES 的同步策略，例如手动重建、RocketMQ 事件同步或定时补偿。

### 3. Nacos 拆服务

当预约、通知、搜索边界稳定后，再拆成多个 Spring Boot 服务并接入 Nacos。不要为了“微服务”提前拆，拆服务会增加配置、端口、启动顺序和排障成本。

抉择点：

- 单体 Demo 能说明业务时，先保持单体。
- 只有当服务边界清楚、调用关系可解释时，才引入注册发现。
- 配置中心要演示真实配置，例如通知开关、搜索分页默认值、Sentinel 规则地址。

### 4. Sentinel 接口保护

Sentinel 应用于已有接口保护，而不是孤立 Demo。优先保护预约创建和搜索接口，演示限流、熔断、降级和热点参数。

抉择点：

- 预约创建接口限流后返回明确错误，避免无限排队。
- 搜索接口可降级为热门医生/药品推荐或空结果提示。
- 规则必须能被压测触发，并记录 QPS、错误率和日志。

### 5. XXL-JOB 补偿任务

XXL-JOB 适合做补偿，不适合替代主链路。典型任务包括失败通知补偿、ES 索引补偿、过期预约清理。

抉择点：

- 任务必须幂等，否则失败重跑会造成重复通知或重复写入。
- 任务要有可查询执行日志。
- 先有 MySQL 状态字段，再写补偿任务；不要只扫内存。

### 6. Prometheus/Grafana 观测

最后补观测。观测不是装一个面板，而是回答“哪里慢、哪里错、消息有没有积压”。先采集接口 QPS、HTTP 延迟、错误率、RocketMQ 消费失败次数、任务执行结果。

抉择点：

- 功能闭环未稳定前，监控价值有限。
- 每个面板都要对应一个排障问题。
- 不做未经压测支撑的性能结论。

## 一个对话一个 Stage

后续中间件按“一个对话或后台线程只推进一个 Stage”的方式执行，避免多个中间件同时改动导致测试、联调和学习记录不可复现。

| Stage | 线程标题 | 状态 | 范围 |
| --- | --- | --- | --- |
| 1 | `middleware-stage-1-mysql-idempotency` | 已完成 | MySQL 幂等落库 |
| 2 | `middleware-stage-2-elasticsearch-search` | 已完成 | Elasticsearch 搜索 |
| 3 | `middleware-stage-3-nacos-services` | 已完成 | Nacos 服务注册与配置管理 |
| 4 | `middleware-stage-4-sentinel-protection` | 已完成 | Sentinel 接口保护 |
| 5 | `middleware-stage-5-xxljob-compensation` | 已完成 | XXL-JOB 补偿任务 |
| 6 | `middleware-stage-6-prometheus-grafana` | 已完成 | Prometheus/Grafana 观测 |

每个 Stage 的固定流程：

1. 输入：先阅读 `docs/resume-driven-roadmap.md`、本文件和 `docs/implementation-log.md`。
2. 实现：只做当前 Stage，不提前实现后续中间件。
3. 验收：运行自动化测试、Docker Compose 联调、至少一次故障或恢复演练。
4. 记录：更新 Demo README、`docs/implementation-log.md` 和本文件中的当前状态。
5. 交接：在文档中保留下一个 Stage 的可复制提示词。

## 下一步执行建议

> 历史说明：下方建议记录 `2026-06-09` 的有限额度收口方案。当前原 Demo
> 作为 L1 学习基线保留，后续 RocketMQ 可靠发布、消费状态机、死信恢复和
> 可观测性深化统一按 `docs/middleware-specialization-roadmap.md` 在独立 V2
> 目录推进。

RocketMQ 主线 Stage 1-6 已完成：MySQL 幂等落库、Elasticsearch 医生搜索、
Nacos 注册配置、Sentinel 接口保护、XXL-JOB ES 索引补偿、Prometheus/Grafana
本地观测均已有代码、测试、Docker/HTTP 验证和实现日志。

后续可复制提示词：

```text
继续在有限额度下收口中间件证据链。RocketMQ 主线 Stage 1-6 已完成：MySQL 幂等落库、Elasticsearch 搜索、Nacos 注册配置、Sentinel 接口保护、XXL-JOB ES 索引补偿、Prometheus/Grafana 本地观测均已有代码、测试、Docker/HTTP 验证和实现日志。下一步只做 Redis 缓存穿透、击穿、雪崩的最小可验证版本，不引入新大型中间件；要求补测试、README、实现日志和可复现命令，最后生成基于证据的简历表述与面试追问清单。不要写生产高可用、百万并发、未经压测的 P95/P99 或线上调优经验。
```
