# RAG 索引任务 Active 去重 Context Pack

## 允许修改

- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- 本轮 `docs/product`、`docs/requirements`、`docs/specs`、`docs/plans`、`docs/tasks`、`docs/context`、`docs/evidence`、`docs/acceptance`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/changelog/CHANGELOG.md`

## 不允许修改

- 前端代码。
- Orchestrator 生产代码。
- Resource Generation 生产代码。
- 数据库迁移。
- 依赖配置。

## 当前任务边界

只做 P0-3 的 RAG 索引 active 去重：最新索引任务为 `PENDING/RUNNING` 时复用；为 `FAILED/SUCCEEDED` 时新建。上传 `requestId`、RAG query 重放、后台恢复任务留给后续切片。

## 测试命令

```bash
cd backend
mvn "-Dtest=DocumentControllerTest" test
mvn test
```
