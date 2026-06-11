# EVIDENCE - 深度健康检查
状态：已完成（2026-06-07）

## 1. 验证范围

P3-5 深度健康检查切片：`/api/health` 对数据库、Redis、MinIO、模型 provider 的最小深度状态判断，以及健康响应脱敏。

## 2. 验证结果

| 命令 | 结果 |
|---|---|
| `cd backend && mvn --% -Dtest=HealthServiceTest,HealthControllerTest test` | 通过，13 tests，0 failures，0 errors |
| `cd backend && mvn --% -Dtest=HealthServiceTest,HealthControllerTest,StructuredRequestLoggingFilterTest test` | 通过，18 tests，0 failures，0 errors |
| `cd backend && mvn test` | 通过，256 tests，0 failures，0 errors，1 skipped |

## 3. 行为证据

- `HealthService` 使用实际 `DataSource` 执行 `Connection#isValid(1)`，成功返回 `UP`，连接不可用、连接无效或无 `DataSource` bean 时返回 `DOWN`。
- Redis 未配置 host 时返回 `UNCONFIGURED`，配置存在时执行 `ping()`，失败返回 `DOWN`，不会让 `/api/health` 返回 500。
- MinIO 只做配置完整性、endpoint 结构校验和 client 构造，不做 bucket/object I/O。
- 模型 provider 为 `none` 时返回 `DISABLED`；provider 非 `none` 且存在 chat 或 embedding model 时返回 `CONFIGURED`；缺少模型名时返回 `UNCONFIGURED`。
- `HealthControllerTest` 覆盖数据库 `UP`、数据库 `DOWN`、Redis `DOWN`、MinIO `CONFIGURED`、MinIO `DOWN`、模型 `DISABLED` 的 HTTP envelope 路径。
- `HealthServiceTest` 覆盖数据库失败、无数据库 bean、Redis 成功/失败/未配置、MinIO 不完整/异常、模型配置分支。

## 4. 安全证据

- 健康响应只输出稳定状态、布尔 metadata 和固定 `errorCode`，不输出原始异常消息。
- 测试断言响应不包含 JDBC URL、Redis host/password、MinIO endpoint/bucket/access key/secret key、token、password。
- `application.yml` 将 `spring.data.redis.host` 默认值改为空，未显式配置 Redis 时不再误判为已配置。
- `powershell -File C:\Users\wonderful\.codex\skills\java-security-review\scripts\scan-mybatis-dollar.ps1 -ScanDir backend` 可执行，结果只命中两个 `@Scheduled(fixedDelayString = "...")` 配置占位符，不是 MyBatis SQL `${}` 注入。
- `find-hardcoded-secrets.ps1` 因脚本自身字符串引号解析错误无法执行；本次以健康检查相关文件定向 `rg` 和测试脱敏断言补偿，未发现健康响应泄露敏感配置。

## 5. 专家审查证据

- Backend/Architecture Expert 建议保持 `/api/health` 路径、不新增依赖，DB 用 `Connection#isValid(1)`，Redis 用 `ping()`，MinIO 仅 client 构造，model 仅配置态。
- Security Reviewer 建议固定错误码、禁止 raw exception、约束同步探测成本，并去除危险默认 secret。
- Verifier 指出闭环文档和分支覆盖不足，已补齐 Evidence/Acceptance/Retro 和服务层分支测试。
- Code Reviewer 指出数据库不应通过 `DataSourceProperties.url` 进入 `UNCONFIGURED` 分支，已改为基于实际 `DataSource` 探测，无 `DataSource` 时返回 `DOWN`。

## 6. 限制

- 本切片不做 Redis 写入探测。
- 本切片不做 MinIO bucket/object I/O。
- 本切片不调用真实模型 provider。
- 告警、Dashboard、Prometheus/Grafana、慢查询和慢模型调用告警仍在 P3-5 后续项。
