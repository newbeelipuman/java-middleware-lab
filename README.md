# Java 中间件学习项目

这个仓库用于从最小可运行 Demo 开始学习 Java 后端常见中间件。每个 Demo
都包含运行方式、核心场景、验证命令和实现过程。

## 学习顺序

| 顺序 | 技术 | 先掌握的场景 | 原因 |
| --- | --- | --- | --- |
| 1 | Redis | 缓存旁路、TTL、缓存失效 | Java 后端岗位中出现频率高，入门成本低，能直接理解缓存一致性 |
| 2 | Kafka 或 RocketMQ | 订单事件、异步消费、重复消费处理 | 消息队列是微服务岗位的常见要求，重点是可靠性而不是 API |
| 3 | Elasticsearch | 商品搜索、倒排索引、分页 | 搜索和日志检索岗位常见，适合在掌握数据库后补充 |
| 4 | MySQL 进阶 | 索引、事务、锁、慢查询 | 严格说不是中间件，但几乎所有 Java 后端岗位都会考察 |
| 5 | Spring Cloud Alibaba | 服务注册、配置、限流 | 用于串联微服务能力，建议在单体 Demo 熟练后学习 |
| 6 | 分布式任务与可观测性 | XXL-JOB、Prometheus、链路追踪 | 面向生产运维和故障排查能力 |

第一阶段从 [redis-cache-demo](./redis-cache-demo/README.md) 开始。
第二阶段从 [rocketmq-notification-demo](./rocketmq-notification-demo/README.md) 练习 RocketMQ 预约异步通知、幂等消费和失败重试。
RocketMQ 开发流程记录见 [docs/rocketmq-enterprise-development-flow.md](./docs/rocketmq-enterprise-development-flow.md)。

结合现有简历补齐能力证据的路线见
[docs/resume-driven-roadmap.md](./docs/resume-driven-roadmap.md)。

后续专项完善以
[Java 中间件专项完善路线](./docs/middleware-specialization-roadmap.md)
为唯一总路线。该路线覆盖 Redis 压测与高可用、RocketMQ 可靠发布和消费状态机、
Elasticsearch 索引生命周期、Nacos/Sentinel 治理、XXL-JOB Admin，以及
Prometheus/Loki/Tempo 可观测性闭环。原有 Demo 保留为学习基线，专项能力在独立
V2 目录实现。

已完成 Demo 后的复习材料：

- [中间件使用场景与面试复盘卡片](./docs/middleware-learning-cards.md)：整理 Redis、RocketMQ、MySQL、Elasticsearch、Nacos、Sentinel、XXL-JOB、Prometheus/Grafana 的使用原因、项目场景、企业级用法、生产差距和面试问答。
- [中间件 Demo 技术复盘](./docs/middleware-demo-technical-review.md)：说明当前验证程度、可迁移能力和不能夸大的边界。

## 市场说明

中间件技能不会产生可以单独归因的固定涨薪比例。招聘定价通常同时受城市、
工作年限、项目复杂度、行业、学历、系统设计能力和面试表现影响。

当前调研结论、证据和薪资解释见 [docs/market-research.md](./docs/market-research.md)。
