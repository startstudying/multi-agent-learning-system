# Evidence - RAG Parser Adapter 最小生产切片

## 1. 目标

将 RAG 文档解析边界从 `IndexService` 中抽出到 `backend/src/main/java/com/learningos/rag/parser/**`，保持 Markdown / TXT / PDF / DOCX 的现有轻量解析行为，并确保 worker/manual 两条索引路径共用同一 parser 边界。

## 2. 交付内容

- 新增 `DocumentParserService`、`DocumentParser`、`ParsedDocument`、`ParsedSection`、`DocumentParseException`
- `IndexService` 现在只负责索引编排、chunk/hash/metadata 和任务状态流转
- parser 失败统一返回安全错误码 `DOCUMENT_PARSE_FAILED`
- 为 parser 边界补充单元测试，并为解析失败补充安全回归测试

## 3. 验证记录

### 3.1 parser 边界测试

命令：

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
```

结果：

- 3 tests
- 0 failures
- 0 errors
- 通过

### 3.2 parser + 索引回归

命令：

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest test
```

结果：

- 19 tests
- 0 failures
- 0 errors
- 通过

### 3.3 迁移与索引组合回归

命令：

```powershell
cd backend
mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test
```

结果：

- 31 tests
- 0 failures
- 0 errors
- 1 skipped (`MysqlMigrationSmokeTest`)
- 通过

### 3.4 全量回归

命令：

```powershell
cd backend
mvn test
```

结果：

- 235 tests
- 0 failures
- 0 errors
- 1 skipped (`MysqlMigrationSmokeTest`)
- 通过

## 4. 补充说明

- 本切片未新增依赖、未改数据库 schema、未改公开 RAG query/citation API。
- 由于当前子代理线程上限已满，额外的 code-review subagent 无法成功派发；最终以本地 diff 审查和完整测试结果收口。
