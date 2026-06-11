# RAG 文档上传幂等 Acceptance

| 验收项 | 状态 | 证据 |
|---|---|---|
| 上传接口接受可选 `requestId` | PASS | `DocumentController.upload(...)` |
| 首次上传保存 `requestId/requestHash/responseJson` | PASS | `DocumentControllerTest.replaysDocumentUploadWithSameRequestIdWithoutDuplicatingDocumentOrIndexTask` |
| 相同 payload 重放首次响应 | PASS | 同上，断言 `documentId/indexTaskId` 不变 |
| 重放不新增文档和索引任务 | PASS | 同上，断言 `documentRepository.count() == 1` 且 `indexTaskRepository.count() == 1` |
| 不同 payload 复用 `requestId` 返回 409 | PASS | `DocumentControllerTest.rejectsDocumentUploadWhenSameRequestIdUsesDifferentPayload` |
| V8 migration 包含字段和唯一索引 | PASS | `SchemaConvergenceMigrationTest.v8MigrationAddsDocumentUploadIdempotencyColumnsAndConstraint` |

## 剩余风险

- 文件内容 hash 当前通过 `MultipartFile#getBytes()` 计算，大文件场景后续应改为流式 digest。
- V8 只有文本迁移检查和 H2/JPA 覆盖，真实 MySQL smoke 仍属于 P3。
