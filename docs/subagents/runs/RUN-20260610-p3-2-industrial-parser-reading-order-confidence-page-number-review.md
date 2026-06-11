# RUN-20260610 P3-2 industrial parser metadata review

## 1. 背景

本轮子任务为 `P3-2 子任务：industrial parser reading-order/confidence/page-number metadata`，目标是在不新增依赖、不修改 API/DTO/DB/frontend 的前提下，为后续工业级 PDF/DOCX layout/table/TOC、native/cloud OCR 与真实页码映射补齐 parser metadata 承载层。

由于当前会话已达到新建 subagent 线程上限，主线程复用了现有专家线程执行 L1 并行只读复核：

- RAG/parser 专家：`019eb08d-b823-7d53-858b-d2a4f7f3dc8a`
- 测试/验收专家：`019eb08d-6e19-7370-99da-764ad17b9e62`

## 2. RAG/parser 专家结论

结论：建议验收该 M 切片，但必须明确它只是 parser metadata contract，不是工业级 layout/OCR 实现。

关键复核点：

- `ParsedSection` 增加 `pageNumSource / readingOrderIndex / layoutConfidence / ocrConfidence / contentKind`，且保留旧 5 参数构造器。
- `ParsedDocument` 只按 section 顺序补齐 1-based `readingOrderIndex`，未引入 layout 算法。
- `OcrFallbackResult` 增加 `confidence` 并归一化非法值。
- `ConfigurableOcrFallbackService` sanitize 后保留 `confidence`，失败/不可用仍清空 text。
- `PdfBoxDocumentFormatParser` 与 `DocumentParserService` 只在 OCR fallback 成功时写入 `OCR_FALLBACK / OCR_TEXT / ocrConfidence`。
- `IndexService` 只把短 metadata 写入 `metadataJson`，`layoutConfidence / ocrConfidence` 仅非空写入。

未发现：

- 新 Maven 依赖。
- HTTP API/DTO/DB schema/frontend/VectorDB/retrieval/citation contract 变更。
- 真实 layout/table/TOC 算法或 native/cloud OCR 接入。

专家建议记录的风险：

- `readingOrderIndex` 是 1-based，后续不得误按 0-based 使用。
- `pageNumSource=PARSER_INFERRED` 不等于真实渲染页码。
- chunk metadata 不得写入 `rawOcrText / providerResponse / stderr / path / secret`。

## 3. 测试/验收专家结论

结论：当前测试矩阵足以覆盖本 M 切片，前提是 focused、adjacent 和 full backend 均有新鲜验证证据。

测试覆盖判断：

- `DocumentParserServiceTest` 覆盖默认 metadata、reading order、`NONE/TEXT/null`。
- `RealParserProviderTest` 覆盖 OCR fallback 的 `OCR_FALLBACK / OCR_TEXT / ocrConfidence`。
- `IndexServiceTest` 覆盖 chunk metadata 输出 `pageNumSource / readingOrderIndex / contentKind`，并断言不包含 `layoutConfidence / ocrConfidence / rawOcrText / providerResponse`。
- `IndexServiceTest` DOCX 路径覆盖 `PARSER_INFERRED` 和 reading order。

建议：

- 可选增强：后续可给 `ConfigurableOcrFallbackServiceTest` 增加独立 confidence sanitize 测试；本切片已有 `OcrFallbackResult` 归一化和 OCR fallback 传递测试，不阻塞验收。
- full backend 若出现无关 SSE/MockMvc async race，需单独复现归因，并记录证据。

## 4. Integration Reviewer 决议

主线程采纳专家结论：

- 本切片按 M 级验收。
- 不关闭 P3-2 工业 parser 父项。
- Evidence/Acceptance 必须明确本切片只完成 metadata contract foundation。
- 第一次 full backend 出现 `SseProductionAuthStrategyTest` MockMvc async `ConcurrentModificationException` 时，不直接忽略；主线程单独复现 nested test 通过，再重跑 full backend 并通过后，才将 full backend 作为通过证据。

