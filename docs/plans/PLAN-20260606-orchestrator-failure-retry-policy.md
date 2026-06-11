# Orchestrator 失败与重试策略计划

## 1. Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求功能/修复先写文档和 Context Pack |
| `spring-ai-agent-backend` | Java 21 + Spring Boot Orchestrator service/controller 实现 |
| `agent-trace-governance` | 涉及 `agent_task`、`agent_trace` 失败证据 |
| `test-driven-development` | 行为变更先写失败测试 |
| `spring-boot-architecture` | 保持 Controller -> Service 分层 |
| `api-contract-design` | 新增 retry endpoint 契约 |
| `agent-workflow-design` | 明确 workflow failure/retry 策略 |
| `agent-trace-design` | 失败 step 脱敏和可查询 |
| `test-generator` | Controller 集成测试覆盖 |

Missing skills: 无。

GitHub Research Needed: No。本切片为项目内 Orchestrator 事务与 trace 补齐，不新增依赖。

New Project-Specific Skill To Create: No。

## 2. Subagent Decision

Use Subagents: No。

Reason: 当前会话身份已是后端 Worker B，文件边界明确，只涉及 Orchestrator service/controller/dto/test 和本切片 docs。

Parallelism Level: Single Worker。

Implementation Mode: Single task execution。

## 3. Confidence Check

| Check | Status | Evidence |
|---|---|---|
| No duplicate implementation | PASS | 已有 `ApiException` evidence，但无通用 `RuntimeException` 和 retry endpoint |
| Architecture compliance | PASS | 复用现有 controller/service/dto/repository/test 结构 |
| Official docs verified | N/A | 不新增外部 API/SDK |
| OSS references | N/A | 不新增依赖 |
| Root cause identified | PASS | `createWorkflow` 事务 noRollback 未覆盖通用运行期异常；controller 缺少 retry mapping |

Confidence: 0.93。

## 4. 实施步骤

1. 创建本切片 PRD/REQ/SPEC/PLAN/TASK/Context Pack。已完成。
2. 写 RED 测试覆盖 RuntimeException evidence 与 retry endpoint。已完成。
3. 实现 `RuntimeException` failure recording。已完成。
4. 实现 `POST /api/orchestrator/workflows/{workflowId}/retry` 最小 endpoint。已完成。
5. 运行聚焦测试。已完成。
6. 创建 Evidence / Acceptance。已完成。

## 5. 架构漂移检查

实施前：

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只委托 Service |
| Agent / RAG rules | PASS | 不改变 RAG citation 成功路径；失败不写伪成功证据 |
| Security | PASS | owner 查询、失败摘要脱敏 |
| API / Database | PASS | SPEC 已记录新 endpoint；不改 DB |

实施后需再次检查。

## 6. 实施后架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只新增 retry mapping，业务逻辑在 Service |
| Agent / RAG rules | PASS | 失败证据写入 `agent_task/agent_trace`；RAG 失败不写伪成功 citation |
| Security | PASS | retry/get 按 owner 查询；RuntimeException summary 只记录 `INTERNAL_ERROR` |
| API / Database | PASS | SPEC 已记录 retry endpoint；未改数据库 schema |
