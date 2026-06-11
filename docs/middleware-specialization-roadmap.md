# Java 中间件专项完善路线

## 目标

本路线用于把当前“最小可运行 Demo”提升为可测试、可压测、可恢复、可排障、
可迁移的专项实验平台。工作量不作为删减能力的理由，但每项能力必须有清晰边界，
不能用本地实验冒充生产经验。

专项建设同时服务两个目标：

1. 补齐 `中间件理解情况.txt` 中记录的知识和实现缺口。
2. 为后续医院运营协同项目输出经过验证的设计、代码模式、测试脚本和观测规范。

## 完成级别

每项中间件统一按四级记录，不再用“接入过”代替完成程度：

| 级别 | 定义 | 证据 |
| --- | --- | --- |
| L0 概念 | 能解释用途和基本术语 | 学习记录、问答 |
| L1 功能 | 最小业务链路可运行 | 代码、单元测试、README |
| L2 专项 | 并发、失败、恢复和性能可验证 | 集成测试、压测、故障演练 |
| L3 迁移 | 输出稳定接口、数据模型和运行规范 | 迁移清单、模板、架构决策记录 |

当前 Demo 大多处于 L1，部分能力具备初步 L2 证据。本轮目标是让 Redis、
RocketMQ、Elasticsearch、XXL-JOB 和可观测性达到 L2，并形成 L3 迁移产物；
Nacos、Sentinel 补齐当前未覆盖的治理能力后达到 L2。

## 工程组织

保留现有目录作为不可覆盖的学习基线：

```text
redis-cache-demo/
rocketmq-notification-demo/
```

专项代码放入独立目录：

```text
redis-cache-specialization-v2/
rocketmq-reliability-specialization-v2/
elasticsearch-specialization-v2/
service-governance-specialization-v2/
scheduler-specialization-v2/
observability-specialization-v2/
```

每个目录必须能够独立构建和运行。公共约定通过文档、事件格式和测试规范共享，
不建立会迫使所有实验同时启动的公共运行时模块。

专项业务统一使用脱敏的医院运营小场景，例如人员班表、换班事件、通知任务、
制度索引和审计记录。只保留验证中间件所需字段，不在本仓库开发完整医院业务。

## Stage 0：仓库与证据治理

### 实现

- 确立本文为后续专项的唯一总路线，旧路线保留历史记录但不再决定后续阶段。
- 为当前基线创建 Git 标签，后续每个 Stage 使用独立分支和 Pull Request。
- 私人学习记录、简历、求职评估和大项目交接材料保持本地，不进入公开仓库。
- 新建公共证据目录：

```text
docs/benchmark-reports/
docs/failure-drills/
docs/architecture-decisions/
docs/migration-contracts/
```

- 每份报告记录提交号、机器环境、容器资源、配置、数据规模、执行命令和结果。

### 完成标准

- GitHub 中不存在真实密钥、个人材料、本地数据库和运行日志。
- 根 README 能区分原始 Demo、专项 V2、已完成能力和计划能力。
- 任何公开结论都能定位到代码、测试或实验记录。

## Stage 1：Redis 缓存、并发与高可用

### 业务和数据

- MySQL 保存人员班表查询数据，Redis 保存查询结果。
- 使用 Flyway 管理表结构和固定数据集。
- 提供同一 Service 的策略切换，不用两套业务代码制造不公平对比：
  - `NONE`：只查 MySQL。
  - `CACHE_ASIDE`：缓存旁路。
  - `LOCAL_MUTEX`：单 JVM 热点互斥。
  - `REDISSON_LOCK`：跨实例分布式锁。
  - `LOGICAL_EXPIRE`：逻辑过期和后台重建。

### 必须实现

- 缓存旁路、TTL、随机抖动、空值缓存、更新后删除缓存。
- 稳定复现“查询旧值重新写回缓存”的并发时序，并记录一致性边界。
- 缓存删除失败使用持久化失效任务重试，避免只记录日志后丢失。
- Redisson 可重入锁、等待时间、租约或看门狗模式实验。
- 逻辑过期只允许返回有限时间旧值，并保证同一 Key 只有一个重建任务。
- Redis 不可用时采用短超时、受控回源、限流和明确降级响应。
- 记录缓存命中、空值命中、回源、重建、锁等待、失败和降级指标。

### 压测

- 使用 Docker 化 k6，脚本提交到仓库。
- 固定数据规模、MySQL 连接池、JVM 参数、Redis 配置和容器资源。
- 场景覆盖：
  - 冷缓存。
  - 预热后的高命中查询。
  - 随机不存在 ID。
  - 单热点 Key 失效。
  - 多热点同时失效。
  - Redis 停止和恢复。
  - 两个应用实例竞争同一热点 Key。
- 每个场景独立预热，正式测试至少运行三轮。
- 记录吞吐量、P50/P95/P99、错误率、数据库查询数、锁等待和资源使用。

### 高可用实验

- 提供独立 Compose Profile 验证主从复制与 Sentinel 自动故障转移。
- 提供 Redis Cluster 实验，验证槽位、重定向和多 Key 限制。
- 记录故障切换期间的错误窗口和客户端恢复行为。

### 边界

- 不声称生产容量、高可用 SLA 或跨机房能力。
- 逻辑过期只用于允许短暂旧值的查询，不用于余额、库存扣减等强一致数据。
- 分布式锁不作为数据库事务和唯一约束的替代品。

### 迁移产物

- `CachePolicy` 接口与策略选择说明。
- Key 命名、TTL、空值和逻辑过期数据格式。
- 缓存失效任务表和重试规则。
- k6 脚本、指标命名和 Redis 故障演练脚本。

## Stage 2：RocketMQ 可靠发布与可靠消费

### 统一事件格式

所有事件使用统一信封：

```text
eventId
eventType
aggregateType
aggregateId
occurredAt
schemaVersion
traceId
payload
```

消费者必须按 `eventType + schemaVersion` 解析，未知版本进入隔离记录，不能静默丢弃。

### Stage 2A：本地消息表

- 业务数据与 `outbox_event` 在同一 MySQL 事务中提交。
- 状态使用 `NEW/PROCESSING/SENT/FAILED/DEAD`。
- 保存 `worker_id`、`processing_deadline`、`retry_count`、`next_retry_at` 和错误摘要。
- 使用条件更新或 `FOR UPDATE SKIP LOCKED` 批量抢占任务。
- 发布成功后按 Worker 所有权更新状态，旧 Worker 不得覆盖新结果。
- 使用指数退避和最大重试次数；DEAD 可通过管理接口重新激活。

### Stage 2B：RocketMQ 事务消息

- 另建独立 Profile 实现半消息、本地事务和 Broker 事务回查。
- 回查只根据数据库事实判断 `COMMIT/ROLLBACK/UNKNOWN`。
- 故障演练覆盖本地事务前后退出、回查期间数据库不可用和重复回查。
- 使用相同业务场景和验收测试，对比事务消息与本地消息表的复杂度和恢复方式。

### Stage 2C：消费任务状态机

- 通知任务幂等键使用 `event_id + channel`。
- 状态使用 `PROCESSING/SUCCESS/FAILED/DEAD`。
- 保存 `worker_id`、`processing_deadline`、业务重试次数、RocketMQ 重投次数和错误分类。
- 使用条件 SQL 完成首次抢占、超时接管、成功确认和失败更新。
- 模拟 `SMS/EMAIL/IN_APP` 三种渠道，验证同一事件不同渠道互不阻塞。
- 模拟下游服务保存相同幂等键，处理“下游已成功但本地确认失败”的不确定结果。
- 增加 RocketMQ 死信查询、消费、人工重放和重复重放防护。

### 性能和故障实验

- 记录生产吞吐、发送延迟、消费延迟、积压量、成功率和重试次数。
- 覆盖 Broker 暂停、Producer 超时、Consumer 重启、重复投递、积压恢复和 Poison Message。
- 测试两个 Consumer 并发抢占同一业务任务。
- MySQL 并发语义使用 Testcontainers 或真实 MySQL 测试，不用 H2 代替最终验证。

### 边界

- 不宣称精确一次消费；目标是至少一次投递下的业务幂等。
- 单机或单 Broker 实验不宣称生产高可用。
- Outbox 和事务消息都实现用于学习与比较，大项目根据业务边界只选择一种主方案。

### 迁移产物

- 事件信封和版本兼容规范。
- Outbox 与通知任务迁移脚本。
- 抢占、接管和 Fencing 条件 SQL。
- 重试、DEAD、人工恢复和下游幂等规范。

## Stage 3：Elasticsearch 索引生命周期与查询

### 必须实现

- MySQL 是事实数据源，ES 只保存可重建的搜索副本。
- 明确 Mapping、分析器、字段类型和 `dynamic` 策略。
- 支持中文名称、科室、制度和审计摘要检索。
- 实现普通分页和 `PIT + search_after` 深分页。
- 稳定排序最后使用唯一字段，客户端保存最新 PIT ID。
- 演示 refresh 前搜索不可见、按 ID 可读以及手工 refresh 的差异。
- 实现事件驱动增量同步、失败重试和同步状态查询。
- 全量重建使用版本化索引、Bulk、校验和 Alias 原子切换。
- 旧索引延迟删除，切换失败时保留回滚能力。
- 记录索引延迟、同步失败、Bulk 耗时、查询耗时和删除文档比例。

### 集群与故障

- 提供单节点日常 Profile 和多节点故障实验 Profile。
- 演练 Yellow/Red、节点停止、分片未分配、磁盘水位和写入拒绝。
- 使用 `_cluster/health`、`_cat/shards`、线程池、慢日志和 Profile API 定位问题。
- 记录 refresh、flush、translog、segment merge 的可观察现象。

### 边界

- 不实现医疗诊断检索。
- 不把 ES 当事务数据库或唯一数据源。
- 不根据小样本实验声称大规模集群调优经验。

### 迁移产物

- Mapping 与索引版本规范。
- 增量同步事件、重试表和重建状态模型。
- Alias 切换与回滚脚本。
- 深分页接口约定和故障排查清单。

## Stage 4：Nacos 与 Sentinel 服务治理

### Nacos

- 建立至少两个可独立部署的服务和一个调用方，真实验证注册发现。
- 配置中心管理超时、开关、分页和限流参数。
- 配置内容必须校验；非法配置保持最后一次有效值。
- 演练 Nacos 不可用、服务上下线、配置回滚和客户端重连。
- 提供 standalone 日常 Profile；集群只作为故障实验，不宣称生产治理。

### Sentinel

- 实现 QPS 限流、热点参数、慢调用比例、异常比例和异常数规则。
- 为查询接口提供可解释降级，为写接口提供明确拒绝和重试建议。
- 规则由 Nacos 持久化，应用重启后仍能恢复。
- 使用 k6 稳定触发每类规则，记录返回码、业务损失、指标和恢复时间。
- 接入 Sentinel Dashboard，展示资源与规则。

### 边界

- 没有真实服务边界时不为了数量继续拆服务。
- 限流阈值必须来自压测或业务约束，不使用无法解释的数字。
- 降级结果不能伪装成正常业务成功。

### 迁移产物

- 服务注册元数据规范。
- 动态配置校验和回退模板。
- Sentinel 资源命名、规则来源和错误响应规范。

## Stage 5：XXL-JOB 调度、分片与恢复

### 必须实现

- Compose 启动真实 XXL-JOB Admin 和独立数据库。
- 两个执行器实例自动注册，Admin 能查看在线状态。
- 实现定时任务、批量任务、失败补偿和人工触发。
- 批量任务按主键游标分页，不使用大 Offset，也不一次加载全表。
- 分片广播按 `index/total` 分配互斥数据范围。
- 每批保存进度、执行次数、处理数量和错误摘要。
- 重跑必须幂等；执行器退出后由后续调度恢复未完成数据。
- 验证路由策略、超时、中止、失败重试、日志清理和执行器故障转移。

### 边界

- XXL-JOB 只负责调度和管理，不承载业务处理算法。
- 定时补偿不掩盖主链路丢数据问题。
- 分片实验不等同于生产任务容量结论。

### 迁移产物

- 批量任务模板、游标状态表和幂等规范。
- Handler、Service 和 Repository 的职责边界。
- Admin 配置、分片和失败恢复操作手册。

## Stage 6：Metrics、Logs、Traces 可观测性

### Metrics

- Prometheus 采集 JVM、HTTP、Tomcat、HikariCP、Redis、RocketMQ、ES、任务和业务指标。
- 固化 Grafana 数据源和 Dashboard JSON。
- Dashboard 至少包含流量、延迟、错误、饱和度、积压和业务结果。
- 增加告警规则：错误率、P95、积压、DEAD 任务、缓存回源突增和任务失败。

### Logs

- 应用输出 JSON 结构化日志。
- 统一字段：`traceId`、`spanId`、`eventId`、业务 ID、操作人、结果、错误类型和耗时。
- Grafana Alloy 收集日志到 Loki。
- 日志实行脱敏、级别约定、保留期限和高基数字段限制。

### Traces

- 使用 Micrometer Tracing/OpenTelemetry，经 Collector 写入 Tempo。
- 串联 HTTP、JDBC、Redis、RocketMQ Producer/Consumer、ES 和 XXL-JOB。
- RocketMQ 消息属性显式传播追踪上下文。
- 异步任务创建新 Span，并通过 Link 或父子关系关联原始事件。

### 排障验收

至少完成以下演练：

```text
指标发现通知失败率上升
-> Grafana 确认开始时间和影响范围
-> Loki 按 eventId/traceId 找到错误日志
-> Tempo 定位失败 Span
-> MySQL 状态和 RocketMQ 指标确认是否可恢复
-> 执行恢复并观察指标回落
```

另行演练 Redis 故障、MQ 积压、ES 索引异常和 XXL-JOB 失败。

### 边界

- 本地监控栈不宣称长期存储、高可用监控或值班体系。
- Trace 和日志不得记录密码、Token、手机号、身份证号和医疗隐私。

### 迁移产物

- 指标命名和标签基数规范。
- JSON 日志字段规范。
- HTTP、MQ 和任务追踪传播模板。
- Dashboard、告警和排障 Runbook。

## Stage 7：综合可靠性演练

完成单项专项后，建立只用于演练的组合环境，不新增业务功能：

- Redis 故障导致受控回源，Sentinel 防止 MySQL 被打满。
- Outbox 在 RocketMQ 恢复后补发，消费者按幂等状态机处理。
- ES 同步失败进入重试，XXL-JOB 扫描并恢复。
- Prometheus、Loki 和 Tempo 能串联定位全过程。
- 执行多故障组合，并记录恢复顺序和数据一致性校验。

综合环境通过 Compose Profile 按需启动，避免日常开发必须运行全部组件。

## 测试体系

每个专项统一包含：

- 单元测试：状态转换、策略选择和错误分类。
- MySQL/Redis/MQ/ES 集成测试：优先 Testcontainers 或真实容器。
- 契约测试：事件格式、配置格式和错误响应。
- 并发测试：唯一索引竞争、任务抢占、缓存重建和超时接管。
- 压测：固定脚本、环境、数据集和重复轮次。
- 故障测试：停止、延迟、重复、超时、恢复和数据校验。
- 回归测试：原 Demo 继续通过，专项不能改变已有证据结论。

测试结果只保存摘要和必要样本，不提交超大原始日志。

## 阶段交付

每个 Stage 结束必须提交：

1. 可运行代码和配置。
2. 自动化测试。
3. Docker Compose 或 Testcontainers 验证。
4. 压测脚本和结果摘要。
5. 故障演练记录。
6. 已知限制与不能声称的内容。
7. 面试问题与代码位置映射。
8. 大项目迁移合同。
9. `docs/implementation-log.md` 追加记录。
10. 下一阶段交接提示词。

## 大项目迁移合同

专项完成后，只向医院运营协同项目迁移以下成熟产物：

- Redis 的缓存策略接口、失效任务、Key/TTL 规范和压测基线。
- RocketMQ 的事件信封、Outbox、消费状态机、幂等和人工恢复。
- ES 的 Mapping、增量同步、Alias 重建和深分页接口。
- Nacos/Sentinel 的配置校验、资源命名和降级响应。
- XXL-JOB 的批量分页、分片、进度和恢复模板。
- 可观测性的指标、日志、追踪、Dashboard 和 Runbook。

商品、预约、医生等教学领域模型不迁移。大项目重新建立医院组织、排班、考勤、
补贴、资金审计和资料 ACL 模型。

## 执行顺序

```text
Stage 0 仓库治理
-> Stage 1 Redis
-> Stage 2 RocketMQ
-> Stage 3 Elasticsearch
-> Stage 4 Nacos/Sentinel
-> Stage 5 XXL-JOB
-> Stage 6 可观测性
-> Stage 7 综合演练
```

可以提前建立大项目空白架构和需求文档，但在对应专项达到 L2 前，不把该能力
写入大项目主链路。每次只实施一个 Stage，完成验收和记录后再进入下一阶段。
