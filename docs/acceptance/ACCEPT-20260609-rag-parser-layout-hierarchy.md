# ACCEPT - P3-2-E RAG Parser Layout / Page Hierarchy

## 1. 验收结论

P3-2-E 本切片验收通过。

本次只验收无新增依赖的 parser layout/page hierarchy 最小增强，不代表复杂 PDF/DOCX、OCR fallback 或真实 VectorDB 已完成。

## 2. 需求验收

| 需求 | 状态 | 证据 |
|---|---|---|
| FR-01 PDF simple multi-page 输出不同 `pageNum` | 通过 | `pdfExtractsSimplePageBoundariesBestEffort`、`processSimpleMultiPagePdfPreservesPageNumbersInChunks` |
| FR-02 PDF 无文本对象不 raw fallback | 通过 | `pdfWithoutTextObjectsDoesNotFallbackToRawBinaryContent` |
| FR-03 DOCX `w:tab` 文本分隔 | 通过 | `docxPreservesTabAndLineBreakSeparators` |
| FR-04 DOCX 非分页 `w:br` 文本分隔 | 通过 | `docxPreservesTabAndLineBreakSeparators` |
| FR-05 DOCX page break 递增 `pageNum` | 通过 | `docxSplitsTextAroundPageBreakInsideParagraph`、既有 page-break 测试 |
| FR-06 chunk 保留 parser page metadata | 通过 | `processSimpleMultiPagePdfPreservesPageNumbersInChunks` |

## 3. 非功能验收

| 非功能项 | 状态 | 说明 |
|---|---|---|
| 不新增 Maven dependency | 通过 | 未修改 `backend/pom.xml` |
| 不修改 DB schema / migration | 通过 | 未新增 migration |
| 不修改公开 API / frontend | 通过 | 未触碰 controller / frontend |
| 不持久化 raw parser error / storage key / secret | 通过 | 仍只输出 section content/page metadata；错误码保持安全 |
| focused / adjacent / full verification | 通过 | 详见 evidence |

## 4. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest test
mvn --% -Dtest=IndexServiceTest#processSimpleMultiPagePdfPreservesPageNumbersInChunks test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test
mvn test
```

最终全量结果：

```text
Tests run: 361, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. 未验收 / 后续项

- OCR fallback。
- 真实 PDF/DOCX parser SDK。
- 复杂 PDF 真实页码、章节层级和版面结构。
- 真实 VectorDB adapter。
- P3-4 broader class/course RBAC、正式 OAuth2/JWK/Spring Security、完整权限渗透测试矩阵。
