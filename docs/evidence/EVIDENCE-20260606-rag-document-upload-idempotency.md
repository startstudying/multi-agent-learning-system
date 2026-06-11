# RAG 文档上传幂等 Evidence

## 变更文件

- `backend/src/main/resources/db/migration/V8__rag_document_upload_idempotency.sql`
- `backend/src/main/java/com/learningos/rag/domain/KbDocument.java`
- `backend/src/main/java/com/learningos/rag/repository/KbDocumentRepository.java`
- `backend/src/main/java/com/learningos/rag/api/DocumentController.java`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`

## RED

```powershell
cd backend
mvn "-Dtest=SchemaConvergenceMigrationTest,DocumentControllerTest" test
```

结果：失败，`KbDocument` 缺少 `getRequestId/getRequestHash/getResponseJson`，证明测试覆盖了新增幂等字段。

## GREEN

```powershell
cd backend
mvn "-Dtest=SchemaConvergenceMigrationTest,DocumentControllerTest" test
```

结果：12 tests, 0 failures, 0 errors。

## 行为证据

- `replaysDocumentUploadWithSameRequestIdWithoutDuplicatingDocumentOrIndexTask`
- `rejectsDocumentUploadWhenSameRequestIdUsesDifferentPayload`
- `v8MigrationAddsDocumentUploadIdempotencyColumnsAndConstraint`
