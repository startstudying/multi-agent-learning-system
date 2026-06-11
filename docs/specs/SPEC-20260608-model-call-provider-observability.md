# SPEC-20260608 模型调用 provider 持久化观测

## 范围

本规格定义 P3-3-B：`model_call_log.provider` schema / entity / recorder / gateway normalization 的最小生产化切片。

不改变外部 API，不接真实 provider，不新增依赖。

## 数据库变更

新增 Flyway migration：

```text
backend/src/main/resources/db/migration/V18__model_call_provider_observability.sql
```

新增列：

```sql
ALTER TABLE model_call_log ADD COLUMN provider varchar(80) NOT NULL DEFAULT 'none'
```

要求：

- 使用现有 `add_column_if_missing` procedure 风格，支持重复收敛。
- 历史数据默认 `none`。
- 本切片不新增索引；后续高频 provider 查询再按实际查询计划补 `(provider, status, created_at)`。

## Domain Model

`ModelCallLog` 新增：

```java
@Column(nullable = false, length = 80)
private String provider = "none";
```

`@PrePersist` 兜底：

- null / blank -> `none`
- 非空由 recorder / gateway 先归一化

## Provider 归一化规则

允许持久化和 metrics tag 的 provider 值：

```text
none
openai
dashscope
anthropic
gemini
mock
other
```

规则：

- null / blank -> `none`
- trim + lowercase 后命中白名单 -> 白名单值
- 其他全部 -> `other`

禁止落库：

- raw base URL
- deployment URL / endpoint path
- tenant / region / account ID
- API key / token / `sk-` 片段
- provider raw requestId
- raw exception message

## 成功日志语义

`AgentRunRecorder.recordSuccessfulModelEvidence(context, trace, modelResponse)`：

- `provider` 使用 `modelResponse.provider()` 经安全归一化后的值。
- `model` 使用 `modelResponse.model()`。
- `latencyMs` 使用 `modelResponse.latencyMs()`。
- token / cost 使用 `modelResponse.tokenUsage()`。
- prompt metadata 继续由 prompt version 白名单生成。

旧 overload `recordSuccessfulModelEvidence(context, trace, TokenUsageEstimate)` 没有 provider 来源，写 `none`。

## 失败日志语义

`AiModelGateway.generateStructuredWithRetry(...)` 在重试耗尽时：

- 使用 gateway 当前 normalized provider 传给 recorder。
- 不从 raw exception message 提取 provider。
- metrics 与 DB provider 使用同一低基数口径。

`AgentRunRecorder.recordFailure(...)`：

- 保留旧方法签名，默认 provider 为 `none`。
- 新增 provider overload，供 gateway failure path 调用。
- `model_call_log.provider` 写安全 provider。
- `model_call_log.errorMessage` 仍只写安全错误码。

## 安全要求

- 不保存 raw provider error。
- 不保存 raw prompt、student answer、RAG chunk、malformed output。
- 不保存 secret、token、Authorization、cookie。
- 不扩大 analytics API 权限。
- 不新增依赖；后续真实 provider adapter 必须单独做 dependency review。

## 测试策略

| Test | Purpose |
|---|---|
| `SchemaConvergenceMigrationTest` | V18 migration 文本包含 provider schema。 |
| `MysqlMigrationSmokeTest` | 真实 MySQL 从空 schema 迁移到 V18，验证 provider 列存在且类型正确。 |
| `AiModelGatewayTest` | 未知 provider 归一化为 `other`，failure recorder 使用安全 provider。 |
| `AgentRunRecorderTest` | 成功/失败 model call log 写 provider，敏感 provider 归一化。 |
| `ResourceGenerationControllerTest` | 资源生成成功链路持久化 gateway provider。 |

## 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 不接触 provider；Service/Gateway/Recorder 分层不变。 |
| Frontend rules | PASS | 不改 frontend。 |
| Agent / RAG rules | PASS | Agent 执行仍写 trace/model log/token log。 |
| Security | PASS | provider 低基数归一化，raw error 不落库。 |
| API / Database | PASS | DB schema 已在 SPEC 中定义，API contract 不变。 |

