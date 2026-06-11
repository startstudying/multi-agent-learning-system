# Orchestrator RAG_QA 上下文收敛 Context Pack

## 允许修改

- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- 本轮 `docs/product`、`docs/requirements`、`docs/specs`、`docs/plans`、`docs/tasks`、`docs/context`、`docs/evidence`、`docs/acceptance`、`docs/retrospectives`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/changelog/CHANGELOG.md`

## 不允许修改

- 前端代码。
- Assessment 生产代码。
- Resource Generation 生产代码。
- 数据库迁移。
- 依赖配置。

## 当前任务边界

只做 P0-1 的 `RAG_QA` 最小上下文收敛：Orchestrator 创建统一 task/trace，RAG 查询复用 traceId，追加 RAG trace steps。`ANSWER_SUBMISSION`、运行期失败 no-rollback 策略、workflow retry/recovery 留给后续切片。

## 测试命令

```bash
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest" test
mvn test
```
