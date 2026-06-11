# CONTEXT-20260608 模型调用 provider 持久化观测

## Current Task

P3-3-B：为 `model_call_log` 增加 provider 持久化观测，并让成功/失败模型日志写入安全低基数 provider。

Status：Completed。

## Related Memory and Docs

- `AGENTS.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/model-gateway-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/specs/SPEC-20260608-model-call-provider-observability.md`

## Selected Skills

- feature-development-workflow
- spring-ai-agent-backend
- agent-trace-governance
- model-gateway-boundary
- database-design
- security-review
- test-driven-development
- verification-before-completion
- Confidence Check

## Subagent Plan

- DB/Migration Expert：已完成只读分析，确认 V18 / entity / MySQL smoke 必改。
- Security & Quality Expert：已完成只读分析，要求 provider 白名单低基数归一化，禁止 raw URL / key / tenant。
- Test Integration Reviewer：已完成只读分析，给出 RED/GREEN 测试顺序。
- Integration：主 Codex 汇总为 `docs/subagents/runs/RUN-20260608-model-call-provider-observability-integration.md`。
- Implementation：主 Codex 串行实现，不并行修改同一文件。

## Files Allowed To Modify

- `backend/src/main/resources/db/migration/V18__model_call_provider_observability.sql`
- `backend/src/main/java/com/learningos/agent/domain/ModelCallLog.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- `backend/src/test/java/com/learningos/agent/application/AgentRunRecorderTest.java`
- `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `docs/product/PRD-20260608-model-call-provider-observability.md`
- `docs/requirements/REQ-20260608-model-call-provider-observability.md`
- `docs/specs/SPEC-20260608-model-call-provider-observability.md`
- `docs/plans/PLAN-20260608-model-call-provider-observability.md`
- `docs/tasks/TASK-20260608-model-call-provider-observability.md`
- `docs/context/CONTEXT-20260608-model-call-provider-observability.md`
- `docs/evidence/EVIDENCE-20260608-model-call-provider-observability.md`
- `docs/acceptance/ACCEPT-20260608-model-call-provider-observability.md`
- `docs/retrospectives/RETRO-20260608-model-call-provider-observability.md`
- `docs/subagents/runs/RUN-20260608-model-call-provider-observability-integration.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/model-gateway-boundary.md`

## Files Not Allowed To Modify

- `backend/pom.xml`
- `frontend/**`
- `docs/superpowers/**`
- RAG parser/indexer/vector/retrieval implementation files
- Permission/RBAC implementation files
- Any provider SDK, secret, or credential configuration files

## Test Commands

```powershell
cd backend
mvn --% -Dtest=SchemaConvergenceMigrationTest,AgentRunRecorderTest,AiModelGatewayTest test
mvn --% -Dtest=SchemaConvergenceMigrationTest,AgentRunRecorderTest,AiModelGatewayTest,ResourceGenerationControllerTest test
mvn --% -Dtest=MysqlMigrationSmokeTest -Dlearningos.mysql.smoke=true test
mvn test
```

## Architecture Drift Pre-Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 只改 gateway / recorder / entity / migration。 |
| Frontend rules | PASS | 不改 frontend。 |
| Agent / RAG rules | PASS | 模型调用仍写 trace/model/token 日志。 |
| Security | PASS | provider 安全归一化，不保存 raw error。 |
| API / Database | PASS | DB 变更已在 SPEC 记录；API 不变。 |

## Architecture Drift Post-Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 不接触 provider；gateway / recorder / entity 分层保持 |
| Frontend rules | PASS | 未修改 frontend |
| Agent / RAG rules | PASS | Agent 执行仍写 trace/model/token evidence |
| Security | PASS | provider 白名单归一化，raw error / secret 不落库 |
| API / Database | PASS | API 不变；V18 schema 与 SPEC / migration test 对齐 |

## Verification Summary

| Command | Result |
|---|---|
| `mvn --% -Dtest=SchemaConvergenceMigrationTest,AgentRunRecorderTest,AiModelGatewayTest,ResourceGenerationControllerTest test` | BUILD SUCCESS；Tests run: 51, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn test` | BUILD SUCCESS；Tests run: 306, Failures: 0, Errors: 0, Skipped: 1 |
| `mvn --% -Dtest=MysqlMigrationSmokeTest -Dlearningos.mysql.smoke=true test` | BUILD FAILURE；本机 MySQL 3306 `root` 凭据不可用，真实 V18 smoke 待补跑 |

## Confidence Check

- No duplicate implementations：PASS，现有 `ModelResponse.provider` 未落 DB。
- Architecture compliance：PASS，保持 backend-owned model gateway 与 recorder。
- Official docs：N/A/PASS，本轮不接外部 SDK/API。
- OSS reference：N/A/PASS，本轮是项目内 schema/recorder 增量。
- Root cause：PASS，`model_call_log` 缺 provider 字段且 recorder 未持久化 provider。

Confidence：0.95，可进入 TDD 实现。
