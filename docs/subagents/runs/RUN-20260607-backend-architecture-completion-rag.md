# RUN-20260607 Agent/RAG Expert

## 任务

围绕 `docs/planning/backend-architecture-todolist.md` 中 P3-2 RAG 索引生产化条目做只读专家分析。

## 1. 当前已实现证据

### Parser

- `rag/parser` 已提供统一 parser 边界：Markdown/TXT/PDF/DOCX 输出 `ParsedSection`。
- Markdown 已按 heading 生成 section；TXT/PDF/DOCX 当前仍为轻量文本抽取。
- PDF 页码当前固定为 `1`，DOCX 仅从 `word/document.xml` 抽文本。
- `DocumentParserServiceTest` 已覆盖 Markdown heading hierarchy、TXT 空内容、PDF/DOCX 轻量解析。

### Index / chunk

- `IndexService` 已统一 parser → token-ish chunk → metadata/hash → chunk replace。
- chunk 策略为 `TOKEN_WINDOW_V1`，220 token-ish window、40 overlap。
- chunk metadata 已记录 parser、embeddingModel、contentLength、chunkingStrategy、chunkTokenCount、overlapTokenCount、headingLevel、headingPath。
- chunk hash 使用 document id、version、headingPath、content 生成 SHA-256。
- 测试覆盖 token window、overlap、metadata、stable hash 和 parser failure 安全错误码。

### Index task worker / recovery

- 创建索引任务时对 document 加行锁并复用 active `PENDING/RUNNING` task。
- 索引任务支持 progress、phase、heartbeat、lease、retry、recoverable、detail API。
- worker scheduler 自动 claim due task 并逐个处理。

### Query / citation

- RAG query 先经 `PermissionService.requireReadableKbIds` 权限过滤。
- 当前 retrieval 为 `ChunkService.retrieveAllowedChunks` → `RerankerService.rerankOrFallback` → citation 生成。
- query log 持久化 traceId、kbIds、question、requestId、requestHash、responseJson、retrievalCount、rerankerStatus、sourcesJson、latencyMs。
- 非空 sources 会写 `source_citation`，no-source 不写伪 citation。

### Schema

- V16 增加 index task worker/progress/lease 字段和索引。
- V17 增加 `kb_doc_chunk.chunk_hash`、唯一索引和查询索引。
- MySQL smoke 当前覆盖到 V17。

## 2. 未完成项拆分

1. 复杂 PDF/DOCX、OCR fallback、真实页码和章节层级识别。
2. Embedding service 与可选 VectorDB adapter。
3. Hybrid retrieval、RRF、reranker timeout fallback。

## 3. 建议最小实现方案

### 优先级 1：无新依赖 hybrid retrieval 过渡层

- 将 `ChunkService` 从“最近 chunk”改为关键词 lexical candidate + 可选 vector candidate 的接口化结构。
- 先不新增外部依赖：按 question token 命中、sectionTitle 命中、createdAt 计算 lexical score。
- 新增 RRF 合并函数；vector adapter disabled 时只合并 lexical list。
- `RerankerService` 使用 `RagProperties.rerankerTimeoutMs`，默认 passthrough；未来真实 reranker 必须 timeout fallback。

### 优先级 2：Embedding/Vector adapter 先抽接口

- `EmbeddingService` 增加 `embed(String text)` 和 `enabled()`，默认 disabled/noop。
- 新建可选 `VectorIndexAdapter` 接口：`upsert`、`deleteByDocumentId`、`search`。
- `IndexService` 在 `EMBEDDING` phase 调用 embedding/vector adapter；disabled 时保持当前 chunk 持久化。

### 优先级 3：Parser 增强分阶段

- 无新依赖阶段：DOCX 解析 `w:p`、`w:pStyle`、`w:t`，识别 Heading1/2/3 和分页符；PDF 增加 parser quality metadata，无法可靠抽取时返回 `OCR_REQUIRED` 或安全失败码。
- 需要依赖阶段：PDFBox / Apache POI / OCR 必须先做 dependency review。

## 4. 风险

- 复杂 PDF/OCR 无新依赖很难真正达标，不能把轻量 parser 误标为完整能力。
- 当前检索按创建时间取 chunk，相关性弱。
- Reranker 当前 passthrough，无 timeout/failure 状态。
- VectorDB / OCR 依赖会影响部署、健康检查、测试和安全评审。

## 5. 建议测试命令

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest,IndexTaskWorkerSchedulerTest,IndexTaskRecoverySchedulerTest,SchemaConvergenceMigrationTest test
mvn --% -Dtest=DocumentControllerTest,ChatControllerTest,KnowledgeBaseControllerTest,RagEvaluationServiceTest test
mvn test
```

MySQL Flyway smoke 可用时追加：

```powershell
$env:MYSQL_PORT='3307'
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
```
