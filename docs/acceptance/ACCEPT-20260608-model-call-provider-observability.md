# ACCEPT - P3-3-B 模型调用 provider 持久化观测

## 1. 验收结论

代码与文档验收通过；真实 MySQL V18 smoke 验收项受本机数据库凭据限制，状态为待补验。

本切片已完成 `model_call_log.provider` 的 schema / entity / recorder / gateway 低基数持久化闭环。失败日志仍只保存安全错误码，不保存 raw provider error、URL、API key、tenant 或高基数字符串。

## 2. 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| V18 migration 增加 `model_call_log.provider varchar(80) not null default 'none'` | PASS | `V18__model_call_provider_observability.sql`；`SchemaConvergenceMigrationTest` |
| `ModelCallLog` entity 增加 provider 字段和空值兜底 | PASS | `ModelCallLog` 字段与 `@PrePersist` |
| 成功模型调用从 `ModelResponse.provider()` 写 provider | PASS | `AgentRunRecorder.recordSuccessfulModelEvidence(..., ModelResponse)`；`AgentRunRecorderTest` |
| 失败模型调用支持 provider overload，旧签名兼容默认 `none` | PASS | `AgentRunRecorder.recordFailure(...)` overload；`AgentRunRecorderTest` |
| provider 归一化为 `none/openai/dashscope/anthropic/gemini/mock/other` | PASS | `AiModelGateway.normalizeProvider()`；`AgentRunRecorder.safeProvider(...)` |
| URL / `apiKey` / `sk-` 等敏感 provider 配置只记录 `other` | PASS | `AiModelGatewayTest`、`AgentRunRecorderTest` |
| `model_call_log.errorMessage` 仍只保存安全错误码 | PASS | `AiModelGatewayTest`、`AgentRunRecorderTest`、`ResourceGenerationControllerTest` |
| 不新增依赖、不修改前端、不接真实 provider、不新增 API | PASS | 未修改 `backend/pom.xml`、`frontend/**`；静态边界检查无 SDK 直连 |
| 聚焦、相邻回归、完整后端测试 | PASS | 51 tests / 306 tests 均 0 failure |
| 真实 MySQL V18 smoke | BLOCKED | 本机 3306 MySQL `root` 凭据不可用，`Access denied`；需环境恢复后补跑 |
| Evidence / Acceptance / Memory / Changelog / Retrospective / Skill 更新 | PASS | 本验收文档、Evidence、Memory、Changelog、Retro、`model-gateway-boundary` skill |

## 3. 测试验收

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=SchemaConvergenceMigrationTest,AgentRunRecorderTest,AiModelGatewayTest,ResourceGenerationControllerTest test` | BUILD SUCCESS；Tests run: 51, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn test` | BUILD SUCCESS；Tests run: 306, Failures: 0, Errors: 0, Skipped: 1 |
| `mvn --% -Dtest=MysqlMigrationSmokeTest -Dlearningos.mysql.smoke=true test` | BUILD FAILURE；环境凭据问题：`Access denied for user 'root'@'localhost'` |

## 4. Open Items

- 环境可用后补跑真实 MySQL 8 smoke，确认 Flyway V1-V18 空库迁移。
- P3-3-C：真实 Spring AI / Spring AI Alibaba Chat provider adapter 接入，需 dependency review / security review / 官方文档核验。
- 如后续 provider 聚合查询成为高频路径，再按查询计划评估 `(provider, status, created_at)` 索引。
