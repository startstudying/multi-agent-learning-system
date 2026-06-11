# Orchestrator Resource Generation 上下文统一 Context Pack

## 允许修改

- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- 本轮 `docs/product`、`docs/requirements`、`docs/specs`、`docs/plans`、`docs/tasks`、`docs/context`、`docs/evidence`、`docs/acceptance`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/changelog/CHANGELOG.md`
- `docs/subagents/runs/`

## 不允许修改

- 前端代码。
- RAG 生产代码。
- Assessment 生产代码。
- 数据库迁移。
- 依赖配置。

## 当前任务边界

只做 P0-1 的 `RESOURCE_GENERATION` 最小接入：Orchestrator 创建 workflow 后直接执行资源生成，并让资源生成复用同一个 `agentTaskId/traceId`。不处理 RAG QA、Answer Submission、workflow retry/recovery 或独立 workflow 表。

## 测试命令

```bash
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,AgentRunRecorderTest" test
mvn test
```
