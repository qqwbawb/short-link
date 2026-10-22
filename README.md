# short-link

基于 Spring Boot + MyBatis-Plus + Redis 的短链服务：原链转短码、302 跳转，跳转读取优先走 Redis 缓存。

## 技术栈

| 组件 | 版本 / 说明 |
| --- | --- |
| Spring Boot | 2.7.18（Java 8） |
| MyBatis-Plus | 3.4.1 |
| Druid | 1.2.1（数据源连接池） |
| MySQL | 8.0.33 驱动 |
| Redis | spring-boot-starter-data-redis（Lettuce + commons-pool2 连接池） |
| Validation | spring-boot-starter-validation（参数校验） |
| Lombok | 简化实体代码 |

## 功能

- **创建短链** `POST /api/link`：原链查重（同一原链不重复生成，重复则抛业务异常），Redis 自增序列生成 62 进制短码，写入 MySQL；支持可选 `expireDay` 设置过期天数。
- **参数校验**：创建接口通过 Bean Validation 校验（原链非空、长度 ≤ 2048、必须以 http/https 开头），校验失败返回 400 及具体原因。
- **全局异常处理**：`@RestControllerAdvice` 统一处理业务异常（BizException → 500）、参数校验异常（→ 400）、未捕获异常（→ 500），响应体统一为 `ResultVo`。
- **短链跳转** `GET /s/{code}`：先查 Redis 缓存，未命中再查 MySQL 并回写缓存（TTL 1 小时），命中后 302 重定向到原链；短码不存在返回 404。
- **过期控制**：创建时可指定过期天数，跳转时校验 `expireTime`，过期短链按不存在处理（返回 404 并写入空值缓存）。
- **缓存穿透防护**：短码不存在（或已过期）时向 Redis 写入空值缓存（TTL 60 秒），避免恶意短码持续打穿数据库。
- **异步访问日志**：跳转时记录 IP、User-Agent、访问时间，`@Async` 异步写入 `access_log`，不阻塞主流程。
- **点击计数同步**：跳转时 Redis `INCR` 计数（key：`shortLink:count:{code}`），定时任务每 5 分钟批量落库到 `click_count`。
- **查询短链详情** `GET /api/link/{code}`：返回短链信息（含完整短链地址）。
- **查询短链统计** `GET /api/link/{code}/stats`：返回点击量、创建/过期时间及最近 10 条访问日志。
- **统一响应体** `ResultVo<T>`（msg / code / data）。
- **Redis 序列化**：key 使用 String 序列化，value 使用 Jackson JSON 序列化，避免 CLI 查看乱码。

## 项目结构

```
short-link
├── pom.xml
└── src/main
    ├── java/com/xs
    │   ├── ShortLinkApplication.java        # 启动类
    │   ├── common
    │   │   ├── exception                    # BizException + GlobalExceptionHandler
    │   │   ├── status/StatusCode.java       # 200 / 500 状态码常量
    │   │   └── utils                       # ResultVo / ResultUtils 统一响应
    │   ├── config
    │   │   ├── MybatisPlusConfig.java
    │   │   └── RedisConfig.java            # RedisTemplate 序列化配置
    │   ├── controller/ShortLinkController.java
    │   ├── dto                             # CreateLinkRequest / LinkStatsVo
    │   ├── entity                          # ShortLink / AccessLog
    │   ├── mapper                         # ShortLinkMapper / AccessLogMapper
    │   ├── service                         # 业务接口与实现（缓存读写、访问日志落库）
    │   └── utils
    │       ├── ShortCodeGenerator.java     # 62 进制短码生成器
    │       └── ClickCountSyncTask.java     # 点击计数定时同步任务（每 5 分钟）
    └── resources
        ├── application.yml
        └── mapper/ShortLinkMapper.xml
```

## 快速开始

### 环境要求

- JDK 8+、Maven 3+
- MySQL 8.x（库名 `short_link`）
- Redis 6.x（默认本地 6379，无密码）

### 1. 建库建表（参考，按实体字段推断，可按需调整）

```sql
CREATE DATABASE IF NOT EXISTS short_link DEFAULT CHARSET utf8mb4;

CREATE TABLE short_link (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    short_code  VARCHAR(16)  NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    click_count BIGINT       DEFAULT 0,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    expire_time DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_short_code (short_code)
);

CREATE TABLE access_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    short_code  VARCHAR(16) NOT NULL,
    ip          VARCHAR(64),
    user_agent  VARCHAR(512),
    access_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
```

### 2. 修改配置

编辑 `src/main/resources/application.yml`，按本地环境填写 MySQL 账号密码、Redis 地址：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/short_link?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8
    username: root
    password: root
  redis:
    host: localhost
    port: 6379
    database: 0
```

### 3. 启动

```bash
mvn spring-boot:run
```

服务默认端口 **8090**。

## 接口说明

### 创建短链

```
POST http://localhost:8090/api/link
Content-Type: application/json
```

请求体：

```json
{
  "originalUrl": "https://www.baidu.com",
  "expireDay": 30
}
```

- `originalUrl` 必填：非空、长度 ≤ 2048、必须以 `http://` 或 `https://` 开头，不合法返回 `code=400` 及具体原因。
- `expireDay` 可选：过期天数，> 0 时写入 `expireTime = createTime + expireDay`。

响应：

```json
{
  "msg": "生成成功",
  "code": 200,
  "data": {
    "shortCode": "2",
    "shortLink": "http://localhost:8090/s/2",
    "originalUrl": "https://www.baidu.com",
    "clickCount": 0,
    "createTime": "2026-10-18T10:00:00",
    "expireTime": "2026-11-17T10:00:00"
  }
}
```

- 同一 `originalUrl` 已存在短链时抛业务异常，返回 `code=500`，msg：`已有短链`。

### 短链跳转

```
GET http://localhost:8090/s/{code}
```

- 命中缓存或数据库：`302` 重定向到原链。
- 短码不存在或已过期：`404`，响应体 `短链不存在`。

### 查询短链详情

```
GET http://localhost:8090/api/link/{code}
```

- 返回短链信息（含完整短链地址）；短码不存在时返回 `code=500`，msg：`短链不存在`。

### 查询短链统计

```
GET http://localhost:8090/api/link/{code}/stats
```

- 返回短链基础信息 + 最近 10 条访问日志（按访问时间倒序）；短码不存在时返回 `code=500`，msg：`短链不存在`。

完整可执行测试脚本见 `src/test/resources/http/api-test.http`（IDEA HTTP Client 直接运行）。

## 核心设计

### 短码生成（62 进制）

`ShortCodeGenerator` 通过 Redis `INCR shortLink:seq` 获取全局递增序号，再按 62 进制（`0-9`、`a-z`、`A-Z`）编码为短码，避免数据库自增 ID 直接暴露。

### 缓存读写（读穿透 + 空值防护 + 过期校验 + 回写）

跳转流程：

1. 查 Redis（key：`shortLink:{code}`，value：原链，TTL 1 小时）；
2. 命中直接返回并 `INCR shortLink:count:{code}` 计数，无 SQL；
3. 未命中查 MySQL，先校验 `expireTime` 是否过期：已过期则写入空值缓存（TTL 60 秒）并返回 `null`（404）；未过期则回写 Redis 缓存，同样计数；
4. 查不到则写入空值缓存（TTL 60 秒）并返回 `null`，控制器回 404，后续相同短码直接命中空值，不穿透数据库。

### 点击计数同步

跳转产生的计数先落在 Redis（避免每次请求写库），`ClickCountSyncTask` 每 5 分钟扫描 `shortLink:count:*`，读取后删除 key，把增量累加到数据库 `click_count` 字段。计数先走 Redis 再异步落库，与访问日志一样不阻塞跳转主流程。

### 全局异常处理

`GlobalExceptionHandler`（`@RestControllerAdvice`）统一收敛异常：

| 异常 | code | 响应 msg |
| --- | --- | --- |
| `BizException`（业务异常，如重复创建、短链不存在） | 500 | 业务提示语 |
| 参数校验异常（`MethodArgumentNotValidException` / `BindException`） | 400 | 具体校验信息 |
| 未捕获异常 | 500 | 系统繁忙，请稍后重试 |

## 性能对比（本地同环境）

本地 localhost（MySQL + Redis），每组 32 次测量，PowerShell `Measure-Object` 统计：

| 指标 | 无 Redis（直查 MySQL） | 有 Redis 缓存 | 优化幅度 |
| --- | ---: | ---: | ---: |
| 平均耗时 | 17.82 ms | 15.55 ms | ↓ 12.74% |
| 中位数 | 17.78 ms | 15.62 ms | ↓ 12.15% |
| 最小值 | 16.12 ms | 14.24 ms | ↓ 11.66% |
| 最大值（峰值抖动） | 22.28 ms | 17.03 ms | ↓ 23.56% |

说明：以上为本地开发环境同条件对比，仅用于验证缓存效果，不代表生产 QPS；缓存的主要价值是降低数据库读压力、削峰和收窄响应波动。

## 当前局限 / 待完善

- 分布式锁（防重复提交）代码已注释，未启用。
- 过期短链在跳转时会被拒绝（404），但过期数据未定期清理删除。
- 短码冲突未做重试处理。
- 定时任务使用 `keys` 扫描计数前缀，数据量大时可考虑改用 scan 或计数单独存储。
