# RAG 文档上传幂等 PLAN

## 范围

完成 P0-3 中“重复上传不产生重复业务结果”的最小后端切片。

## 步骤

| Step | Status | 内容 |
|---|---|---|
| 1 | DONE | 阅读现有 DocumentController / DocumentService / KbDocument / tests |
| 2 | DONE | 写 RED 测试覆盖上传 replay 和冲突 |
| 3 | DONE | 新增 V8 migration 和实体/仓储字段 |
| 4 | DONE | 实现 service/controller `requestId` 幂等 |
| 5 | DONE | 运行相关测试并更新 Evidence/Acceptance |

## 影响文件

- `backend/src/main/java/com/learningos/rag/api/DocumentController.java`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/main/java/com/learningos/rag/domain/KbDocument.java`
- `backend/src/main/java/com/learningos/rag/repository/KbDocumentRepository.java`
- `backend/src/main/resources/db/migration/V8__rag_document_upload_idempotency.sql`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
