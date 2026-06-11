# RAG 文档上传幂等 Context

## 当前状态

- `DocumentService.upload(...)` 每次都会保存新 `KbDocument`。
- `IndexService.createPendingTask(...)` 已支持同一 document 的 active task 复用。
- 但重复上传同一业务请求仍会产生不同 document，因此 active task dedup 不能阻止重复业务结果。

## 设计约束

- 保持旧 API 兼容：不传 `requestId` 继续可上传。
- 幂等语义只对同一用户生效。
- 先校验写权限，再 replay。
- 复用 answer/RAG query 的 `requestHash + responseJson + unique index` 模式。
