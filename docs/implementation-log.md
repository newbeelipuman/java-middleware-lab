# 实现过程记录

## 2026-06-03：Redis 缓存旁路 Demo

### 目标

建立第一个可运行的 Java 中间件 Demo，用一个商品查询接口理解缓存旁路模式：

1. 先查 Redis。
2. Redis 未命中时查询模拟数据库。
3. 将查询结果写回 Redis，并设置 TTL。
4. 更新商品时删除缓存，避免继续读取旧值。

### 技术选择

- Java 17：本机已安装。
- Spring Boot：提供最小 HTTP 应用骨架。
- Spring Data Redis：使用 `StringRedisTemplate` 操作 Redis。
- Docker Compose：以单容器启动 Redis，避免污染本机安装。
- 内存 Map：代替数据库，让这个阶段只聚焦 Redis。

### 验证策略

- 单元测试覆盖缓存命中、缓存未命中回源、数据不存在、更新后失效。
- Maven 测试不要求 Redis 正在运行。
- 手工 HTTP 验证需要启动 Redis 和应用。

### 已知限制

- 模拟数据库会在应用重启后恢复初始数据。
- 暂未处理缓存穿透、缓存击穿和缓存雪崩；这些是下一轮 Redis 练习内容。

### 验证结果

本机初始状态下 Docker Desktop 引擎未运行。启动 Docker Desktop 后，已完成
Redis 容器、Spring Boot 应用和 HTTP 接口的真实联调：

```text
cache_exists_before_get=0
first_get={"id":1,"name":"Redis Practice Book","price":59.90}
cache_exists_after_get=1
ttl_seconds=300
put={"id":1,"name":"Updated Redis Book","price":69.90}
cache_exists_after_put=0
get_after_put={"id":1,"name":"Updated Redis Book","price":69.90}
cache_after_reload={"id":1,"name":"Updated Redis Book","price":69.90}
```

这证明缓存未命中回源、TTL、更新后失效和再次回填均按预期工作。

## 2026-06-03：RocketMQ 预约异步通知 Demo

### 目标

基于医疗预约成功后的通知场景，新增 `rocketmq-notification-demo/`，验证 RocketMQ 在业务中的基本用法和可靠性边界：

1. HTTP 接口创建预约后发布 `AppointmentCreatedEvent`。
2. Producer 使用 topic/tag 和 eventId key 发送消息。
3. Consumer 使用 eventId 做幂等消费。
4. 消费失败时抛出异常，交给 RocketMQ 重试。
5. 保存 README 和 `docs/rocketmq-enterprise-development-flow.md`，方便按步骤复现。

### 技术选择

- Spring Boot 3.5.7：保持和已有 Redis Demo 的 Spring Boot 风格一致。
- RocketMQ Spring Boot Starter 2.3.3：封装 `RocketMQTemplate` 和监听器注解，降低 Demo 复杂度。
- RocketMQ 5.3.2 Docker 镜像：本地启动单 NameServer、单 Broker，用于学习联调。
- 内存 Map：保存预约和通知记录，避免在消息队列阶段引入数据库复杂度。

### 验证结果

已完成自动化测试：

```text
cd C:\Users\PC\Desktop\中间件学习\rocketmq-notification-demo
mvn test

Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

测试覆盖：

- 创建预约后发布事件。
- 同一 eventId 重复消费只写入一条通知记录。
- `patientId=500` 第一次消费失败不写成功记录，第二次处理成功，用于演示重试恢复。
- Spring 上下文能加载预约 Controller、Service、Repository 和测试发布器，避免构造器注入问题再次漏掉。

已完成 RocketMQ 容器和 HTTP 接口真实联调：

```text
docker compose up -d
mvn spring-boot:run

normal_create={"id":1001,"patientId":10,"doctorId":20,"appointmentTime":"2026-06-04T09:00:00Z","status":"CREATED"}
normal_notifications={"eventId":"2472bd58-ce02-428c-8d44-8da3ebf92d74","appointmentId":1001,"patientId":10,"status":"SENT","handledAt":"2026-06-02T23:49:50.226793700Z"}
retry_create={"id":1002,"patientId":500,"doctorId":20,"appointmentTime":"2026-06-04T10:00:00Z","status":"CREATED"}
retry_notifications={"eventId":"209a0470-a91b-4f65-a9e5-33235295b30f","appointmentId":1002,"patientId":500,"status":"SENT","handledAt":"2026-06-02T23:50:08.422143Z"}
```

关键应用日志：

```text
Published appointment event eventId=2472bd58-ce02-428c-8d44-8da3ebf92d74 appointmentId=1001
Notification recorded eventId=2472bd58-ce02-428c-8d44-8da3ebf92d74 appointmentId=1001 status=SENT
Published appointment event eventId=209a0470-a91b-4f65-a9e5-33235295b30f appointmentId=1002
Simulating transient notification failure eventId=209a0470-a91b-4f65-a9e5-33235295b30f appointmentId=1002
consume message failed. messageId:1AC5FF023FE0070DEA4E0BFAFDA80001, topic:appointment-events, reconsumeTimes:0
Notification recorded eventId=209a0470-a91b-4f65-a9e5-33235295b30f appointmentId=1002 status=SENT
```

### 已知限制

- 本地 Compose 只有单 Broker，不能证明生产高可用。
- 通知记录和预约数据都在内存中，应用重启会丢失；真实项目应改为 MySQL，并为 eventId 建唯一索引。该限制已在下方 Stage 1 修复。
- 当前只记录功能正确性，没有压测吞吐量、P95/P99 或错误率。

## 2026-06-03：RocketMQ Stage 1 MySQL 幂等落库

### 目标

把 `rocketmq-notification-demo` 中的预约和通知记录从内存 Map 改为 MySQL 持久化，让 RocketMQ 重复消费的幂等边界由 `notification_records.event_id` 唯一索引保证。

### 改动

- `compose.yaml` 新增 MySQL 8.4，数据库 `rocketmq_notification_demo`，用户 `demo/demo123456`。
- `pom.xml` 新增 `spring-boot-starter-jdbc`、`mysql-connector-j` 和测试用 H2。
- 新增 `schema.sql` 初始化 `appointments` 与 `notification_records`。
- `AppointmentRepository` 改为 `JdbcTemplate` 写入和查询 MySQL。
- `NotificationRecordStore` 改为 `JdbcTemplate`，重复 `event_id` 捕获 `DuplicateKeyException` 并返回 `false`。

### 表结构证据

```text
CREATE TABLE `appointments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patient_id` bigint NOT NULL,
  `doctor_id` bigint NOT NULL,
  `appointment_time` timestamp(6) NOT NULL,
  `status` varchar(32) NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
)

CREATE TABLE `notification_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(64) NOT NULL,
  `appointment_id` bigint NOT NULL,
  `patient_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL,
  `handled_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_event_id` (`event_id`),
  KEY `idx_notification_appointment_id` (`appointment_id`)
)
```

### 自动化测试

```text
cd C:\Users\PC\Desktop\中间件学习\rocketmq-notification-demo
mvn test

Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

测试覆盖：

- 创建预约后发布事件。
- `NotificationService` 重复 `eventId` 只记录一次。
- `patientId=500` 第一次失败、第二次成功。
- `AppointmentRepository` 使用 JDBC 保存后可查询。
- `NotificationRecordStore.saveIfAbsent` 首次返回 `true`，重复 `eventId` 返回 `false`。
- Spring 上下文能加载 Controller、Service、Repository 和测试发布器。

### 真实联调与重启验证

```text
docker compose up -d
mvn spring-boot:run

normal_create={"id":1,"patientId":10,"doctorId":20,"appointmentTime":"2026-06-04T09:00:00Z","status":"CREATED"}
normal_notifications={"eventId":"f80e68fb-106b-470c-9b2b-a37cbecfe41e","appointmentId":1,"patientId":10,"status":"SENT","handledAt":"2026-06-03T01:05:32.385991Z"}
retry_create={"id":2,"patientId":500,"doctorId":20,"appointmentTime":"2026-06-04T10:00:00Z","status":"CREATED"}
retry_notifications={"eventId":"d06f0306-a003-49b5-a820-6bbeca0a5e98","appointmentId":2,"patientId":500,"status":"SENT","handledAt":"2026-06-03T01:05:50.649162Z"}
app_restarted=true
retry_notifications_after_restart={"eventId":"d06f0306-a003-49b5-a820-6bbeca0a5e98","appointmentId":2,"patientId":500,"status":"SENT","handledAt":"2026-06-03T01:05:50.649162Z"}
```

关键日志：

```text
Published appointment event eventId=f80e68fb-106b-470c-9b2b-a37cbecfe41e appointmentId=1
Notification recorded eventId=f80e68fb-106b-470c-9b2b-a37cbecfe41e appointmentId=1 status=SENT
Published appointment event eventId=d06f0306-a003-49b5-a820-6bbeca0a5e98 appointmentId=2
Simulating transient notification failure eventId=d06f0306-a003-49b5-a820-6bbeca0a5e98 appointmentId=2
consume message failed. messageId:1AC5FF02697C070DEA4E0C404C9A0001, topic:appointment-events, reconsumeTimes:0
Notification recorded eventId=d06f0306-a003-49b5-a820-6bbeca0a5e98 appointmentId=2 status=SENT
```

### 已知限制

- MySQL 是本地单实例 Compose，只证明持久化和唯一索引幂等，不证明生产高可用。
- 当前仍是单体 Demo，尚未拆分 `appointment-service`、`notification-service` 和 `search-service`。
- 还没有 Elasticsearch 搜索、Nacos 注册配置、Sentinel 限流、XXL-JOB 补偿和 Prometheus/Grafana 观测。

## 2026-06-09：RocketMQ Stage 2 Elasticsearch 医生搜索

### 目标

在 `rocketmq-notification-demo` 内继续推进搜索能力，不拆服务、不引入 Nacos/Sentinel/XXL-JOB。MySQL 仍作为事实数据源，Elasticsearch 只作为医生搜索索引，并且索引可删除后从 MySQL 重建。

### 改动

- `compose.yaml` 新增 Elasticsearch 8.15.3 单节点容器，关闭安全认证用于本地学习联调。
- `schema.sql` 新增 `doctors` 表，字段包含 `id`、`name`、`department`、`specialty`、`available`、`updated_at`。
- 新增 `DoctorRepository`，使用 JDBC 从 MySQL 保存和查询医生主数据。
- 新增 `DoctorDataInitializer`，首次启动时写入 4 条医生样例数据。
- 新增 `DoctorSearchIndex` 和 `ElasticsearchDoctorSearchIndex`，通过 ES HTTP API 创建索引、bulk 写入和搜索。
- 新增 `DoctorSearchService` 与 `DoctorSearchController`，提供：
  - `POST /api/search/doctors/rebuild-index`
  - `GET /api/search/doctors?keyword=xxx&page=0&size=10`

### 自动化测试

```text
cd C:\Users\PC\Desktop\中间件学习\rocketmq-notification-demo
mvn test

Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

测试覆盖：

- 既有预约事件发布、RocketMQ 消费幂等、失败重试语义仍然通过。
- `DoctorRepository` 能用 JDBC 保存并查询医生数据。
- `DoctorSearchService.rebuildIndex` 从 MySQL 读取医生列表并交给索引层。
- 搜索分页参数拒绝非法 `page` 和 `size`。

### Docker 联调与恢复演练

```text
docker compose up -d
mvn spring-boot:run
```

重建索引：

```text
POST http://localhost:8081/api/search/doctors/rebuild-index
=> {"indexedCount":4}
```

关键词搜索：

```text
GET http://localhost:8081/api/search/doctors?keyword=Cardiology&page=0&size=10
=> {"content":[{"id":1,"name":"Li Ming","department":"Cardiology","specialty":"hypertension and coronary disease","available":true,"updatedAt":"2026-06-03T00:00:00Z"}],"page":0,"size":10,"totalElements":1,"totalPages":1}

GET http://localhost:8081/api/search/doctors?keyword=pain&page=0&size=2
=> {"content":[{"id":4,"name":"Zhao Lin","department":"Orthopedics","specialty":"sports injury and joint pain","available":true,"updatedAt":"2026-06-03T00:00:00Z"}],"page":0,"size":2,"totalElements":1,"totalPages":1}
```

删除索引后恢复：

```text
DELETE http://localhost:9200/doctors
=> {"acknowledged":true}

POST http://localhost:8081/api/search/doctors/rebuild-index
=> {"indexedCount":4}

GET http://localhost:8081/api/search/doctors?keyword=Neurology&page=0&size=10
=> {"content":[{"id":2,"name":"Wang Fang","department":"Neurology","specialty":"migraine and stroke rehabilitation","available":true,"updatedAt":"2026-06-03T00:00:00Z"}],"page":0,"size":10,"totalElements":1,"totalPages":1}
```

### 已知限制

- Elasticsearch 是本地单节点 Compose，只证明搜索索引和重建能力，不证明生产高可用。
- 当前同步策略是手动全量重建，尚未实现 RocketMQ 事件增量同步或定时补偿。
- 搜索使用 ES 默认分词能力，未配置中文分词器；当前样例数据使用英文姓名、科室和专长。
- 仍未开始 Nacos 拆服务、Sentinel 接口保护、XXL-JOB 补偿任务和 Prometheus/Grafana 观测。

## 2026-06-09：RocketMQ Stage 3 Nacos 服务注册与配置管理

### 目标

在不破坏 Stage 1 MySQL 幂等落库和 Stage 2 Elasticsearch 搜索的前提下，增加 Nacos 服务注册与配置管理能力。当前阶段不启动 Sentinel、XXL-JOB、Prometheus/Grafana。考虑到直接拆成多个 Maven 模块会显著增加启动顺序和排障成本，本阶段先保留单体应用，但在注册元数据和接口中明确 `appointment`、`notification`、`search` 三个边界。

### 改动

- `compose.yaml` 新增 Nacos 2.4.3 standalone 容器。
- `pom.xml` 新增 `nacos-client` 2.4.3。
- 新增 `NacosRegistrationRunner`，应用启动后注册 `appointment-notification-service` 到 Nacos。
- 新增 `NacosConfigRunner`，读取并监听 `rocketmq-notification-demo.properties`。
- 新增 `SearchSettings`，由本地配置或 Nacos 配置控制医生搜索默认分页大小。
- `DoctorSearchController` 在请求未传 `size` 时使用动态默认分页大小。
- 新增 `GET /api/nacos/status`，返回 Nacos 启用状态、注册状态、配置 dataId、当前搜索默认分页大小和业务边界。

### 自动化测试

```text
cd C:\Users\PC\Desktop\中间件学习\rocketmq-notification-demo
mvn test

Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

测试覆盖：

- 既有预约事件发布、RocketMQ 消费幂等、失败重试语义仍然通过。
- 既有 MySQL 医生数据、ES 索引重建和分页参数测试仍然通过。
- `NacosConfigRunner` 能应用 `app.search.default-page-size=2`。
- 无效 Nacos 配置不会覆盖当前搜索默认分页大小。
- `DoctorSearchController` 在不传 `size` 时使用动态默认分页大小。

### Docker 联调与配置刷新演练

启动中间件：

```text
docker compose up -d
```

写入 Nacos 配置：

```text
POST http://localhost:8848/nacos/v1/cs/configs
dataId=rocketmq-notification-demo.properties
group=DEFAULT_GROUP
content=app.search.default-page-size=2
=> true

GET http://localhost:8848/nacos/v1/cs/configs?dataId=rocketmq-notification-demo.properties&group=DEFAULT_GROUP
=> app.search.default-page-size=2
```

启动应用后查询 Nacos 状态：

```text
GET http://localhost:8081/api/nacos/status
=> {"enabled":true,"registered":true,"serverAddr":"127.0.0.1:8848","serviceName":"appointment-notification-service","configDataId":"rocketmq-notification-demo.properties","searchDefaultPageSize":2,"serviceBoundaries":["appointment","notification","search"]}
```

从 Nacos 注册表查询实例：

```text
GET http://localhost:8848/nacos/v1/ns/instance/list?serviceName=appointment-notification-service&groupName=DEFAULT_GROUP
=> hosts[0].ip=127.0.0.1
=> hosts[0].port=8081
=> hosts[0].healthy=true
=> hosts[0].metadata.stage=middleware-stage-3-nacos-services
```

验证 Nacos 配置影响搜索默认分页：

```text
POST http://localhost:8081/api/search/doctors/rebuild-index
=> {"indexedCount":4}

GET http://localhost:8081/api/search/doctors?keyword=
=> {"content":[...2 records...],"page":0,"size":2,"totalElements":4,"totalPages":2}
```

验证已有预约通知链路未回退：

```text
POST http://localhost:8081/api/appointments
=> {"id":3,"patientId":10,"doctorId":20,"appointmentTime":"2026-06-04T09:00:00Z","status":"CREATED"}

GET http://localhost:8081/api/appointments/3/notifications
=> {"eventId":"baa95395-ad1c-49bb-b084-79804a701807","appointmentId":3,"patientId":10,"status":"SENT","handledAt":"2026-06-09T07:25:13.165325Z"}
```

动态配置刷新演练：

```text
POST http://localhost:8848/nacos/v1/cs/configs
content=app.search.default-page-size=3
=> true

GET http://localhost:8081/api/nacos/status
=> ... "searchDefaultPageSize":3 ...

GET http://localhost:8081/api/search/doctors?keyword=
=> {"content":[...3 records...],"page":0,"size":3,"totalElements":4,"totalPages":2}
```

### 已知限制

- 当前使用 Nacos 原生 client，而不是 Spring Cloud Alibaba 自动装配；这是为了避免 Spring Boot 3.5.7 与 Spring Cloud Alibaba 版本兼容性干扰当前学习目标。
- 当前仍是单体应用，只通过注册信息和代码边界表达 appointment、notification、search，没有真正拆成多个独立部署服务。
- Nacos 是本地 standalone 容器，只证明注册发现和配置读取/刷新，不证明生产高可用或微服务治理经验。
- 配置刷新只覆盖搜索默认分页大小，尚未覆盖通知开关、路由策略或 Sentinel 规则。

## 2026-06-09：RocketMQ Stage 4 Sentinel 接口保护

### 目标

在已有 `rocketmq-notification-demo` 单体应用中增加 Sentinel 接口保护，优先保护真实业务入口：

1. `POST /api/appointments`：预约创建入口，避免请求洪峰下无限排队。
2. `GET /api/search/doctors`：医生搜索入口，避免搜索请求打满后端索引服务。

本阶段不启动 XXL-JOB、Prometheus/Grafana，也不把当前单体拆成多个服务。MySQL 仍是事实数据源，Elasticsearch 仍是可重建搜索索引，Nacos 仍用于本地服务注册和搜索默认分页配置。

### 改动

- `pom.xml` 新增 `sentinel-core` 1.8.8。
- `application.yml` 新增本地 Sentinel 配置：
  - `app.sentinel.appointment-create-qps=2`
  - `app.sentinel.doctor-search-qps=3`
- 新增 `SentinelRuleInitializer`，应用启动后加载 `appointment.create` 和 `doctor.search` 两条 QPS 规则。
- 新增 `SentinelGuard`，在控制器入口通过 `SphU.entry(resource)` 保护真实接口。
- `AppointmentController.create` 接入 `appointment.create` 资源。
- `DoctorSearchController.search` 接入 `doctor.search` 资源。
- 新增 `ApiExceptionHandler` 和 `RateLimitResponse`，命中限流时返回 HTTP 429 与可解释 JSON。

### 自动化测试

```text
cd C:\Users\PC\Desktop\中间件学习\rocketmq-notification-demo
mvn test

Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

新增测试覆盖：

- `SentinelGuard` 在未超限时放行业务调用。
- 同一 Sentinel 资源超过 QPS 规则时抛出 `SentinelRateLimitException`，且不会继续执行业务函数。
- `ApiExceptionHandler` 将 Sentinel 限流异常转换为 HTTP 429。
- 既有预约事件发布、MySQL 幂等、ES 搜索、Nacos 默认分页测试继续通过。

### Docker Compose 联调

启动中间件和应用：

```text
docker compose up -d
mvn spring-boot:run
```

启动日志确认 Sentinel 规则加载：

```text
Sentinel rules loaded appointmentCreateQps=2.0 doctorSearchQps=3.0
```

验证既有搜索接口仍可用：

```text
POST http://localhost:8081/api/search/doctors/rebuild-index
=> {"indexedCount":4}

GET http://localhost:8081/api/search/doctors?keyword=Cardiology&page=0&size=10
=> {"content":[{"id":1,"name":"Li Ming","department":"Cardiology","specialty":"hypertension and coronary disease","available":true,"updatedAt":"2026-06-03T00:00:00Z"}],"page":0,"size":10,"totalElements":1,"totalPages":1}
```

验证既有预约通知链路仍可用：

```text
POST http://localhost:8081/api/appointments
=> {"id":4,"patientId":10,"doctorId":20,"appointmentTime":"2026-06-04T09:00:00Z","status":"CREATED"}

GET http://localhost:8081/api/appointments/4/notifications
=> {"eventId":"f2312181-3dac-4996-99e0-2141e0ae235d","appointmentId":4,"patientId":10,"status":"SENT","handledAt":"2026-06-09T07:43:51.668533Z"}
```

### 限流演练

快速请求医生搜索接口：

```text
curl.exe -s -i "http://localhost:8081/api/search/doctors?keyword=&page=0&size=1"
```

实测连续请求返回：

```text
attempt 1 => HTTP/1.1 200
attempt 2 => HTTP/1.1 200
attempt 3 => HTTP/1.1 200
attempt 4 => HTTP/1.1 429
{"code":"RATE_LIMITED","message":"Request was blocked by Sentinel flow control. Please retry later.","resource":"doctor.search","reason":"local-qps-rule"}
attempt 5 => HTTP/1.1 429
{"code":"RATE_LIMITED","message":"Request was blocked by Sentinel flow control. Please retry later.","resource":"doctor.search","reason":"local-qps-rule"}
```

关键应用日志：

```text
Sentinel blocked request resource=doctor.search limitApp=default
```

### 已知限制

- 当前使用 Sentinel 本地内存规则，只证明接口限流语义，不证明集群规则下发、控制台治理或生产高可用。
- 当前只实现 QPS 限流，尚未实现慢调用熔断、异常比例熔断或热点参数规则。
- 限流后返回明确 429，而不是业务降级推荐结果；后续可以为搜索接口补充热门医生兜底。
- 这次快速请求只能证明本地规则可触发，不生成吞吐量、P95/P99 或容量结论。

### 下一阶段交接提示词

```text
继续推进 Stage 5：XXL-JOB 补偿任务。基于当前 `rocketmq-notification-demo`，Stage 1 MySQL 幂等落库、Stage 2 Elasticsearch 搜索、Stage 3 Nacos 注册配置、Stage 4 Sentinel 接口保护均已完成并通过 `mvn test` 与 Docker HTTP 联调。请只实现 XXL-JOB 本地补偿任务，不引入 Prometheus/Grafana。优先选择一个真实补偿场景，例如扫描通知记录或 ES 索引补偿，要求包含 Docker Compose、自动化测试、手工触发或调度演练、失败重试/幂等说明、实现日志和 README 更新；不要声称生产高可用或线上调度经验。
```

## 2026-06-09：RocketMQ Stage 5 XXL-JOB 补偿任务

### 目标

在 `rocketmq-notification-demo` 中增加真实补偿场景：当 Elasticsearch `doctors` 索引缺失或漂移时，通过 XXL-JOB handler 从 MySQL 医生主数据全量重建索引。MySQL 仍是事实数据源，ES 仍是可重建搜索索引，本阶段不引入 Prometheus/Grafana。

### 中途发现并修复的错误

- `mvn test` 曾在测试编译阶段报 `找不到符号`。原因是当前中文路径工作区下 Maven/JDK 对 `target/classes` 的测试 classpath 解析异常。修复方式是在 `pom.xml` 中使用 `build-helper-maven-plugin` 将 `src/main/java` 加入 test source roots，避免测试编译依赖异常解析的 `target/classes` 路径。
- `mvn package -DskipTests` 后可执行 jar 首次启动失败，日志为 `DoctorIndexCompensationService: No default constructor found`。原因是补偿 Service 同时存在生产构造器和测试用构造器，Spring 未明确选择生产构造器。修复方式是在 `DoctorIndexCompensationService(DoctorSearchService)` 上添加 `@Autowired`。

### 改动

- `pom.xml` 新增 `xxl-job-core` 2.4.1，并保留上述测试编译路径修复。
- `application.yml` 新增 `app.xxl-job.*` 配置，默认 `enabled=false`，本地学习时不连接 XXL-JOB Admin。
- 新增 `XxlJobProperties` 和 `XxlJobConfiguration`，开启配置后注册 `XxlJobSpringExecutor`。
- 新增 `DoctorIndexCompensationService`，调用 `DoctorSearchService.rebuildIndex()` 并返回执行结果。
- 新增 `DoctorIndexCompensationJob`，提供 `@XxlJob("doctorIndexCompensationJob")` handler 和手工运行方法。
- 新增 `CompensationJobController`，提供 `POST /api/jobs/doctor-index-compensation/run` 用于本地演练。

### 自动化测试

```text
cd C:\Users\PC\Desktop\中间件学习\rocketmq-notification-demo
mvn test

Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

新增测试覆盖：

- 补偿 Service 会调用 `DoctorSearchService.rebuildIndex()`，并返回 `COMPLETED`、索引数量和处理时间。
- XXL-JOB handler 和手工入口共用同一个补偿 Service。
- `POST /api/jobs/doctor-index-compensation/run` 能返回补偿执行结果。
- 原有预约通知、MySQL 幂等、ES 搜索、Nacos 配置和 Sentinel 限流测试继续通过。

### Docker Compose 集成检查

```text
docker compose ps

rocketmq-notification-broker          Up
rocketmq-notification-elasticsearch   Up  0.0.0.0:9200->9200/tcp
rocketmq-notification-mysql           Up  0.0.0.0:3306->3306/tcp
rocketmq-notification-nacos           Up  0.0.0.0:8848->8848/tcp
rocketmq-notification-namesrv         Up  0.0.0.0:9876->9876/tcp
```

可执行 jar 前台启动验证：

```text
java -jar target\rocketmq-notification-demo-0.0.1-SNAPSHOT.jar

Tomcat started on port 8081 (http) with context path '/'
Started RocketMqNotificationApplication in 21.215 seconds
Registered service to Nacos serviceName=appointment-notification-service group=DEFAULT_GROUP ip=127.0.0.1 port=8081
Sentinel rules loaded appointmentCreateQps=2.0 doctorSearchQps=3.0
```

### 故障恢复演练

删除 ES 医生索引：

```text
DELETE http://127.0.0.1:9200/doctors
=> {"acknowledged":true}

GET http://127.0.0.1:9200/_cat/indices?v
=> no doctors index
```

触发补偿任务：

```text
POST http://127.0.0.1:8081/api/jobs/doctor-index-compensation/run
=> {"jobName":"doctorIndexCompensationJob","status":"COMPLETED","indexedCount":4,"handledAt":"2026-06-09T09:02:22.355785500Z"}
```

确认搜索恢复：

```text
GET http://127.0.0.1:8081/api/search/doctors?keyword=Neurology&page=0&size=10
=> {"content":[{"id":2,"name":"Wang Fang","department":"Neurology","specialty":"migraine and stroke rehabilitation","available":true,"updatedAt":"2026-06-03T00:00:00Z"}],"page":0,"size":10,"totalElements":1,"totalPages":1}

GET http://127.0.0.1:9200/_cat/indices/doctors?v
=> doctors docs.count=4
```

回归预约通知链路：

```text
POST http://127.0.0.1:8081/api/appointments
=> {"id":5,"patientId":9001,"doctorId":2,"appointmentTime":"2026-06-10T09:00:00Z","status":"CREATED"}

GET http://127.0.0.1:8081/api/appointments/5/notifications
=> {"eventId":"0a81e57c-b226-4387-8220-8fed9e9ec1ca","appointmentId":5,"patientId":9001,"status":"SENT","handledAt":"2026-06-09T09:03:08.804008Z"}
```

### 已知限制

- 当前只验证了 XXL-JOB handler 代码和本地手工触发入口，未启动 XXL-JOB Admin 容器，也未验证 Admin 调度、失败重试、分片广播或执行器集群。
- 补偿策略是全量重建 ES 医生索引，适合当前 4 条样例数据；未实现大数据量分批、增量同步或索引别名无缝切换。
- ES 是本地单节点 Compose，MySQL 也是本地单实例，不证明生产高可用。
- 本阶段没有采集任务耗时、失败次数、HTTP 延迟或 RocketMQ 消费指标；这些留到 Stage 6。

### 下一阶段交接提示词

```text
继续推进 Stage 6：Prometheus/Grafana 观测。基于当前 `rocketmq-notification-demo`，Stage 1 MySQL 幂等落库、Stage 2 Elasticsearch 医生搜索、Stage 3 Nacos 注册配置、Stage 4 Sentinel 接口保护、Stage 5 XXL-JOB ES 索引补偿均已完成，并通过 `mvn test`、Docker Compose 检查、HTTP 联调、ES 删除后补偿恢复演练和预约通知链路回归。请只实现 Prometheus/Grafana 观测，不引入新的业务中间件。优先采集 HTTP 请求计数/耗时、预约事件发布与通知消费结果、医生索引补偿执行次数/耗时/失败数、Sentinel 限流次数等能服务排障的问题；要求包含自动化测试、Docker Compose 或本地端点验证、一次故障/限流/补偿演练的指标变化记录、README 与实现日志更新；不要声称生产监控经验、线上容量结论或未经压测的 P95/P99。
```

## 2026-06-09：RocketMQ Stage 6 Prometheus/Grafana 观测

### 目标

为 `rocketmq-notification-demo` 增加最小可行观测闭环：HTTP 指标由 Actuator/Micrometer 暴露，业务指标覆盖预约事件发布、通知消费、ES 索引补偿和 Sentinel 限流。Prometheus 负责本地抓取，Grafana 保留为本地可视化入口。本阶段不引入新的业务中间件，不声明生产监控、告警治理或容量结论。

### 调整计划和档期

考虑用户本周 Plus 额度剩余有限，并预计只有约 35% 可继续投入本项目，后续安排收紧为：

1. 本轮完成 RocketMQ 主线最后阶段：Prometheus/Grafana 观测，范围控制在 Actuator、业务指标、Prometheus 抓取和基础 Grafana 入口。
2. 下一小轮只做 Redis 缓存穿透、击穿、雪崩的最小可验证版本，不展开复杂压测平台。
3. 最后一小轮生成简历可写表述和面试追问清单，只基于已验证证据，不写未压测的 P95/P99、百万并发或高可用结论。

### 改动

- `pom.xml` 新增 `spring-boot-starter-actuator` 和 `micrometer-registry-prometheus`。
- `application.yml` 暴露 `health`、`info`、`prometheus`，并给指标增加 `application=rocketmq-notification-demo` 标签。
- 新增 `BusinessMetrics`，记录业务计数器和补偿耗时：
  - `appointment.events.published`
  - `notification.consumption`
  - `compensation.runs`
  - `compensation.duration`
  - `sentinel.blocks`
- `AppointmentService` 记录预约事件发布成功/失败。
- `NotificationService` 记录通知消费 `sent`、`duplicate`、`failed`。
- `DoctorIndexCompensationService` 记录补偿执行次数、结果和耗时。
- `SentinelGuard` 记录限流命中次数。
- `compose.yaml` 新增 Prometheus 和 Grafana。
- 新增 `prometheus.yml`，抓取 `host.docker.internal:8081/actuator/prometheus`。

### 自动化测试

```text
cd C:\Users\PC\Desktop\中间件学习\rocketmq-notification-demo
mvn test

Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

新增测试覆盖：

- `BusinessMetricsTest` 验证业务 counter 和补偿 timer 可以写入 `MeterRegistry`。
- Spring 最小上下文测试补充测试用 `BusinessMetrics`，确保新增指标依赖不破坏应用启动。
- 原有预约通知、MySQL 幂等、ES 搜索、Nacos 配置、Sentinel 限流和 XXL-JOB 补偿测试继续通过。

### Docker Compose 和 HTTP 验证

```text
docker compose up -d
```

实测容器状态：

```text
rocketmq-notification-broker          Up
rocketmq-notification-elasticsearch   Up  0.0.0.0:9200->9200/tcp
rocketmq-notification-grafana         Up  0.0.0.0:3000->3000/tcp
rocketmq-notification-mysql           Up  0.0.0.0:3306->3306/tcp
rocketmq-notification-nacos           Up  0.0.0.0:8848->8848/tcp
rocketmq-notification-namesrv         Up  0.0.0.0:9876->9876/tcp
rocketmq-notification-prometheus      Up  0.0.0.0:9090->9090/tcp
```

可执行 jar 打包：

```text
mvn package -DskipTests
=> BUILD SUCCESS
```

应用启动后，指标端点可访问：

```text
GET http://127.0.0.1:8081/actuator/prometheus
=> HTTP 200
=> contains jvm_memory_used_bytes
```

Grafana 登录页可访问：

```text
GET http://127.0.0.1:3000/login
=> HTTP 200
```

### 指标变化演练

触发预约通知、ES 补偿和 Sentinel 限流：

```text
POST http://127.0.0.1:8081/api/appointments
=> {"id":6,"patientId":9101,"doctorId":2,"appointmentTime":"2026-06-10T10:00:00Z","status":"CREATED"}

GET http://127.0.0.1:8081/api/appointments/6/notifications
=> one SENT notification

DELETE http://127.0.0.1:9200/doctors
POST http://127.0.0.1:8081/api/jobs/doctor-index-compensation/run
=> {"status":"COMPLETED","indexedCount":4}

GET http://127.0.0.1:8081/api/search/doctors?keyword=&page=0&size=1
=> 200, 200, 200, 429, 429
```

应用 Prometheus 文本中出现：

```text
appointment_events_published_total{application="rocketmq-notification-demo",result="success"} 1.0
notification_consumption_total{application="rocketmq-notification-demo",result="sent"} 1.0
compensation_runs_total{application="rocketmq-notification-demo",job="doctorIndexCompensationJob",result="completed"} 1.0
compensation_duration_seconds_count{application="rocketmq-notification-demo",job="doctorIndexCompensationJob",result="completed"} 1
sentinel_blocks_total{application="rocketmq-notification-demo",resource="doctor.search"} 2.0
http_server_requests_seconds_count{application="rocketmq-notification-demo",status="429",uri="/api/search/doctors"} 2
```

Prometheus 抓取状态：

```text
GET http://127.0.0.1:9090/api/v1/targets
=> health="up", scrapeUrl="http://host.docker.internal:8081/actuator/prometheus"

GET http://127.0.0.1:9090/api/v1/query?query=sentinel_blocks_total
=> sentinel_blocks_total{resource="doctor.search"} 2
```

### 已知限制

- 当前 Grafana 只验证登录入口可访问，未固化 dashboard JSON；后续如有额度可以补一个最小 dashboard。
- Prometheus 抓取目标使用 `host.docker.internal:8081`，适合 Windows 本地宿主机运行 Spring Boot；如果改为容器化应用，需要调整 target。
- 当前只采集本地学习指标，不包含告警规则、容量规划、RocketMQ broker 内部指标或生产级日志链路。
- 没有进行压测，因此不写 P95/P99、吞吐量或容量结论。

### 下一阶段交接提示词

```text
继续在有限额度下收口中间件证据链。RocketMQ 主线 Stage 1-6 已完成：MySQL 幂等落库、Elasticsearch 搜索、Nacos 注册配置、Sentinel 接口保护、XXL-JOB ES 索引补偿、Prometheus/Grafana 本地观测均已有代码、测试、Docker/HTTP 验证和实现日志。下一步只做 Redis 缓存穿透、击穿、雪崩的最小可验证版本，不引入新大型中间件；要求补测试、README、实现日志和可复现命令，最后生成基于证据的简历表述与面试追问清单。不要写生产高可用、百万并发、未经压测的 P95/P99 或线上调优经验。
```

## 2026-06-09：Redis 缓存穿透、击穿、雪崩最小补强

### 目标

在不引入新大型中间件的前提下，补齐 `redis-cache-demo` 的三个常见缓存风险场景：缓存穿透、缓存击穿和缓存雪崩。当前阶段只要求最小可验证实现，不做生产集群、复杂压测平台或容量结论。

### 改动

- `ProductCache` 增加 `isKnownMissing(id)` 和 `putMissing(id)`。
- `RedisProductCache` 对不存在商品写入 `__MISSING__`，使用 `demo.cache.product-missing-ttl=30s`。
- `RedisProductCache` 对正常商品 TTL 增加 `demo.cache.product-ttl-jitter=30s` 随机抖动，基础 TTL 仍是 `5m`。
- `ProductService` 在缓存未命中时按商品 ID 使用本地互斥锁，并在锁内二次检查缓存，避免同一热点 key 并发重复回源。
- `README.md` 增加风险补强说明和本地验证命令。

### 自动化测试

```text
cd C:\Users\PC\Desktop\中间件学习\redis-cache-demo
mvn test

Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

新增测试覆盖：

- 缓存命中时不访问 repository。
- 缓存未命中且查到商品时写入正常缓存。
- 商品不存在时写入 missing 缓存。
- missing 缓存命中时直接返回空，不再访问 repository。
- 更新商品后删除旧缓存。
- 6 个并发线程同时查询同一热点 key 时，只触发 1 次 repository 回源。
- 正常商品 TTL 抖动范围落在 5 分钟到 5 分 30 秒之间。

### Docker + HTTP/Redis 联调

```text
docker compose up -d
mvn package -DskipTests
java -jar target\redis-cache-demo-0.0.1-SNAPSHOT.jar
```

缓存穿透验证：

```text
GET http://127.0.0.1:8080/api/products/99
=> HTTP 404

redis-cli GET demo:product:99
=> __MISSING__

redis-cli TTL demo:product:99
=> 17
```

TTL 抖动验证：

```text
GET http://127.0.0.1:8080/api/products/1
=> {"id":1,"name":"Java Middleware Handbook","price":49.90}

redis-cli TTL demo:product:1
=> 315
```

### 已知限制

- 本地互斥锁只保护单个 JVM 实例；多实例部署需要 Redis 分布式锁、逻辑过期、异步刷新或热点 key 预热等更完整方案。
- 空值缓存只适合短 TTL；如果业务存在“稍后创建商品”的场景，需要结合更新事件删除空值缓存。
- TTL 抖动只能降低同一时间大面积过期概率，不等于解决所有雪崩问题；还需要容量、限流、降级和预热策略。
- 本阶段没有做压测，因此不记录 P95/P99、吞吐量或错误率。

### 下一阶段交接提示词

```text
基于当前仓库已完成的 Redis 风险补强和 RocketMQ 主线 Stage 1-6，生成一份简历可写表述和面试追问清单。只允许引用仓库中已经验证的代码、测试、Docker/HTTP 输出和实现日志；不要写生产高可用、百万并发、未经压测的 P95/P99、线上调优经验或 Kubernetes 经验。输出应包含：3-5 条项目表述、每条对应的证据文件、可能面试追问、我必须能口述的技术边界。
```
