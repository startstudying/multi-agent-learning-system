# RAG 文档上传幂等 REQ

## 需求

1. `POST /api/knowledge-bases/{kbId}/documents` 支持可选 `requestId` 表单字段。
2. 当 `requestId` 存在时，服务层按 `createdBy + requestId` 查找已有上传记录。
3. 相同请求 payload 命中已有记录时，返回首次 `DocumentUploadResponse`。
4. 不同请求 payload 复用同一 `requestId` 时，返回 `409 CONFLICT`。
5. `requestId` 最大长度为 120，空白视为未传。
6. 上传 payload hash 至少覆盖：`userId`、`kbId`、`courseId`、`chapterId`、文件名、contentType、文件大小、文件内容 SHA-256。
7. 数据库需要唯一索引防并发：`kb_document(created_by, request_id)`。

## 验收

- 单元或集成测试覆盖首次上传、同 payload 重放、payload 冲突、迁移字段/索引。
- 旧上传接口不传 `requestId` 时保持兼容。
