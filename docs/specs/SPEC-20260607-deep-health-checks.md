# SPEC - 深度健康检查
状态：已完成（2026-06-07）

## 1. 设计目标

在不新增依赖、不改数据库、不改 API 路径的前提下，把 `/api/health` 从浅配置检查升级为最小深度健康检查。健康检查用于运行诊断，不用于暴露内部拓扑或凭据。

## 2. 响应模型

沿用：

```text
ApiResponse<HealthDtos.HealthResponse>
```

`ComponentStatus` 继续包含：

- `status`
- `detail`
- `metadata`

新增稳定状态：

- `UP`
- `DOWN`
- `CONFIGURED`
- `UNCONFIGURED`
- `DISABLED`

metadata 只允许布尔值、稳定错误分类、能力标志，不允许原始配置值。

## 3. 组件语义

### 3.1 Application

- 始终返回 `UP`。
- metadata 可包含 `environment`，但不得包含 host、path、secret。

### 3.2 Database

- 使用 `DataSource` 获取连接并执行 `Connection#isValid(timeoutSeconds)`。
- 成功返回 `UP`。
- 失败返回 `DOWN`，detail 使用 `database connection check failed`。
- metadata 只包含：
  - `configured`
  - `checked`
  - `errorCode`

### 3.3 Redis

- 使用 `RedisConnectionFactory` 建立连接并执行 `ping()`。
- 未配置 host 返回 `UNCONFIGURED`。
- 成功返回 `UP`。
- 失败返回 `DOWN`。
- metadata 只包含：
  - `configured`
  - `checked`
  - `errorCode`

### 3.4 MinIO

- 不做对象读写。
- 配置完整时尝试构造 `MinioClient`，构造成功返回 `CONFIGURED`。
- 配置不完整返回 `UNCONFIGURED`。
- 构造失败返回 `DOWN`。
- metadata 只包含：
  - `configured`
  - `checked`
  - `errorCode`

### 3.5 Model Provider

- `provider=none` 返回 `DISABLED`。
- provider 非 `none` 且 `chatModel` 或 `embeddingModel` 有值时返回 `CONFIGURED`。
- provider 非 `none` 但模型名缺失时返回 `UNCONFIGURED`。
- 本切片不发起模型 API 调用。
- metadata 只包含：
  - `configured`
  - `providerConfigured`
  - `chatModelConfigured`
  - `embeddingModelConfigured`

## 4. 安全边界

禁止响应出现：

- JDBC URL、数据库用户名、密码。
- Redis host、port、username、password。
- MinIO endpoint、bucket、access key、secret key。
- 模型 API key、token、base URL、deployment URL。
- 原始 exception message。

## 5. 文件边界

计划修改：

- `backend/src/main/java/com/learningos/health/application/HealthService.java`
- `backend/src/main/java/com/learningos/health/api/HealthDtos.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/learningos/health/api/HealthControllerTest.java`
- `backend/src/test/java/com/learningos/health/application/HealthServiceTest.java`

计划更新文档：

- `docs/product/PRD-20260607-deep-health-checks.md`
- `docs/requirements/REQ-20260607-deep-health-checks.md`
- `docs/specs/SPEC-20260607-deep-health-checks.md`
- `docs/plans/PLAN-20260607-deep-health-checks.md`
- `docs/tasks/TASK-20260607-deep-health-checks.md`
- `docs/context/CONTEXT-20260607-deep-health-checks.md`
- `docs/subagents/runs/RUN-20260607-deep-health-checks.md`

## 6. 测试策略

- `HealthControllerTest`
  - 正常配置下返回安全 shape。
  - 数据库探测成功时返回 `UP`。
  - 数据库探测失败时返回 `DOWN`，但接口仍返回 200。
  - Redis 连接不可用时返回 `DOWN`，但接口仍返回 200。
  - MinIO 配置完整时返回 `CONFIGURED`。
  - MinIO endpoint 异常时返回 `DOWN`，但接口仍返回 200。
  - 模型 provider `none` 返回 `DISABLED`。
  - 响应不包含敏感配置值。
- `HealthServiceTest`
  - 覆盖数据库成功、失败、无 `DataSource` bean。
  - 覆盖 Redis 成功、失败、未配置 host。
  - 覆盖 MinIO 配置不完整、endpoint 异常。
  - 覆盖模型 provider `none`、非 `none` 配置完整、非 `none` 缺少模型名。

## 7. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只委托 `HealthService` |
| Frontend rules | PASS | 不涉及 frontend |
| Agent / RAG rules | PASS | 不调用模型，不改变 RAG |
| Security | PASS | 无新增依赖，健康响应脱敏 |
| API / Database | PASS | 路径不变，不改 DB |
