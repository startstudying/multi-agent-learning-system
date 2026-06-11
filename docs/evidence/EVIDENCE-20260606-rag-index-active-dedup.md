# RAG 索引任务 Active 去重证据

## 范围

本轮完成 P0-3 的最小 RAG 幂等切片：同一文档最新索引任务为 `PENDING` 或 `RUNNING` 时，重复 `reindex` 返回已有任务；最新任务为 `FAILED` 或 `SUCCEEDED` 时，新建一个 `PENDING` 任务。

本轮不实现文档上传 `requestId`、RAG query 响应重放、后台恢复扫描或数据库唯一约束。

## 代码证据

- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
  - 增加 `ACTIVE_STATUSES = PENDING/RUNNING`。
  - `createPendingTask(...)` 先查询该文档最新索引任务。
  - 最新任务为 active 时复用；否则创建新 `PENDING` 任务。
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
  - 覆盖上传后 `PENDING` 任务重复 reindex 复用。
  - 覆盖 `RUNNING` 任务重复 reindex 复用。
  - 覆盖 `FAILED` 和 `SUCCEEDED` 后再次 reindex 新建任务。

## TDD 过程

### RED

先补测试后运行聚焦测试：

```text
cd backend
mvn "-Dtest=DocumentControllerTest" test
```

结果：失败，符合预期。

关键失败：

```text
Tests run: 4, Failures: 2, Errors: 0, Skipped: 0
uploadsDocumentCreatesPendingIndexTaskAndSupportsReindex: expected first indexTaskId, got a new idx_*
reindexReturnsActiveRunningTaskAndCreatesNewTaskAfterTerminalStatus: expected running task id, got a new idx_*
```

说明旧实现每次调用 `createPendingTask(...)` 都新建 `kb_index_task`，没有 active task 去重。

### GREEN：聚焦测试

实现 Service 层 active 去重后重新运行：

```text
cd backend
mvn "-Dtest=DocumentControllerTest" test
```

结果：

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T02:13:54+08:00
```

### GREEN：全量后端测试

完成后运行：

```text
cd backend
mvn test
```

结果：

```text
Tests run: 95, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T02:15:21+08:00
```

## 覆盖范围

- `PENDING` 最新索引任务重复 `reindex` 返回同一 `indexTaskId`，任务数量不增加。
- `RUNNING` 最新索引任务重复 `reindex` 返回同一 `indexTaskId`，任务数量不增加。
- `FAILED` 最新索引任务再次 `reindex` 新建 `PENDING` 任务。
- `SUCCEEDED` 最新索引任务再次 `reindex` 新建 `PENDING` 任务。
- 公开知识库仍只允许所有者写入，其他用户可读不可 reindex。
- 文档上传、详情和列表响应结构保持兼容。

## 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | Controller 未增加业务逻辑；去重逻辑在 `IndexService`。 |
| Frontend rules | PASS | 未修改前端。 |
| Agent / RAG rules | PASS | 保持 RAG 索引任务语义；本轮不涉及生成引用。 |
| Security | PASS | 未新增依赖、密钥或权限绕过；写权限仍由 `DocumentService.ensureCanWrite(...)` 控制。 |
| API / Database | PASS | 响应 DTO 不变；未修改数据库 schema。 |

## 后续限制

- 当前是单事务查询最新任务后的应用层去重；高并发下仍可能在极小窗口重复创建任务，生产化阶段应考虑业务锁或唯一约束。
- 文档上传本身还没有 `requestId`。
- RAG query 还没有响应快照重放。
- 索引 worker 的 `retry_count`、`next_retry_at`、`last_error`、后台恢复扫描仍是后续 P0-3/P3-2 任务。
