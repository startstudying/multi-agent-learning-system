# RUN - RAG Chunk Production Metadata - Test Strategy

## 1. 角色

Test Engineer。

## 2. RED 测试清单

优先添加以下失败测试，再实现生产代码：

| 测试位置 | 行为 |
|---|---|
| `IndexServiceTest` | 长 Markdown 文本按 token-ish 窗口切出多个 chunk，且相邻 chunk 有 overlap |
| `IndexServiceTest` | 同一文档重复 reindex 后相同 chunk 的 `chunkHash` 保持稳定 |
| `IndexServiceTest` | Markdown heading hierarchy 写入 metadata，`sectionTitle` 使用当前标题 |
| `IndexServiceTest` | 不同 heading 下相同正文不会被正文级去重误删 |
| `IndexTaskWorkerSchedulerTest` | worker 自动处理路径产生相同生产 metadata 和 `chunkHash` |
| `SchemaConvergenceMigrationTest` | V17 migration 包含 `chunk_hash`、`idx_kb_doc_chunk_hash`、`uk_kb_doc_chunk_document_version_hash` |
| `MysqlMigrationSmokeTest` | latest version/count 更新到 17，并验证 MySQL schema 中 `chunk_hash` 列和索引 |

## 3. 验证命令

```powershell
cd backend
mvn --% -Dtest=IndexServiceTest#processMarkdownIndexTaskUsesTokenWindowOverlapStableHashAndHeadingHierarchy test
mvn --% -Dtest=IndexServiceTest#reindexKeepsStableChunkHashesForSameDocumentVersion,IndexServiceTest#sameTextUnderDifferentHeadingsIsNotDeduplicatedAway,IndexTaskWorkerSchedulerTest#workerProducesProductionChunkMetadata test
mvn --% -Dtest=SchemaConvergenceMigrationTest test
mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test
mvn test
```

真实 MySQL smoke 若 3306 被占用，继续使用 3307：

```powershell
$env:MYSQL_PORT='3307'
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
```

## 4. 退出标准

- RED 阶段失败原因必须指向缺失字段或缺失行为，而不是编译错误或测试夹具错误。
- GREEN 后聚焦测试、迁移文本测试、后端全量测试通过。
- MySQL smoke 若环境不可用，Evidence 必须记录具体失败原因。
