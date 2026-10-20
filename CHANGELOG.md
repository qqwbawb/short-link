# 更新日志

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.1.0] - 2026-10-20

### 新增

- 跳转时异步记录访问日志（IP、User-Agent、访问时间）到 `access_log` 表，基于 `@Async` + `@EnableAsync`，不阻塞跳转主流程。
- 点击计数：跳转时 Redis `INCR shortLink:count:{code}` 计数，`ClickCountSyncTask` 定时任务（`@Scheduled`，每 10 秒）批量同步累加到数据库 `click_count` 字段。
- 缓存穿透防护：短码不存在时写入空值缓存（TTL 60 秒），避免不存在的短码反复穿透数据库。
- 启动类开启 `@EnableScheduling`，支持定时任务调度。

### 修复

- 修正缓存命中判断：命中空值缓存时返回 404（短链不存在），不再误跳转。

## [1.0.0] - 2026-10-18

### 新增

- Spring Boot 2.7.18（Java 8）基础工程，集成 MyBatis-Plus 3.4.1、Druid 1.2.1、MySQL 8.0.33 驱动。
- 创建短链接口 `POST /api/link`：原链查重，Redis 自增序列生成 62 进制短码，写入 MySQL。
- 短链跳转接口 `GET /s/{code}`：Redis 缓存优先，未命中查 MySQL 并回写缓存（TTL 1 小时），302 重定向；短码不存在返回 404。
- `RedisConfig`：key 使用 String 序列化，value 使用 Jackson JSON 序列化。
- 统一响应体 `ResultVo<T>` 与 `ResultUtils` 工具类，状态码常量 `StatusCode`（200 / 500）。
- 62 进制短码生成器 `ShortCodeGenerator`（`0-9`、`a-z`、`A-Z`）。
- IDEA HTTP Client 测试脚本 `src/test/resources/http/api-test.http`，覆盖创建、重复创建、跳转缓存验证、404 等用例。

### 性能优化

- 跳转读取引入 Redis 缓存后，本地同环境（每组 32 次）对比直查 MySQL：
  - 平均耗时 17.82 ms → 15.55 ms（↓ 12.74%）；
  - 中位数 17.78 ms → 15.62 ms（↓ 12.15%）；
  - 最小值 16.12 ms → 14.24 ms（↓ 11.66%）；
  - 最大值 22.28 ms → 17.03 ms（↓ 23.56%），响应波动收窄。

### 已知限制

- 分布式锁、过期清理、原链合法性校验尚未实现。
