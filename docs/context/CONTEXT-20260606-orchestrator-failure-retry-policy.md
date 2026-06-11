# Orchestrator 失败与重试策略 Context Pack

## 1. 当前任务

推进 backend TODO P0-1/P0-3：补齐 Orchestrator 通用 `RuntimeException` 失败证据、最小 retry endpoint，以及 `RESOURCE_GENERATION`、`RAG_QA`、`ANSWER_SUBMISSION` 的失败/重试策略说明。

## 2. 已读上下文

- `AGENTS.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/specs/SPEC-20260606-orchestrator-runtime-failure-evidence.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`

## 3. Selected Skills

- `feature-development-workflow`
- `spring-ai-agent-backend`
- `agent-trace-governance`
- `test-driven-development`
- 项目内：`spring-boot-architecture`、`api-contract-design`、`agent-workflow-design`、`agent-trace-design`、`test-generator`

## 4. Subagent Plan

不再启用额外 subagent。当前执行者为后端 Worker B，单任务实现。

## 5. Allowed Files

- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java`
- `backend/src/main/java/com/learningos/orchestrator/dto/*`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `docs/product/PRD-20260606-orchestrator-failure-retry-policy.md`
- `docs/requirements/REQ-20260606-orchestrator-failure-retry-policy.md`
- `docs/specs/SPEC-20260606-orchestrator-failure-retry-policy.md`
- `docs/plans/PLAN-20260606-orchestrator-failure-retry-policy.md`
- `docs/tasks/TASK-20260606-orchestrator-failure-retry-policy.md`
- `docs/context/CONTEXT-20260606-orchestrator-failure-retry-policy.md`
- `docs/evidence/EVIDENCE-20260606-orchestrator-failure-retry-policy.md`
- `docs/acceptance/ACCEPT-20260606-orchestrator-failure-retry-policy.md`

## 6. Not Allowed

- `docs/memory/*`
- `docs/changelog/*`
- `docs/planning/backend-architecture-todolist.md`
- RAG document upload/idempotency 文件
- `IndexService`

## 7. Test Commands

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest" test
```

## 8. Acceptance Boundary

完成本切片后，失败 workflow 可被 GET 查询到安全 failure evidence；retry endpoint 存在并覆盖 `FAILED` 与非 `FAILED` 两类路径。完整后台恢复、自动重试和 workflow 独立表留给后续任务。
