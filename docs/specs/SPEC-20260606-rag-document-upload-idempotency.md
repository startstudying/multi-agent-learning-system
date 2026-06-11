# RAG 文档上传幂等 SPEC

## API

`POST /api/knowledge-bases/{kbId}/documents`

新增 multipart form 字段：

- `requestId`：可选，长度不超过 120。

旧客户端不传 `requestId` 时沿用原上传流程。

## 数据模型

`kb_document` 新增：

- `request_id varchar(120)`
- `request_hash varchar(128)`
- `response_json text`
- 唯一索引：`uk_kb_document_user_request(created_by, request_id)`

## 服务流程

1. 校验 KB 存在和写权限。
2. 若 `requestId` 为空，执行原上传流程。
3. 若 `requestId` 非空，计算规范化 payload hash。
4. 查找 `createdBy + requestId`：
   - hash 相同且 response snapshot 存在：反序列化并返回首次响应。
   - hash 不同：返回 409。
   - response snapshot 缺失：返回 409，表示请求仍在处理中或记录不完整。
5. 未命中时存储文件、保存 `kb_document`、创建 pending index task、写入 response snapshot。
6. 并发唯一索引冲突时重新查找 winner，并按相同 replay 规则返回或 409。

## 安全

- 权限检查发生在 replay 之前。
- 错误信息不回显原始文件内容。
- payload hash 使用文件内容摘要，不保存文件内容。

## 风险

- 当前文件 hash 使用 `MultipartFile#getBytes()`，大文件场景后续应改为流式 digest。
- 真实 MySQL Flyway smoke 仍属于 P3。
