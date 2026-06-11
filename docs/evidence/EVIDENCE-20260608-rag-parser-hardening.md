# Evidence - P3-2-C RAG 无依赖 Parser 加固

## 1. 证据摘要

本切片完成 RAG parser 的 no-dependency hardening：

- PDF 不再在文本 operator 抽取失败时 fallback 原始 `%PDF` / object stream。
- PDF 支持 `Tj` 与简单 `TJ` array 抽取，并保留最小 escape 解码。
- DOCX 通过 EOCD / central directory 统计 entry 数并定位 `word/document.xml`，只读取目标 entry，不解压非目标 entry body。
- DOCX 对 entry count、`word/document.xml` 解压后大小、malformed zip、ZIP64、分卷、加密、central/local name mismatch 做安全失败。
- DOCX 解析 `<w:p>`、`Heading1`-`Heading6`、page break，并输出 best-effort `headingPath` / `pageNum`。
- TXT/Markdown 对 malformed UTF-8、NUL、明显 binary/control garbage 安全失败。
- parser failure 经 `IndexService` 只持久化 safe code，不记录 raw cause/path/secret/original text。

## 2. TDD RED 证据

### 2.1 初始 parser hardening RED

命令：

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
```

结果：失败符合预期，覆盖以下缺失能力：

- PDF raw fallback 会把不可解析 PDF 当文本。
- PDF `TJ` array 未按目标抽取。
- DOCX heading/page-break metadata 缺失。
- DOCX XML/entry limit 缺失。
- Markdown BOM / TXT binary 拒绝缺失。

### 2.2 Code review 后 ZIP 非目标 entry inflate RED

命令：

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
```

结果：

```text
DocumentParserServiceTest.docxDoesNotInflateNonDocumentEntryBodiesAfterDocumentXml
DocumentParseException: DOCUMENT_PARSE_FAILED
Caused by: java.util.zip.ZipException: invalid block type
```

说明：旧实现为统计后续 entries 使用 `ZipInputStream#getNextEntry()`，会隐式消费/解压非目标 entry body；破坏的非目标 entry 导致 parser 失败，验证 reviewer 指出的资源边界问题真实存在。

### 2.3 Local header name mismatch RED

命令：

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
```

结果：

```text
DocumentParserServiceTest.docxMalformedZipAndTooManyEntriesFailWithSafeParserCode
Expecting code to raise a throwable.
```

说明：central directory 名称伪造成 `word/document.xml`、local header 名称不一致时，修复前未失败；随后增加 local header name 校验并转为 GREEN。

## 3. Fresh GREEN 验证

### 3.1 Parser 聚焦测试

命令：

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
```

结果：

```text
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-08T11:38:46+08:00
```

覆盖点：

- Markdown heading hierarchy、BOM、binary 拒绝。
- TXT blank skip、binary 拒绝。
- PDF no raw fallback、`Tj`、`TJ`、escape 解码、`pageNum=1`。
- DOCX paragraph、headingPath、page break、多 page break。
- DOCX malformed zip、`PK` 截断、entry count、XML size limit。
- DOCX `document.xml` 后 entries 超限。
- DOCX 不解压损坏的非目标 entry body。
- DOCX central directory / local header name mismatch 安全失败。

### 3.2 IndexService 相邻回归

命令：

```powershell
cd backend
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
```

结果：

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-08T11:39:43+08:00
```

覆盖点：

- 无文本 PDF 不生成垃圾 chunk。
- DOCX `pageNum` / `headingPath` / parser metadata 回写 chunk。
- parser failure 只保存 `DOCUMENT_PARSE_FAILED`，不泄露 raw cause、路径、secret 或原文片段。

### 3.3 RAG/index 相关回归

命令：

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest test
```

结果：

```text
Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-08T11:40:50+08:00
```

### 3.4 Dependency drift 检查

命令：

```powershell
cd backend
mvn --% dependency:tree -Dscope=compile
```

结果：

```text
BUILD SUCCESS
Finished at: 2026-06-08T11:41:09+08:00
```

结论：未新增 PDFBox、POI、Tika、docx4j、iText、Tesseract、OCR SDK 等 parser/OCR 依赖；`backend/pom.xml` 未修改。

### 3.5 全量后端测试

命令：

```powershell
cd backend
mvn test
```

结果：

```text
Tests run: 298, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-08T11:44:24+08:00
```

说明：Maven 输出包含既有 Mockito / ByteBuddy dynamic agent warning；本切片未新增该行为。

## 4. Code Review 证据

Reviewer：Feynman (`019ea536-53c0-75d2-a2bc-f5d0723c378b`)

| 轮次 | 结论 | 处理 |
|---|---|---|
| 初审 | REQUEST CHANGES | 修复 `PK` 截断 zip、document.xml 后续 entries 超限、无 `<w:p>` fallback |
| 复审 | REQUEST CHANGES | 修复统计 entries 时隐式解压非目标 entry body 的资源滥用风险；改为 EOCD/central directory 定位 |
| 终审 | APPROVE | 补齐 local header name 校验后，无 Critical / Important / Minor 遗留 |

最终 reviewer 结论：

```text
Critical: 无
Important: 无
Minor: 无新的必须处理项
Assessment: APPROVE
```

## 5. 需求到证据映射

| 需求 | 证据 | 状态 |
|---|---|---|
| FR-01 PDF 不 fallback raw binary | `pdfWithoutTextObjectsDoesNotFallbackToRawBinaryContent`；`IndexServiceTest` 空 PDF 回归 | PASS |
| FR-02 PDF `Tj` / `TJ` | `selectsPdfAndDocxParsersWithCurrentLightweightExtraction`、`pdfExtractsSimpleTextArrayWithoutIndexingOperators` | PASS |
| FR-03 DOCX entry/XML limit | `docxOversizedDocumentXmlFailsWithSafeParserCode`、`docxMalformedZipAndTooManyEntriesFailWithSafeParserCode` | PASS |
| FR-04 DOCX heading | `docxParsesHeadingParagraphsAndPageBreaksBestEffort` | PASS |
| FR-05 DOCX page break | `docxParsesHeadingParagraphsAndPageBreaksBestEffort`、`docxCountsMultiplePageBreaksInSingleParagraph` | PASS |
| FR-06 TXT/Markdown binary 拒绝 | `markdownBomStillRecognizesFirstHeadingAndBinaryTextIsRejected` | PASS |
| FR-07 parser failure 不泄露 raw cause | `IndexServiceParserFailureTest` | PASS |
| FR-08 parser metadata 写入 chunk | `IndexServiceTest` | PASS |
| NFR-01 无新 dependency | `mvn --% dependency:tree -Dscope=compile` | PASS |
| NFR-02 无 DB schema | `SchemaConvergenceMigrationTest` 相关回归通过；未修改 migrations | PASS |
| NFR-03 无 API/frontend | 未触达 controller/frontend；全量测试通过 | PASS |
| NFR-04 parser 资源上限 | DOCX central directory count + target XML size limit + no non-target inflate | PASS |
| NFR-05 RED/GREEN/full 回归 | 本 Evidence 第 2、3 节 | PASS |

## 6. 依赖 / Schema / Frontend / API Gate

| Gate | 结果 | 说明 |
|---|---|---|
| Dependency | PASS | 未修改 `backend/pom.xml`，未新增 parser/OCR dependency。 |
| DB schema | PASS | 未新增或修改 `backend/src/main/resources/db/migration/**`。 |
| Frontend | PASS | 未修改 `frontend/**`。 |
| Public API | PASS | 未修改公开 API contract。 |
| Secrets | PASS | 未新增 API key、secret、credential 配置或日志。 |

## 7. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 格式解析保持在 `rag/parser`；`IndexService` 只消费 `ParsedDocument` / `ParsedSection`。 |
| Frontend rules | PASS | 未修改 frontend；未引入前端 LLM/parser 调用。 |
| Agent / RAG rules | PASS | 未改变 retrieval/citation/trace；parser 空内容仍由索引层安全失败。 |
| Security | PASS | 不新增依赖；DOCX 不解压非目标 entry body；parser failure safe code 持久化。 |
| API / Database | PASS | 无 endpoint/schema 变更。 |

## 8. 剩余边界

- 无依赖 PDF 解析仍不能处理 compressed streams、CMap、字体编码、扫描件、真实页码。
- DOCX regex 解析仍不能完整覆盖 header/footer、footnote、textbox、复杂表格、样式继承、修订痕迹。
- 真实 OCR、真实 PDF/DOCX SDK、真实页码与章节增强仍需单独 PRD/REQ/SPEC/PLAN/TASK，并先完成 dependency/security review。
- 全量 dependency CVE audit 插件未在本仓库配置；本切片仅完成 dependency drift 检查。
