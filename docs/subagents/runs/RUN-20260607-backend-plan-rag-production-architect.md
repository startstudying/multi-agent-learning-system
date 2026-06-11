# RUN - Backend Plan RAG Production Architect

## Summary

P3-2 不是空白状态：索引任务幂等/锁/worker/进度/恢复、token-window chunk、overlap、稳定 `chunkHash`、Markdown heading hierarchy 已完成。剩余生产化缺口集中在三块：生产级 parser/OCR、Embedding/VectorDB、hybrid/RRF/reranker，其中当前最适合先做的是 parser adapter 的最小生产切片。

## P3-2 当前判定

| 项目 | 判定 | 证据 |
|---|---|---|
| PDF/DOCX/Markdown/TXT 轻量解析雏形 | 已完成 | `IndexService` 根据 content type/name 选择 `MARKDOWN/TXT/PDF/DOCX`，并内置解析分支。 |
| 基础 chunk 清洗、去重、固定切分、Markdown 元数据 | 已完成 | token-ish window、overlap、heading path、hash、metadata 已在 `IndexService` 中实现。 |
| 生产级 parser adapter、复杂 PDF/DOCX、OCR fallback、真实页码/层级 | 未完成 | PDF 仅正则匹配 `Tj` 文本对象，DOCX 仅读 `word/document.xml` 的 `<w:t>`；`pom.xml` 无 Tika/PDFBox/POI/OCR 依赖。 |
| chunk token 切分、overlap、稳定 hash、heading hierarchy | 已完成但 tokenizer 仍过渡 | TODO 明确当前是无新依赖 `token-ish` 过渡方案。 |
| embedding service / VectorDB adapter | 未完成 | `EmbeddingService` 只返回 `noop-embedding-v1`；`kb_doc_chunk` schema 无 vector/embedding 字段。 |
| hybrid retrieval / RRF / reranker timeout fallback | 未完成 | 当前检索只取最近 20 个 chunk；`RerankerService` 原样返回。 |
| 索引任务幂等、锁、恢复、worker、详情 API | 已完成 | `createPendingTask`、worker、recovery scheduler、task detail API 已存在。 |

## Recommended Slices

| 顺序 | 切片 | 建议文件范围 | 新依赖 | 风险 | 测试命令 |
|---|---|---|---|---|---|
| 1 | Parser adapter / OCR 前置切片 | `rag/application/IndexService.java` 拆 parser 边界；新增 `rag/parser/**`；扩展 `IndexServiceTest`、`SchemaConvergenceMigrationTest` | 本轮不新增依赖；OCR 先定义 fallback 状态，不接重依赖 | 解析库体积、许可证、复杂 PDF 页码准确性、OCR 环境不可控 | `cd backend && mvn "-Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest" test` |
| 2 | Embedding / VectorDB adapter | `EmbeddingService.java`、新增 vector adapter 接口/实现、`KbDocChunk`/migration、`IndexService` embedding 阶段 | 可能需要 Spring AI embedding 或 VectorDB client | P3-3 模型边界未完成，容易绕过 `AiModelGateway`/模型日志治理 | `cd backend && mvn "-Dtest=IndexServiceTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest" test` |
| 3 | Hybrid / RRF / reranker fallback | `ChunkService.java`、`RagQueryService.java`、`RerankerService.java`、`RagProperties.java`、`RagQueryServiceTest` | reranker 如走模型可能需要模型 SDK；BM25 可无依赖或轻依赖 | 当前没有向量索引，先做 hybrid 会退化成关键词+时间排序；reranker 超时需避免破坏引用 | `cd backend && mvn "-Dtest=RagQueryServiceTest,ChatControllerTest" test`；全量 `cd backend && mvn test` |

## First Slice

本轮最适合先实现：**parser adapter 最小生产切片，不直接把 OCR 做满**。

原因：embedding 和 reranker 的质量上限取决于解析文本、页码、章节层级的可靠性；当前 PDF/DOCX 解析仍是内嵌轻量逻辑，复杂文档会直接污染后续向量索引。先抽出 parser adapter，可在不改变 RAG 查询 API 的前提下稳定索引输入，并为后续 OCR、embedding metadata、页码 citation 奠定边界。

## Architecture Drift Risks

| 风险 | 说明 |
|---|---|
| 依赖漂移 | 新 parser/OCR/VectorDB 依赖必须走依赖审查；本轮建议不新增依赖。 |
| 模型边界漂移 | Embedding/reranker 不应在业务服务里直接调用 provider SDK，应走集中治理边界。 |
| 权限漂移 | Retrieval 必须先过滤可读 KB，再查 chunk；当前 `RagQueryService` 已先 `requireReadableKbIds`。 |
| Citation 漂移 | RAG 答案必须带 sources，后续检索增强不能破坏引用。 |

## 验收标准

1. Parser adapter 后，TXT/Markdown/PDF/DOCX 均通过统一接口产出 `sections/page/headingPath/parser metadata`。
2. 复杂 PDF/DOCX 解析失败时写入安全错误码，不泄露原始 provider/文件内容。
3. worker/manual indexing 共用同一 parser/chunk 路径。
4. Markdown heading hierarchy 和稳定 `chunkHash` 现有测试继续通过。
5. 新依赖如引入，必须有 `docs/security/` 依赖审查和 MySQL/Flyway schema 验证。
