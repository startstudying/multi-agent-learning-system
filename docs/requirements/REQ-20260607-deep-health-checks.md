# REQ - 深度健康检查
状态：已完成（2026-06-07）

## 1. 功能需求

1. `/api/health` 必须继续返回统一 `ApiResponse` envelope。
2. 响应必须包含 `application`、`database`、`redis`、`minio`、`model`。
3. `application.status` 必须为 `UP`。
4. 数据库健康必须执行轻量连接探测，成功返回 `UP`，失败返回 `DOWN`。
5. Redis 健康必须在未配置 host 时返回 `UNCONFIGURED`。
6. Redis 健康必须在连接成功时返回 `UP`，连接失败时返回 `DOWN`。
7. MinIO 健康必须在配置完整且客户端可构造时返回 `CONFIGURED`。
8. MinIO 健康必须在配置不完整时返回 `UNCONFIGURED`。
9. 模型 provider 为 `none` 时必须返回 `DISABLED`。
10. 模型 provider 非 `none` 且 chat 或 embedding model 配置存在时必须返回 `CONFIGURED`。
11. 健康检查失败不得导致 `/api/health` 返回 500，除非 controller 本身不可用。

## 2. 安全需求

1. 响应不得包含 JDBC URL。
2. 响应不得包含 Redis host、port、username、password。
3. 响应不得包含 MinIO endpoint、bucket、access key、secret key。
4. 响应不得包含模型 provider key、base URL、token、secret、完整模型部署 endpoint。
5. 失败详情只能使用稳定错误分类，例如 `CONNECTION_FAILED`、`CONFIG_INCOMPLETE`、`CLIENT_BUILD_FAILED`。
6. 健康探测目标必须来自服务端配置，不能由请求参数控制。
7. 本切片不得新增 actuator 高风险 endpoint。

## 3. 约束

- 不新增 Maven dependency。
- 不新增数据库表、字段或 migration。
- 不修改 `/api/health` 路径。
- 不引入外部网络调用模型 provider。
- 不向 Redis 写入数据。
- 不向 MinIO 读写对象。

## 4. 验收条件

- 健康检查测试覆盖成功与失败状态。
- 测试断言响应中不含敏感配置值。
- `docs/planning/backend-architecture-todolist.md` P3-5 深度健康检查项更新。
- Evidence 和 Acceptance 记录验证命令与结果。
