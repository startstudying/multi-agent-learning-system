# RUN - P3-2 industrial parser open item plan

## 1. 结论

P3-2 未完成项 `工业级 PDF/DOCX layout/table/TOC/reading-order、native/cloud OCR、OCR confidence 与真实渲染页码增强` 不建议在当前迭代一次性实现。

原因：

- 当前 `ParsedSection` 只承载 `title / headingLevel / headingPath / content / pageNum`，没有表格、目录、阅读顺序、布局坐标、OCR confidence、页码来源等结构化字段。
- 当前 `OcrFallbackResult` 只承载 `status / reasonCode / text`，没有页级 OCR 结果、confidence、provider metadata、渲染页码映射。
- PDFBox provider 当前按页抽取文本；POI provider 当前按段落、Heading1-6、显式 page break 抽取。它们是生产化基础能力，不是工业级 layout reconstruction。
- native/cloud OCR 与工业 layout provider 都涉及新增依赖、外部服务、成本、隐私、超时、限流、许可和安全审查，不能作为一个无边界大改合并。

推荐把该项作为 L 级父能力，拆成多个 M 子任务推进；第一切片优先做 parser 输出契约和测试夹具，不直接接入云 OCR 或重型 layout SDK。

## 2. Skill Selection Report

| 项目 | 选择 |
|---|---|
| Task type | RAG / retrieval backend architecture planning |
| Selected skills | `feature-development-workflow`, `rag-parser-boundary`, `security-review`, `dependency-review`, `test-generator`, `architecture-drift-check` |
| Why | 该项属于 RAG parser 生产化能力扩展，涉及依赖、安全、测试与架构边界 |
| Missing skills | 无；若后续选择具体云 OCR，可补充 provider-specific 研究 |
| GitHub research needed | 后续 provider 选型需要；本轮拆分规划不需要 |
| New project-specific skill | 暂不需要；等完成第一个工业 parser 契约切片后再沉淀 |

## 3. Size Classification

整体能力：L。

理由：

- 涉及 `rag/parser` 输出模型、OCR SPI、PDF/DOCX provider、IndexService chunk metadata、测试 fixtures、可能的 DB/API/依赖变化。
- native/cloud OCR 涉及外部依赖与安全审查。
- 工业级 table/TOC/reading-order 不是现有 provider 的小修补，而是新能力层。

本轮不建议直接实现整体 L。建议先拆为可验收 M/S 子任务。

## 4. Subagent Decision

| 项目 | 选择 |
|---|---|
| Use subagents | Yes, for future L planning/design |
| Parallelism level | L1/L2：并行分析与设计 |
| Selected subagents | RAG parser/backend、Security & Dependency、Test Engineering、Integration Reviewer |
| Implementation mode | 单任务顺序实现；不建议并行改同一 parser contract |

## 5. 当前代码边界证据

- `backend/src/main/java/com/learningos/rag/parser/ParsedSection.java`：只有标题、层级、路径、内容、页码。
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackResult.java`：只有状态、原因码、文本。
- `backend/src/main/java/com/learningos/rag/parser/PdfBoxDocumentFormatParser.java`：PDFBox 按页抽取文本；无布局坐标、表格、目录、渲染页码映射。
- `backend/src/main/java/com/learningos/rag/parser/PoiDocxDocumentFormatParser.java`：POI 抽取 paragraph、Heading1-6、显式 page break、tab/line break；无表格结构、目录结构、复杂 reading order。
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`：chunk 只消费 `ParsedSection` 的 content、heading、pageNum。
- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`：覆盖 PDF 按页文本、DOCX heading/page break、OCR fallback 文本。
- `backend/src/test/java/com/learningos/rag/parser/ProcessOcrFallbackProviderTest.java`：覆盖 process OCR 的安全失败、超时、输出限制。

## 6. 推荐拆分

### M1：p3-2-industrial-parser-reading-order-confidence-page-number

目标：先定义增强 parser 输出契约和测试夹具，不接入新 OCR/cloud SDK。

范围：

- 扩展或新增 parser metadata 模型，用于承载 `pageNumSource`、`readingOrderIndex`、`layoutConfidence`、`ocrConfidence`、`contentKind`。
- 不改变公开 HTTP API。
- 不引入新 Maven 依赖。
- `IndexService` 只把安全、短字段写入 chunk metadata，不写 raw OCR/provider/private payload。
- 增加 contract tests，证明旧 parser 输出仍兼容，新 metadata 可被 chunk metadata 安全保留。

验收测试：

- `ParsedSectionMetadataTest` 或 `DocumentParserServiceTest`：默认 metadata 为空/安全默认值。
- `IndexServiceTest`：chunk metadata 包含 page source / reading-order / confidence 的短字段，且不包含 raw OCR 文本、provider secrets。
- `RealParserProviderTest`：PDFBox/POI provider 在未提供增强 metadata 时仍保持现有行为。

风险：需要谨慎处理 record 构造器兼容性，避免大范围测试改动。

### M2：p3-2-pdf-layout-table-toc-reading-order-provider

目标：在 PDF provider 内增强 layout/table/TOC/reading-order，仍限定在 `rag/parser`。

范围：

- 基于 PDFBox 可用能力先实现 deterministic reading order 与 TOC/bookmark 提取。
- 表格只做保守识别：输出为文本块或 `contentKind=TABLE_TEXT`，不承诺完整表格结构还原。
- 不接云 OCR。

需要依赖审查：

- 若继续使用已引入 PDFBox，无新增依赖审查。
- 若引入 Tabula/Camelot/商业 SDK，必须新增 `docs/security/DEPENDENCY-REVIEW-*`。

验收测试：

- 多列 PDF reading-order fixture。
- PDF bookmark/outline -> TOC metadata fixture。
- 表格文本保持可检索且不错序，不暴露 raw PDF operators。

风险：真实 PDF layout 差异大，测试 fixture 需要固定、可重复。

### M3：p3-2-docx-table-toc-reading-order-provider

目标：增强 DOCX 表格、目录、段落阅读顺序。

范围：

- 使用现有 POI。
- 抽取 `XWPFTable` 为稳定文本块，保留行列顺序。
- 识别 TOC/目录字段时只输出安全标题文本或 metadata，不生成空 chunk。

需要依赖审查：无新增依赖时不需要。

验收测试：

- DOCX table rows/cells 顺序 fixture。
- DOCX heading + table + paragraph reading order fixture。
- DOCX TOC 字段不污染正文 chunk。

风险：POI 文档对象顺序与复杂 Word layout 不完全等价，需要明确 best-effort。

### M4：p3-2-native-ocr-provider-confidence

目标：接入 native OCR provider，并返回页级 OCR confidence。

范围：

- provider 仍实现 `OcrFallbackProvider` 或新增 page-aware OCR SPI。
- 默认关闭。
- 返回结构化页级结果：page number、text、confidence、safe reason code。
- 不持久化 raw OCR response、stderr、路径、secret。

需要依赖审查：

- Tess4J/JNA、Tesseract native runtime 或其他 native binding。
- 许可证、维护状态、CVE、native binary 分发、CI 可用性。

验收测试：

- provider disabled/unavailable/failed/succeeded。
- confidence 超界归一化或拒绝。
- native runtime 缺失安全失败。
- image-only PDF OCR 结果生成 `ocrConfidence` metadata。

风险：CI/native runtime 不稳定，必须允许 mock provider 与真实 smoke 分离。

### M5：p3-2-cloud-ocr-provider-confidence

目标：接入 cloud OCR provider，但默认关闭且不需要本地测试 secrets。

范围：

- provider behind `OcrFallbackProvider` / page-aware OCR SPI。
- 明确配置项、超时、大小限制、成本保护、PII/内容出境风险。
- 单元测试使用 fake client，不调用真实云。
- 真实云 smoke 仅 opt-in。

需要依赖审查：

- 云 SDK、传输依赖、许可证、维护状态、CVE、凭据配置方式。
- 安全评审需说明文档内容出境、日志脱敏、失败码。

验收测试：

- missing credential -> unavailable。
- timeout/rate limit/provider error -> safe failed。
- successful page OCR -> confidence metadata。
- no raw provider response in logs/result/chunk metadata。

风险：隐私、成本、网络不稳定、供应商锁定。

### M6：p3-2-rendered-page-number-mapping

目标：增强真实渲染页码与 `pageNum` 的映射质量。

范围：

- 区分 physical page index 与 rendered page label。
- PDF 可先读取 page labels；DOCX 可继续标记为 inferred/page-break based。
- Chunk metadata 保存短字段：`pageNum`, `pageLabel`, `pageNumSource`。

需要依赖审查：若只用 PDFBox/POI 无新增。

验收测试：

- PDF page labels fixture。
- DOCX inferred page break source fixture。
- RAG citation response仍使用兼容 `pageNum`，不破坏现有 API。

风险：如果 API 要暴露 `pageLabel`，需另开 API/DTO spec；第一步只进 chunk metadata。

## 7. 第一切片建议

建议第一切片：`p3-2-industrial-parser-reading-order-confidence-page-number`。

理由：

- 不新增依赖，风险最低。
- 先解决承载模型不足，避免后续 OCR/layout provider 把结构化信息塞进 content 字符串。
- 可用 focused parser/index tests 闭环。
- 不改变公开 API/DB schema 时可做 M；若需要新增 chunk DB 字段则升级 L。

第一切片需要文档：

- `docs/requirements/REQ-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`
- `docs/specs/SPEC-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`
- `docs/plans/PLAN-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`
- `docs/tasks/TASK-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`
- `docs/context/CONTEXT-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`

是否需要 PRD：不需要，若仅为后端 parser/index metadata 契约，不改变用户工作流。

是否需要依赖审查：第一切片不需要；M2/M3 若不新增依赖也不需要；M4/M5 必须需要。

## 8. 推荐命令

Focused：

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ProcessOcrFallbackProviderTest test
```

Adjacent：

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
```

Dependency checks for future OCR/cloud slices：

```powershell
cd backend
mvn --% dependency:tree -Dincludes=net.sourceforge.tess4j:tess4j,net.java.dev.jna:jna,org.bytedeco:*,com.aliyun:*,com.google.cloud:google-cloud-vision,software.amazon.awssdk:textract
```

Full backend：

```powershell
cd backend
mvn test
```

## 9. 本轮建议

本轮不建议实现整体工业 parser/OCR 能力。

若必须开始实现，建议只做第一切片的文档与测试先行；生产代码仅允许修改 parser contract、metadata 映射和对应 focused tests，且不得新增依赖、不得改公开 API、不得改 DB schema。
