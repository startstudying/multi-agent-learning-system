# RAG 索引任务 Active 去重规格

## 状态定义

Active 索引任务：

```text
PENDING
RUNNING
```

Terminal 索引任务：

```text
SUCCEEDED
FAILED
```

## 服务规则

`IndexService.createPendingTask(KbDocument document)`：

1. 查询 `KbIndexTaskRepository.findFirstByDocumentIdOrderByCreatedAtDesc(document.getId())`。
2. 如果最新任务状态是 `PENDING` 或 `RUNNING`，直接返回该任务。
3. 如果不存在任务，或最新任务状态是 `FAILED` / `SUCCEEDED`，创建新的 `PENDING` 任务。

## API 行为

`POST /api/documents/{documentId}/reindex`：

- 文档有 active task：返回已有 `indexTaskId` 和当前 status。
- 文档无 active task：返回新建 `indexTaskId` 和 `PENDING`。
- 权限仍由 `DocumentService.ensureCanWrite(...)` 控制。

## 兼容性

- 上传文档第一次创建任务行为不变。
- API 响应 DTO 不变。
- 不新增迁移。
