# Evidence - RAG Chunk 生产化元数据补齐

## 1. 范围

本证据归档 TODO P3-2 的一个独立切片：

```text
补齐生产级 chunk token 切分、overlap、稳定 chunk hash 和 heading hierarchy
```

本轮不关闭 parser adapter/OCR、Embedding/VectorDB、hybrid retrieval/RRF、reranker timeout fallback，也不改变公开 RAG query / citation API。

## 2. 关键变更证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/resources/db/migration/V17__rag_chunk_production_metadata.sql` | `kb_doc_chunk` 增加 `chunk_hash`，回填历史 chunk hash，并创建 `(document_id, document_version, chunk_hash)` 唯一索引和查询索引。 |
| `backend/src/main/java/com/learningos/rag/domain/KbDocChunk.java` | 实体增加 `chunkHash` 字段、JPA 唯一约束/索引映射，并通过 `@PrePersist` 为旧测试夹具或手工保存提供 fallback hash。 |
| `backend/src/main/java/com/learningos/rag/repository/KbDocChunkRepository.java` | 旧 chunk 清理改为 bulk delete，避免逐实体删除在索引流程中触发不必要 flush/lock 问题。 |
| `backend/src/main/java/com/learningos/rag/application/IndexService.java` | chunk 生产改为 `TOKEN_WINDOW_V1`，使用 220 token-ish 窗口、40 token overlap、Markdown heading hierarchy、SHA-256 稳定 hash 和结构化 metadata。 |
| `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java` | 覆盖 token window、40 token overlap、heading path、稳定 hash、不同 heading 下相同正文不误去重、TXT/PDF/DOCX 基础 chunk 兼容。 |
| `backend/src/test/java/com/learningos/rag/application/IndexTaskWorkerSchedulerTest.java` | 覆盖 worker 路径生成的 chunk metadata 与手动索引路径一致。 |
| `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java` | 覆盖 V17 migration 文本中的 `chunk_hash`、唯一索引和查询索引。 |
| `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java` | 最新迁移版本和数量更新为 17，并在真实 MySQL smoke 中验证 `chunk_hash` 列和两个索引存在。 |

## 3. TDD / 调试证据

前序实现阶段已按 TASK 要求先补测试再实现。由于当前会话是接续已完成实现的收尾阶段，未回滚代码去重放红灯，避免破坏已经通过的实现状态。

本轮复核确认的测试覆盖点包括：

- `processMarkdownIndexTaskUsesTokenWindowOverlapStableHashAndHeadingHierarchy`
- `reindexKeepsStableChunkHashesForSameDocumentVersion`
- `sameTextUnderDifferentHeadingsIsNotDeduplicatedAway`
- `workerProcessesDuePendingTaskAndMarksDocumentIndexed`
- `v17MigrationAddsRagChunkProductionMetadataColumnsAndIndexes`
- `MysqlMigrationSmokeTest` 对 V17 schema 的真实 MySQL 断言

## 4. 正向验证命令

### 4.1 P3-2 chunk 聚焦回归

命令：

```powershell
cd backend
mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test
```

结果：

```text
Tests run: 31, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

说明：`MysqlMigrationSmokeTest` 在普通 Maven 测试中按设计跳过，真实 MySQL smoke 见 4.3。

### 4.2 后端全量回归

命令：

```powershell
cd backend
mvn test
```

结果：

```text
Tests run: 231, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

### 4.3 真实 MySQL 8 smoke

命令：

```powershell
$env:MYSQL_PORT='3307'
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
```

结果：

```text
Successfully validated 17 migrations
Migrating schema `learning_os_migration_smoke` to version "17 - rag chunk production metadata"
Successfully applied 17 migrations to schema `learning_os_migration_smoke`, now at version v17
MySQL migration smoke test passed.
```

说明：Flyway 输出的 `Table ... already exists` 和 `PROCEDURE ... does not exist` 为既有 idempotent DDL 模式警告；最终迁移成功且 smoke 断言通过。

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | `IndexService` 负责 chunk 生产规则，Repository 只做持久化，worker 只调度并复用 Service 逻辑。 |
| Frontend rules | PASS | 未修改前端，前端仍不直接调用 LLM。 |
| Agent / RAG rules | PASS | 本轮增强离线索引 chunk 元数据，不改变 RAG answer/citation 生成链路。 |
| Security | PASS | 无新增依赖；metadata 不保存原文片段、overlap 原文、hash 原料、storage key 或 provider 错误文本。 |
| API / Database | PASS | V17 schema、实体映射和 SPEC 一致；公开 RAG query / citation API 未变更。 |

## 6. 残余风险

- 当前 token-ish 切分仍不是模型 tokenizer，后续接入真实 embedding/chat 模型时需要重新校准 token 上限。
- Markdown heading hierarchy 已覆盖，复杂 PDF/DOCX 章节和真实页码仍属于后续 parser adapter/OCR 切片。
- 当前 hash 唯一性限定在 `(document_id, document_version, chunk_hash)`，不是跨文档全局去重。
- 真实 MySQL smoke 验证了 V17 DDL 可执行性，但没有做大文档批量索引性能压测。
