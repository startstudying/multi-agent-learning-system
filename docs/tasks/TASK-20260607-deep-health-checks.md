# TASK - 深度健康检查
状态：已完成（2026-06-07）

## Task 1

补齐 P3-5 深度健康检查：数据库、Redis、MinIO、模型 provider。

### 目标

- `/api/health` 继续提供统一 envelope。
- 组件健康状态比浅配置态更有诊断价值。
- 健康响应继续严格脱敏。
- 不新增依赖、不改 DB、不改 API 路径。

### 交付物

- 数据库轻量连接探测。
- Redis ping 探测。
- MinIO 配置完整性和客户端构造探测。
- 模型 provider 禁用/配置态判断。
- 健康 DTO 状态补齐。
- 测试、证据、验收和文档更新。

### Done Criteria

- [x] database 成功探测返回 `UP`。
- [x] database 失败返回 `DOWN` 且不抛 500。
- [x] redis 连接失败返回 `DOWN` 且不抛 500。
- [x] redis 未配置 host 返回 `UNCONFIGURED` 且不尝试连接。
- [x] minio 配置完整时返回 `CONFIGURED`。
- [x] minio endpoint 明显异常时返回 `DOWN`。
- [x] model provider 为 `none` 时返回 `DISABLED`。
- [x] model provider 非 `none` 且模型名存在时返回 `CONFIGURED`，缺少模型名时返回 `UNCONFIGURED`。
- [x] 响应不包含 JDBC URL、Redis host、MinIO endpoint/bucket/key、secret、token、password。
- [x] 不新增 Maven dependency。
- [x] 定向测试通过。
- [x] 全量 `mvn test` 通过。

## 验证记录

| 命令 | 结果 |
|---|---|
| `cd backend && mvn --% -Dtest=HealthServiceTest,HealthControllerTest test` | 通过，13 tests，0 failures，0 errors |
| `cd backend && mvn --% -Dtest=HealthServiceTest,HealthControllerTest,StructuredRequestLoggingFilterTest test` | 通过，18 tests，0 failures，0 errors |
| `cd backend && mvn test` | 通过，256 tests，0 failures，0 errors，1 skipped |
