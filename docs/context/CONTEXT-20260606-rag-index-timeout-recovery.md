# RAG 索引任务超时恢复 Context Pack

## 允许修改

- `backend/src/main/java/com/learningos/rag/domain/KbIndexTask.java`
- `backend/src/main/java/com/learningos/rag/repository/KbIndexTaskRepository.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- 本轮 `docs/product`、`docs/requirements`、`docs/specs`、`docs/plans`、`docs/tasks`、`docs/context`、`docs/evidence`、`docs/acceptance`、`docs/retrospectives`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/changelog/CHANGELOG.md`
- `docs/subagents/runs/`

## 不允许修改

- 前端代码。
- Orchestrator 生产代码。
- Resource Generation 生产代码。
- Assessment 生产代码。
- 数据库迁移。
- 依赖配置。

## 当前任务边界

只做 P0-3 的 RAG 索引超时恢复最小切片：扫描 `RUNNING` 且 `updatedAt` 早于阈值的索引任务，标记为 `FAILED` 并记录恢复证据。后台定时调度、文档上传幂等、RAG query 重放、数据库级并发去重留给后续切片。

## 测试命令

```bash
cd backend
mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test
mvn test
```
