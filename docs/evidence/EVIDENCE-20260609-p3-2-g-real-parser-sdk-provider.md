# Evidence - P3-2-G Real PDF/DOCX Parser SDK Provider

## 1. 范围

本证据覆盖 P3-2-G：在 P3-2-F `DocumentFormatParser` provider boundary 后接入真实 Apache PDFBox / Apache POI DOCX provider。

本切片已完成：

- 新增 `PdfBoxDocumentFormatParser`。
- 新增 `PoiDocxDocumentFormatParser`。
- 新增 parser 资源限制与文本清洗 helper。
- 新增 Maven 依赖 `org.apache.pdfbox:pdfbox:3.0.7` 与 `org.apache.poi:poi-ooxml:5.5.1`。
- Spring 上下文中的 `DocumentParserService` 可通过 provider registry 覆盖默认 lightweight PDF/DOCX provider。

本切片未完成、也不得声明完成：

- 真实 OCR fallback。
- 工业级 PDF 版面重建、目录识别、表格结构恢复。
- 完整 DOCX 样式体系还原。
- real VectorDB client。
- API / DB / frontend / retrieval / citation 合同变更。

## 2. 关键变更证据

代码：

- `backend/pom.xml`
- `backend/src/main/java/com/learningos/rag/parser/ParserResourceLimits.java`
- `backend/src/main/java/com/learningos/rag/parser/ParserTextSanitizer.java`
- `backend/src/main/java/com/learningos/rag/parser/PdfBoxDocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/PoiDocxDocumentFormatParser.java`

测试：

- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/NoopOcrFallbackServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java`

文档：

- `docs/product/PRD-20260609-p3-2-g-real-parser-sdk-provider.md`
- `docs/requirements/REQ-20260609-p3-2-g-real-parser-sdk-provider.md`
- `docs/specs/SPEC-20260609-p3-2-g-real-parser-sdk-provider.md`
- `docs/plans/PLAN-20260609-p3-2-g-real-parser-sdk-provider.md`
- `docs/tasks/TASK-20260609-p3-2-g-real-parser-sdk-provider.md`
- `docs/context/CONTEXT-20260609-p3-2-g-real-parser-sdk-provider.md`
- `docs/security/DEPENDENCY-REVIEW-20260609-p3-2-g-real-parser-sdk-provider.md`

Subagent 前置报告：

- `docs/subagents/runs/RUN-20260609-p3-2-g-real-parser-sdk-provider-architecture.md`
- `docs/subagents/runs/RUN-20260609-p3-2-g-real-parser-sdk-provider-dependency.md`
- `docs/subagents/runs/RUN-20260609-p3-2-g-real-parser-sdk-provider-test.md`
- `docs/subagents/runs/RUN-20260609-p3-2-g-real-parser-sdk-provider-integration.md`

说明：收尾阶段额外启动的两个只读复核 subagent 因 `stream disconnected before completion` 断线，未产出可用结论，因此不作为验收证据。

## 3. RED 证据

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,NoopOcrFallbackServiceTest test
```

结果：

- Exit code: `1`
- 失败阶段：`testCompile`
- 关键失败信号：
  - 缺少 `org.apache.pdfbox`。
  - 缺少 `org.apache.poi`。
  - 缺少 `PdfBoxDocumentFormatParser`。
  - 缺少 `PoiDocxDocumentFormatParser`。
  - 缺少 `ParserResourceLimits`。

结论：RED 失败原因符合预期，证明新增测试确实覆盖真实 parser provider 与依赖接入行为。

## 4. Focused Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,NoopOcrFallbackServiceTest test
```

结果：

- Exit code: `0`
- `DocumentParserServiceTest`: `18 run, 0 failures, 0 errors`
- `NoopOcrFallbackServiceTest`: `1 run, 0 failures, 0 errors`
- `RealParserProviderTest`: `7 run, 0 failures, 0 errors`
- 合计：`26 run, 0 failures, 0 errors, 0 skipped`
- Finished at: `2026-06-09T10:40:28+08:00`

覆盖点：

- PDFBox provider 按页提取真实 PDF 文本并保留 `pageNum`。
- PDFBox provider 不索引 `BT` / `Tj` / `%PDF` 等 raw operator。
- 空 PDF 在 noop OCR 下返回空 sections。
- 损坏 PDF 映射为 `DOCUMENT_PARSE_FAILED`。
- POI provider 提取真实 DOCX 段落、Heading1/Heading2、headingPath、page break、tab、line break。
- 损坏 DOCX 映射为 `DOCUMENT_PARSE_FAILED`。
- 超限 PDF/DOCX 输入映射为 `DOCUMENT_PARSE_FAILED`。
- 超长 OCR fallback 文本映射为 `DOCUMENT_PARSE_FAILED`。
- P3-2-F lightweight / noop OCR 行为未回归。

## 5. Adjacent Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
```

结果：

- Exit code: `0`
- 合计：`15 run, 0 failures, 0 errors, 0 skipped`
- Finished at: `2026-06-09T10:41:18+08:00`

覆盖点：

- `IndexService` 继续只消费 `ParsedDocument/ParsedSection`。
- parser failure 继续落到安全错误码。
- PDF/DOCX provider 覆盖没有破坏索引任务与 chunk 持久化邻近路径。

## 6. Dependency Tree Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% dependency:tree -Dincludes=org.apache.pdfbox:pdfbox,org.apache.poi:poi-ooxml
```

结果：

- Exit code: `0`
- `org.apache.pdfbox:pdfbox:jar:3.0.7:compile`
- `org.apache.poi:poi-ooxml:jar:5.5.1:compile`
- Finished at: `2026-06-09T10:42:45+08:00`

命令：

```powershell
cd D:\多元agent\backend
mvn --% dependency:tree -Dincludes=commons-logging:commons-logging
```

结果：

- Exit code: `0`
- dependency tree 无 `commons-logging:commons-logging` 条目。
- Finished at: `2026-06-09T10:43:03+08:00`

结论：目标依赖版本已进入 compile scope；PDFBox 的 `commons-logging` transitive dependency 已被排除，避免与 Spring JCL 冲突。

## 7. Full Backend Verification

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- Exit code: `0`
- 合计：`378 run, 0 failures, 0 errors, 1 skipped`
- Finished at: `2026-06-09T10:45:29+08:00`

备注：测试日志中仍有 Mockito dynamic agent 的 JDK 未来兼容 warning，属于既有测试工具链提示，不是本切片引入的失败。

## 8. Architecture Drift Check

| Check | Status | Evidence |
|---|---|---|
| Backend layering | PASS | PDFBox/POI 只在 `rag/parser` provider 内使用；`IndexService` 未参与格式解析 |
| Frontend rules | PASS | 未修改 `frontend/**` |
| Agent / RAG rules | PASS | 未修改 retrieval/citation/trace/VectorDB 合同 |
| Security | PASS WITH CONDITIONS | 新依赖有 dependency review；provider 有输入、页数、段落、输出字符限制；未运行独立 SCA |
| API / Database | PASS | 未新增 API、未修改 DB migration |

## 9. 限制与风险

- 本地工作区根目录不是 git repository，无法用 `git status` / `git diff` 作为改动范围证据；本证据以文件清单、源码读取和 Maven 输出为准。
- Full SCA 未运行；dependency review 使用 Apache 官方安全页、Maven metadata、dependency tree 和测试作为本切片证据。
- POI/PDFBox 面向不可信文件仍可能带来 CPU/内存压力；当前切片采用保守输入与输出限制，后续如支持更大文件应增加更细粒度超时/隔离策略。
- 真实 OCR fallback、工业级 PDF/DOCX layout、real VectorDB 仍是后续 P3-2 工作。
