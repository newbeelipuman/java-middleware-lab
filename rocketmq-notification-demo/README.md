# RocketMQ 预约通知企业级最小 Demo

## 场景

预约创建成功后，不在 HTTP 请求里直接发送通知，而是发布 RocketMQ 事件，由消费者异步处理：

```text
POST /api/appointments
  -> 保存预约
  -> 发送 appointment-events:appointment-created 消息
  -> 返回预约创建结果

NotificationConsumer
  -> 消费 AppointmentCreatedEvent
  -> 用 eventId 做幂等
  -> 发送通知成功后记录 NotificationRecord
  -> 抛出异常时交给 RocketMQ 重试
```

这个 Demo 验证的是消息队列在业务里的边界：异步解耦、幂等消费、失败重试和可观测的处理记录。它不是生产高可用集群。

## 运行

需要 Java 17、Maven 和已启动的 Docker Desktop。

```powershell
docker compose up -d
mvn spring-boot:run
```

另开一个终端创建正常预约：

```powershell
$body = '{"patientId":10,"doctorId":20,"appointmentTime":"2026-06-04T09:00:00Z"}'
$appointment = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/appointments -ContentType application/json -Body $body
$appointment
Start-Sleep -Seconds 3
Invoke-RestMethod http://localhost:8081/api/appointments/$($appointment.id)/notifications
```

预期结果：能查到一条 `SENT` 通知记录。

## 重试演练

`patientId=500` 会在第一次消费时抛出一次模拟异常，RocketMQ 重投后第二次成功：

```powershell
$body = '{"patientId":500,"doctorId":20,"appointmentTime":"2026-06-04T10:00:00Z"}'
$appointment = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/appointments -ContentType application/json -Body $body
Start-Sleep -Seconds 20
Invoke-RestMethod http://localhost:8081/api/appointments/$($appointment.id)/notifications
```

同时可以观察应用日志，第一次会出现 `Simulated transient notification failure for retry drill`，随后同一个事件会被重新消费并写入通知记录。

## 自动化验证

单元测试不依赖正在运行的 RocketMQ：

```powershell
mvn test
```

当前覆盖：

1. 创建预约后发布 `AppointmentCreatedEvent`。
2. 重复消息按 `eventId` 幂等处理，只写入一条通知记录。
3. 第一次消费失败时不写成功记录，下一次处理成功。

## 阅读顺序

1. `AppointmentService`：业务保存与事件发布边界。
2. `RocketMqAppointmentEventPublisher`：topic、tag、message key 和同步发送。
3. `NotificationConsumer`：RocketMQ 消费入口。
4. `NotificationService`：幂等、失败重试和成功记录。
5. `NotificationServiceTest`：不用真实 MQ 也能验证核心可靠性语义。

## 企业级注意点

- Producer 同步发送只能证明消息到达 Broker，不等于业务最终成功。
- Consumer 必须幂等，因为 MQ 重投、服务重启和网络抖动都可能造成重复消费。
- Stage 1 后通知结果已落 MySQL，并通过 `notification_records.event_id` 唯一索引保证幂等；当前 MySQL 仍是本地单实例 Compose，不代表生产高可用。
- 本地 Compose 是单 NameServer、单 Broker，只用于学习和故障演练；不能声称高可用。
- 简历表述只能写“实现并验证了异步通知、幂等消费和失败重试”，不要写未压测的吞吐量或生产级可用性。

## MySQL 幂等落库

Stage 1 已将预约和通知记录从内存 Map 改为 MySQL 持久化。`notification_records.event_id` 有唯一索引，是消费幂等的最终防线。

Compose 会启动 MySQL：

```powershell
docker compose up -d
```

连接信息：

```text
database=rocketmq_notification_demo
username=demo
password=demo123456
port=3306
```

核心表：

```text
appointments(id, patient_id, doctor_id, appointment_time, status, created_at)
notification_records(id, event_id, appointment_id, patient_id, status, handled_at)
UNIQUE KEY uk_notification_event_id(event_id)
```

验证唯一索引：

```powershell
docker compose exec mysql mysql -udemo -pdemo123456 rocketmq_notification_demo -e "SHOW CREATE TABLE notification_records\G"
```

重启验证：

1. 创建 `patientId=500` 的预约并等待重试成功。
2. 停止 `mvn spring-boot:run`。
3. 不删除 MySQL 容器和 volume，重新启动应用。
4. 再次请求 `GET /api/appointments/{id}/notifications`，仍应返回同一条 `SENT` 记录。

## Elasticsearch 医生搜索

Stage 2 在当前单体 Demo 内增加医生搜索。MySQL 仍是事实数据源，Elasticsearch 只保存可重建的搜索索引。

Compose 会额外启动 Elasticsearch：

```powershell
docker compose up -d
mvn spring-boot:run
```

重建医生索引：

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/search/doctors/rebuild-index
```

预期返回：

```json
{"indexedCount":4}
```

按姓名、科室或专长搜索：

```powershell
Invoke-RestMethod "http://localhost:8081/api/search/doctors?keyword=Cardiology&page=0&size=10"
Invoke-RestMethod "http://localhost:8081/api/search/doctors?keyword=pain&page=0&size=2"
```

搜索结果包含分页字段：

```json
{
  "content": [
    {
      "id": 1,
      "name": "Li Ming",
      "department": "Cardiology",
      "specialty": "hypertension and coronary disease",
      "available": true,
      "updatedAt": "2026-06-03T00:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

索引恢复演练：

```powershell
Invoke-RestMethod -Method Delete -Uri http://localhost:9200/doctors
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/search/doctors/rebuild-index
Invoke-RestMethod "http://localhost:8081/api/search/doctors?keyword=Neurology&page=0&size=10"
```

删除索引后仍能通过重建接口从 MySQL 恢复搜索结果，说明 ES 不是唯一数据源。

## Nacos 服务注册与配置管理

Stage 3 使用 Nacos 原生 client 做最小可验证服务治理实验。当前仍保留单体应用，不物理拆成多个 Maven 模块；代码和接口中明确区分 `appointment`、`notification`、`search` 三个业务边界。这样能先验证注册发现和配置中心，避免为了“微服务”过早引入多模块启动顺序和排障成本。

Compose 会额外启动 Nacos：

```powershell
docker compose up -d
```

写入 Nacos 配置，控制医生搜索默认分页大小：

```powershell
curl.exe -X POST "http://localhost:8848/nacos/v1/cs/configs" `
  -d "dataId=rocketmq-notification-demo.properties" `
  -d "group=DEFAULT_GROUP" `
  --data-urlencode "content=app.search.default-page-size=2"
```

启动应用：

```powershell
mvn spring-boot:run
```

查看 Nacos 状态：

```powershell
Invoke-RestMethod http://localhost:8081/api/nacos/status
```

预期能看到：

```json
{
  "enabled": true,
  "registered": true,
  "serviceName": "appointment-notification-service",
  "searchDefaultPageSize": 2,
  "serviceBoundaries": ["appointment", "notification", "search"]
}
```

从 Nacos 查询服务实例：

```powershell
Invoke-RestMethod "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=appointment-notification-service&groupName=DEFAULT_GROUP"
```

不传 `size` 搜索医生时，会使用 Nacos 配置的默认分页大小：

```powershell
Invoke-RestMethod "http://localhost:8081/api/search/doctors?keyword="
```

动态配置刷新演练：

```powershell
curl.exe -X POST "http://localhost:8848/nacos/v1/cs/configs" `
  -d "dataId=rocketmq-notification-demo.properties" `
  -d "group=DEFAULT_GROUP" `
  --data-urlencode "content=app.search.default-page-size=3"

Invoke-RestMethod http://localhost:8081/api/nacos/status
Invoke-RestMethod "http://localhost:8081/api/search/doctors?keyword="
```

预期应用无需重启，`searchDefaultPageSize` 和搜索结果中的 `size` 都变为 `3`。

## Sentinel 接口保护

Stage 4 在已有真实业务接口上增加 Sentinel 本地 QPS 限流，而不是新建孤立限流 Demo。当前保护资源：

```text
POST /api/appointments        -> Sentinel resource: appointment.create, 2 QPS
GET /api/search/doctors       -> Sentinel resource: doctor.search, 3 QPS
```

限流命中时返回 HTTP 429：

```json
{
  "code": "RATE_LIMITED",
  "message": "Request was blocked by Sentinel flow control. Please retry later.",
  "resource": "doctor.search",
  "reason": "local-qps-rule"
}
```

快速重复请求演练：

```powershell
1..5 | ForEach-Object {
  curl.exe -s -i "http://localhost:8081/api/search/doctors?keyword=&page=0&size=1"
}
```

预期前几次返回 200，超过 `doctor.search` 规则后返回 429。应用日志会出现：

```text
Sentinel rules loaded appointmentCreateQps=2.0 doctorSearchQps=3.0
Sentinel blocked request resource=doctor.search limitApp=default
```

Stage 4 只证明本地 Sentinel 规则可以保护预约创建和医生搜索接口，并能给出明确 HTTP 响应；不证明生产级高并发、完整熔断治理或集群规则下发。

## XXL-JOB 补偿任务

Stage 5 增加 `doctorIndexCompensationJob`，用于补偿 Elasticsearch 医生索引。MySQL 仍是事实数据源，ES 只是可删除、可重建的搜索索引；补偿任务会从 MySQL 读取医生主数据并全量重建 `doctors` 索引。

当前提供两种入口：

```text
@XxlJob("doctorIndexCompensationJob")
POST /api/jobs/doctor-index-compensation/run
```

`app.xxl-job.enabled=false` 是默认值，表示本地学习时不连接 XXL-JOB Admin；如需接入真实 Admin，可开启该开关并配置 `admin-addresses`、`app-name`、`ip`、`port`、`access-token` 等参数。本阶段验证的是补偿任务代码、手工触发入口和故障恢复链路，不声明线上调度经验。

本地演练：

```powershell
docker compose up -d
mvn spring-boot:run

Invoke-RestMethod -Method Delete -Uri http://localhost:9200/doctors
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/jobs/doctor-index-compensation/run
Invoke-RestMethod "http://localhost:8081/api/search/doctors?keyword=Neurology&page=0&size=10"
```

补偿接口预期返回：

```json
{
  "jobName": "doctorIndexCompensationJob",
  "status": "COMPLETED",
  "indexedCount": 4,
  "handledAt": "2026-06-09T09:02:22.355785500Z"
}
```

删除 ES 索引后再次触发补偿，仍应能搜索到 `Wang Fang / Neurology`。这证明索引可从 MySQL 恢复；不证明 XXL-JOB Admin 高可用、分片调度或生产重试治理。

## Prometheus/Grafana 观测

Stage 6 增加 Spring Boot Actuator、Micrometer Prometheus 和本地 Prometheus/Grafana Compose 配置，用于观察真实业务边界，而不是只展示一个空面板。

本地启动：

```powershell
docker compose up -d
mvn spring-boot:run
```

关键入口：

```text
Spring Boot metrics: http://localhost:8081/actuator/prometheus
Prometheus:          http://localhost:9090
Grafana:             http://localhost:3000  admin/admin
```

Prometheus 默认抓取 `host.docker.internal:8081/actuator/prometheus`。如果应用不是运行在宿主机 8081 端口，需要同步修改 `prometheus.yml`。

当前业务指标：

```text
appointment_events_published_total{result="success|failed"}
notification_consumption_total{result="sent|duplicate|failed"}
compensation_runs_total{job="doctorIndexCompensationJob",result="completed|failed"}
compensation_duration_seconds_count
sentinel_blocks_total{resource="doctor.search"}
http_server_requests_seconds_count
```

一次观测演练：

```powershell
$body = '{"patientId":9101,"doctorId":2,"appointmentTime":"2026-06-10T10:00:00Z"}'
$appointment = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/appointments -ContentType application/json -Body $body
Start-Sleep -Seconds 5
Invoke-RestMethod http://localhost:8081/api/appointments/$($appointment.id)/notifications

Invoke-RestMethod -Method Delete -Uri http://localhost:9200/doctors
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/jobs/doctor-index-compensation/run

1..5 | ForEach-Object {
  curl.exe -s -o NUL -w "%{http_code}`n" "http://localhost:8081/api/search/doctors?keyword=&page=0&size=1"
}

Invoke-WebRequest -UseBasicParsing http://localhost:8081/actuator/prometheus
Invoke-RestMethod "http://localhost:9090/api/v1/query?query=sentinel_blocks_total"
```

实测可以看到 `appointment_events_published_total=1`、`notification_consumption_total{result="sent"}=1`、`compensation_runs_total{result="completed"}=1`、`sentinel_blocks_total{resource="doctor.search"}=2`，并且 Prometheus target 为 `up`。本阶段只证明本地指标暴露和抓取，不证明生产监控体系、告警治理、容量评估或 P95/P99 结论。
