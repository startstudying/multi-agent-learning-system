# Acceptance - RAG Parser Adapter 最小生产切片

## 验收结论

已通过。

## 验收项

- [x] `IndexService` 不再内嵌 Markdown / TXT / PDF / DOCX 解析细节
- [x] 新的 parser 边界位于 `backend/src/main/java/com/learningos/rag/parser/**`
- [x] Markdown 仍保留 heading hierarchy
- [x] TXT 仍输出单 section
- [x] PDF / DOCX 继续保留轻量解析能力
- [x] parser 失败只返回安全错误码，不泄露原始异常文本
- [x] worker/manual 两条索引路径共用同一 parser 边界
- [x] 现有 chunk/hash/metadata 行为未回退
- [x] 相关测试通过

## 验收依据

- `DocumentParserServiceTest` 通过
- `IndexServiceParserFailureTest` 通过
- `IndexServiceTest` 通过
- `IndexTaskWorkerSchedulerTest` 通过
- `SchemaConvergenceMigrationTest` 通过
- `MysqlMigrationSmokeTest` 仍保持默认跳过
- `mvn test` 通过
