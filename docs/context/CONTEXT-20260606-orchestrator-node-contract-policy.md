# Orchestrator 节点契约与策略显式化 Context Pack

## 1. 当前目标

关闭 `docs/planning/backend-architecture-todolist.md` 中 P0-1 剩余项：明确每个已执行 workflow 节点的输入 DTO、输出 DTO、失败策略和可重试策略。

## 2. 已读上下文

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260606-orchestrator-failure-retry-policy.md`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowType.java`
- `backend/src/main/java/com/learningos/orchestrator/dto/*`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`

## 3. 关键判断

- 不新增 schema。
- 不新增 endpoint。
- 契约作为现有 `steps[]` 的稳定字段返回。
- `LEARNING_GOAL_CREATION` 不在本切片实现。
- 子代理只读分析，主线程单点编码。

## 4. 验证命令

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest#returnsExplicitNodeContractsForResourceGenerationWorkflow" test
mvn "-Dtest=OrchestratorWorkflowControllerTest" test
mvn "-Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest,RagQueryServiceTest,SchemaConvergenceMigrationTest" test
```
