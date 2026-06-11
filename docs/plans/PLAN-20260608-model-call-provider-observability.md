# PLAN-20260608 模型调用 provider 持久化观测

## Status

Completed。

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 执行 Spec-first 全流程。 |
| spring-ai-agent-backend | 修改 Agent/model-call 后端服务边界。 |
| agent-trace-governance | 涉及 `model_call_log` 和模型调用审计。 |
| model-gateway-boundary | provider 观测必须遵守模型网关与脱敏边界。 |
| database-design | 新增 Flyway V18 schema 字段。 |
| security-review | provider 低基数与敏感信息防泄露。 |
| test-driven-development | 先写失败测试再实现。 |
| verification-before-completion | 完成前必须提供新鲜验证证据。 |
| Confidence Check | 实施前确认方向、重复实现和风险。 |

Missing skills：无。

GitHub Research Needed：No。本切片不新增依赖、不接外部 SDK。

New project-specific skill：不新增；更新既有 `model-gateway-boundary` 即可。

## Subagent Decision

Use Subagents：Yes。

Reason：任务影响数据库 schema、后端 Agent/model-call 持久化、测试与安全观测；已完成 DB / Security / Test 三个只读专家分析。

Parallelism Level：L1。

Implementation Mode：主 Codex 串行实现，不并行修改同一文件。

参考报告：

- `docs/subagents/runs/RUN-20260608-backend-p3-productionization-model-gateway.md`
- `docs/subagents/runs/RUN-20260608-model-call-provider-observability-integration.md`

## Confidence Check

| Check | Status | Evidence |
|---|---|---|
| No duplicate implementations | PASS | `ModelResponse.provider` 已存在，但 `model_call_log` schema/entity/recorder 未实现 provider 持久化。 |
| Architecture compliance | PASS | 继续保持业务服务 -> `AiModelGateway` -> `AgentRunRecorder`，不让 Controller 或前端接触 provider SDK。 |
| Official docs verified | N/A/PASS | 不使用外部 SDK/API，不新增依赖；真实 provider 后续单独查官方文档。 |
| OSS implementations referenced | N/A/PASS | 仅项目内 schema/recorder 变更，不引入 OSS 模式。 |
| Root cause identified | PASS | 持久化审计缺 `provider`；成功/失败 recorder 未写 provider；provider 需低基数安全归一化。 |

Confidence：0.95，可进入 TDD 实现。

## 执行计划

1. [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
2. [x] TDD RED：新增 migration / recorder / gateway provider 断言并确认失败。
3. [x] GREEN：新增 V18 migration、`ModelCallLog.provider`、recorder 成功/失败 provider 写入、gateway provider 归一化。
4. [x] 运行聚焦测试：`SchemaConvergenceMigrationTest,AgentRunRecorderTest,AiModelGatewayTest`。
5. [x] 运行相邻回归：增加 `ResourceGenerationControllerTest`。
6. [x] 运行 MySQL smoke（已执行；本机 MySQL `root` 凭据不可用，真实 V18 smoke 待环境恢复后补跑）。
7. [x] 运行 `mvn test`。
8. [x] 更新 Evidence / Acceptance / Changelog / Memory / Retro / Skill。

## 验证结果

| Command | Result |
|---|---|
| `mvn --% -Dtest=SchemaConvergenceMigrationTest,AgentRunRecorderTest,AiModelGatewayTest,ResourceGenerationControllerTest test` | BUILD SUCCESS；Tests run: 51, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn test` | BUILD SUCCESS；Tests run: 306, Failures: 0, Errors: 0, Skipped: 1 |
| `mvn --% -Dtest=MysqlMigrationSmokeTest -Dlearningos.mysql.smoke=true test` | BUILD FAILURE；环境限制：`Access denied for user 'root'@'localhost'` |

## Architecture Drift Post-Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 不接触 provider；模型证据仍经 `AiModelGateway` / `AgentRunRecorder` |
| Frontend rules | PASS | 未修改 frontend |
| Agent / RAG rules | PASS | 模型调用仍写 trace/model/token evidence |
| Security | PASS | provider 低基数归一化；raw provider error / secret 不落库 |
| API / Database | PASS | API 不变；V18 schema 已由 SPEC / migration / convergence test 覆盖 |

## 文件变更

- 新增 `backend/src/main/resources/db/migration/V18__model_call_provider_observability.sql`
- 修改 `backend/src/main/java/com/learningos/agent/domain/ModelCallLog.java`
- 修改 `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- 修改 `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- 修改 `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- 修改 `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- 修改 `backend/src/test/java/com/learningos/agent/application/AgentRunRecorderTest.java`
- 修改 `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java`
- 修改 `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`

## 风险与缓解

| Risk | Mitigation |
|---|---|
| 历史数据新增 non-null 字段失败 | V18 使用 `not null default 'none'`。 |
| provider 写入 secret / URL / tenant | gateway 与 recorder 双层白名单归一化，未知值写 `other`。 |
| H2 测试不能验证 MySQL Flyway | 保留文本 convergence，并运行真实 MySQL smoke。 |
| 失败路径签名破坏调用点 | 新增 overload，旧方法默认 `none`，gateway failure path 调用 provider overload。 |
| 误接真实 provider 或新增依赖 | Context Pack 明确禁止 `pom.xml` 和 provider SDK。 |
