# Redis 缓存旁路最小 Demo

## 场景

商品查询接口使用缓存旁路模式：

```text
GET /api/products/1
  -> 查询 Redis
  -> 未命中时查询模拟数据库
  -> 写入 Redis，TTL 为 5 分钟
  -> 返回商品
```

更新商品时先保存数据，再删除对应缓存：

```text
PUT /api/products/1
  -> 更新模拟数据库
  -> 删除 Redis 缓存
```

## 运行

需要 Java 17、Maven 和已启动的 Docker Desktop。

```powershell
docker compose up -d
mvn spring-boot:run
```

打开另一个终端执行：

```powershell
Invoke-RestMethod http://localhost:8080/api/products/1
docker compose exec redis redis-cli GET demo:product:1
docker compose exec redis redis-cli TTL demo:product:1
```

更新商品并再次查询：

```powershell
$body = '{"name":"Redis Practice Book","price":59.90}'
Invoke-RestMethod -Method Put -Uri http://localhost:8080/api/products/1 -ContentType application/json -Body $body
docker compose exec redis redis-cli GET demo:product:1
Invoke-RestMethod http://localhost:8080/api/products/1
```

`PUT` 后第一次 `GET` Redis 应返回空值；随后再次查询接口，Redis 中会出现新数据。

## 自动化验证

单元测试不依赖正在运行的 Redis：

```powershell
mvn test
```

## 阅读顺序

1. `ProductService`：缓存旁路模式的核心。
2. `RedisProductCache`：Redis key、JSON 序列化和 TTL。
3. `ProductController`：用于手工验证的 HTTP 接口。
4. `ProductServiceTest`：四个最小行为测试。

## 下一步练习

1. 用 MySQL 替换内存 Map，讨论数据库与缓存的一致性边界。
2. 在有明确压测工具和机器规格后，再记录吞吐量、P95/P99 和错误率。

## 缓存风险补强

当前 Demo 已补充三类常见缓存风险的最小处理：

```text
缓存穿透：不存在的商品写入短 TTL 空值 __MISSING__
缓存击穿：同一个商品 ID 回源时使用本地互斥锁，避免并发重复查库
缓存雪崩：正常商品缓存 TTL 在基础 TTL 上增加随机抖动
```

配置：

```yaml
demo:
  cache:
    product-ttl: 5m
    product-missing-ttl: 30s
    product-ttl-jitter: 30s
```

本地验证：

```powershell
docker compose up -d
mvn spring-boot:run

Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/products/99
docker compose exec redis redis-cli GET demo:product:99
docker compose exec redis redis-cli TTL demo:product:99

Invoke-RestMethod http://localhost:8080/api/products/1
docker compose exec redis redis-cli TTL demo:product:1
```

实测结果：

```text
GET /api/products/99 => HTTP 404
GET demo:product:99 => __MISSING__
TTL demo:product:99 => 17

GET /api/products/1 => {"id":1,"name":"Java Middleware Handbook","price":49.90}
TTL demo:product:1 => 315
```

这说明不存在商品会被短 TTL 空值拦截，正常商品 TTL 会落在 300-330 秒之间。本地互斥锁只适合单实例 Demo；多实例服务需要 Redis 分布式锁、逻辑过期或异步刷新等方案继续演进。
