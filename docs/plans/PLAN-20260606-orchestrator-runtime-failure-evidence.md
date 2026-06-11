# Orchestrator 运行期失败证据持久化计划

## 1. 追溯

- PRD：`docs/product/PRD-20260606-orchestrator-runtime-failure-evidence.md`
- REQ：`docs/requirements/REQ-20260606-orchestrator-runtime-failure-evidence.md`
- SPEC：`docs/specs/SPEC-20260606-orchestrator-runtime-failure-evidence.md`

## 2. Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 本任务是 P0 后端功能切片，必须走文档、测试、证据和记忆链路。 |
| `ai-learning-agent-development` | 任务属于多 Agent 学习系统的 Orchestrator 闭环。 |
| `ai-learning-architecture` | 涉及 workflow failure strategy、状态和 fallback 行为。 |
| `agent-trace-governance` | 涉及 `agent_task`、`agent_trace` 和失败审计。 |
| `spring-ai-agent-backend` | 涉及 Java/Spring Boot Service 事务和测试。 |
| `test-driven-development` | 行为变更必须先写失败测试。 |
| `verification-before-completion` | 完成声明前必须跑聚焦和全量验证。 |

Missing skills: 无。

GitHub Research Needed: No。当前切片是本仓库 Orchestrator/Trace 事务边界修复，不需要外部参考。

New Project-Specific Skill To Create: No。

## 3. Subagent Decision

Use Subagents: Yes。

Reason: 用户要求多 subagent 并行开发，且本任务涉及 Orchestrator、Trace、RAG、安全隐私和测试。

Parallelism Level: L1。

Selected Subagents:

- 架构审查：事务边界、失败 step、范围控制。
- 测试审查：RED 测试和断言点。
- 安全审查：失败摘要脱敏、错误码保留、无成功伪证据。

Implementation Mode: Single Codex implementation with parallel analysis。

## 4. Confidence Check

| Check | Status | Evidence |
|---|---|---|
| No duplicate implementation | PASS | `RESOURCE_GENERATION` 模型失败已有证据；`RAG_QA/ANSWER_SUBMISSION` 运行期 `ApiException` 仍可能回滚。 |
| Architecture compliance | PASS | 修改集中在 Orchestrator Service 和 Orchestrator API test。 |
| Official docs verified | N/A | 不新增外部 SDK/API。 |
| OSS references | N/A | 不引入新依赖。 |
| Root cause identified | PASS | `createWorkflow` 事务当前只对模型失败 noRollback，未覆盖运行期业务异常。 |

Confidence: 0.91。可以进入 TDD。

## 5. 实施阶段

| 阶段 | 说明 | 状态 |
|---|---|---|
| 1 | 写 RAG_QA 运行期权限失败 RED 测试 | 已完成 |
| 2 | 实现 `ApiException` 运行期失败持久化 | 已完成 |
| 3 | 跑 Orchestrator 聚焦测试 | 已完成 |
| 4 | 跑交叉回归和全量测试 | 已完成 |
| 5 | 更新 Evidence / Acceptance / Memory / Changelog / TODO | 已完成 |

## 6. 文件变更清单

| 文件 | 操作 |
|---|---|
| `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java` | 修改 |
| `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java` | 修改 |
| `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java` | 修改 |
| 本任务对应 `docs/**` | 新增/更新 |

## 7. 测试命令

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest" test
mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest,AssessmentControllerTest,AssessmentServiceTest" test
mvn test
```
