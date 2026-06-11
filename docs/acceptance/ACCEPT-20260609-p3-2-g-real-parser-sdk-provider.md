# Acceptance - P3-2-G Real PDF/DOCX Parser SDK Provider

## 1. 验收结论

状态：Accepted。

P3-2-G 已完成真实 PDFBox / POI DOCX parser provider 的最小生产化接入。Spring 上下文中的 `DocumentParserService` 可通过 `DocumentFormatParser` bean 覆盖默认 lightweight PDF/DOCX provider；无参构造仍保留 lightweight 行为以兼容既有单元测试。

本切片没有修改 `IndexService`、API、DB migration、frontend、retrieval/citation/VectorDB，也没有实现真实 OCR。

## 2. 验收清单

| 项 | 结果 | 证据 |
|---|---|---|
| PRD/REQ/SPEC/PLAN/TASK/Context 存在 | PASS | `docs/*/20260609-p3-2-g-*` |
| Dependency review 存在 | PASS | `docs/security/DEPENDENCY-REVIEW-20260609-p3-2-g-real-parser-sdk-provider.md` |
| Subagent 前置报告存在 | PASS | `docs/subagents/runs/RUN-20260609-p3-2-g-real-parser-sdk-provider-*.md` |
| RED 已验证 | PASS | `testCompile` 因缺少 PDFBox/POI/provider/limits 失败 |
| `PdfBoxDocumentFormatParser` 已新增 | PASS | `backend/src/main/java/com/learningos/rag/parser/PdfBoxDocumentFormatParser.java` |
| PDF provider 使用 PDFBox 读取真实 PDF | PASS | `RealParserProviderTest#pdfBoxProviderExtractsTextPerPageWithoutIndexingRawOperators` |
| PDF provider 按页保留 `pageNum` | PASS | focused tests `26/26` |
| PDF 无文本时不 fallback raw bytes | PASS | `RealParserProviderTest#pdfBoxProviderReturnsEmptySectionsForImageOnlyPdfWithNoopOcr` |
| `PoiDocxDocumentFormatParser` 已新增 | PASS | `backend/src/main/java/com/learningos/rag/parser/PoiDocxDocumentFormatParser.java` |
| DOCX provider 使用 POI 读取真实 DOCX | PASS | `RealParserProviderTest#poiDocxProviderExtractsHeadingHierarchyPageBreaksAndSeparators` |
| DOCX provider 识别 Heading/page break/tab/line break | PASS | focused tests `26/26` |
| 损坏/超限输入只暴露 `DOCUMENT_PARSE_FAILED` | PASS | `RealParserProviderTest` parser failure / oversized cases |
| `IndexService` 未修改 | PASS | Context Pack 禁止修改；adjacent `IndexServiceTest,IndexServiceParserFailureTest` 通过 |
| 无 DB/API/frontend 变更 | PASS | 本切片只涉及 `pom.xml`、`rag/parser/**`、测试和文档 |
| dependency tree 记录 | PASS | PDFBox `3.0.7`、POI `5.5.1` compile scope；`commons-logging` 无条目 |
| focused verification | PASS | `26 run, 0 failures, 0 errors` |
| adjacent verification | PASS | `15 run, 0 failures, 0 errors` |
| full backend verification | PASS | `378 run, 0 failures, 0 errors, 1 skipped` |

## 3. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,NoopOcrFallbackServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% dependency:tree -Dincludes=org.apache.pdfbox:pdfbox,org.apache.poi:poi-ooxml
mvn --% dependency:tree -Dincludes=commons-logging:commons-logging
mvn test
```

结果：

- Focused: `26 run, 0 failures, 0 errors, 0 skipped`
- Adjacent: `15 run, 0 failures, 0 errors, 0 skipped`
- Dependency tree: `pdfbox:3.0.7` / `poi-ooxml:5.5.1` present; `commons-logging` absent
- Full backend: `378 run, 0 failures, 0 errors, 1 skipped`

## 4. 明确非验收范围

以下内容仍未完成，不能因本切片被误标为完成：

- 真实 OCR fallback。
- 工业级 PDF 版面、目录、表格、阅读顺序恢复。
- 完整 DOCX 样式体系、页眉页脚、批注、表格结构恢复。
- real VectorDB client。
- P3-4 broader class/course 与 formal OAuth2/JWK/Spring Security。

## 5. 后续建议

下一步建议继续 P3-2 的真实 OCR fallback 或工业级 PDF/DOCX layout 细化；如果转向安全线，则继续 P3-4 broader class/course / formal OAuth2-JWK-Spring Security。
