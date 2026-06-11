# Evidence - P3-2-F Parser Provider Boundary + OCR Fallback Contract

## 1. 范围

本证据覆盖 P3-2-F：RAG parser provider boundary 与 disabled/noop OCR fallback contract。

本切片未实现真实 PDFBox/POI/Tess4J/OCR，未新增依赖，未修改 API、DB schema、frontend、VectorDB 或 retrieval/citation contract。

## 2. 关键变更证据

代码：

- `backend/src/main/java/com/learningos/rag/parser/ParseInput.java`
- `backend/src/main/java/com/learningos/rag/parser/DocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackService.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackResult.java`
- `backend/src/main/java/com/learningos/rag/parser/NoopOcrFallbackService.java`
- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`

测试：

- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/NoopOcrFallbackServiceTest.java`

文档：

- `docs/product/PRD-20260609-p3-2-f-parser-provider-boundary.md`
- `docs/requirements/REQ-20260609-p3-2-f-parser-provider-boundary.md`
- `docs/specs/SPEC-20260609-p3-2-f-parser-provider-boundary.md`
- `docs/plans/PLAN-20260609-p3-2-f-parser-provider-boundary.md`
- `docs/tasks/TASK-20260609-p3-2-f-parser-provider-boundary.md`
- `docs/context/CONTEXT-20260609-p3-2-f-parser-provider-boundary.md`
- `docs/security/DEPENDENCY-REVIEW-20260609-p3-2-f-parser-provider-boundary.md`

## 3. RED 证据

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest test
```

结果：

- Exit code：`1`
- 失败阶段：`testCompile`
- 关键失败信号：
  - 缺少 `DocumentFormatParser`
  - 缺少 `ParseInput`
  - 缺少 `NoopOcrFallbackService`
  - 缺少 `OcrFallbackResult`
  - `DocumentParserService` 尚无 provider list 构造入口

结论：RED 失败原因符合预期，证明测试确实覆盖新 contract/provider 行为。

## 4. GREEN / Focused Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest test
```

结果：

- Exit code：`0`
- `DocumentParserServiceTest`：`18 run, 0 failures, 0 errors`
- `NoopOcrFallbackServiceTest`：`1 run, 0 failures, 0 errors`
- 合计：`19 run, 0 failures, 0 errors, 0 skipped`

覆盖点：

- `DocumentParserService` 通过 provider registry 调用 PDF provider。
- `ParseInput` 传递 document id/name/contentType/sizeBytes/bytes。
- provider runtime failure 映射为 `DOCUMENT_PARSE_FAILED`。
- image-only PDF 不产生 section。
- noop OCR 返回 `DISABLED / OCR_DISABLED / ""`。
- 既有 Markdown/TXT/PDF/DOCX 行为不回退。

## 5. Adjacent Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
```

结果：

- Exit code：`0`
- 合计：`15 run, 0 failures, 0 errors, 0 skipped`

覆盖点：

- `IndexService` 继续只消费 `ParsedDocument/ParsedSection`。
- scanned PDF 不产生 raw binary chunk，仍失败为 `DOCUMENT_EMPTY_OR_UNAVAILABLE`。
- parser failure 仍写安全错误码 `DOCUMENT_PARSE_FAILED`。
- chunk parser metadata、pageNum、headingPath 不回退。

## 6. Extended Adjacent Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test
```

结果：

- Exit code：`0`
- 合计：`49 run, 0 failures, 0 errors, 0 skipped`

覆盖点：

- parser provider boundary 未破坏 RAG query / retrieval / citation 邻近链路。

## 7. Full Backend Verification

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- Exit code：`0`
- 合计：`371 run, 0 failures, 0 errors, 1 skipped`

## 8. Architecture Drift Check

| Check | Status | Evidence |
|---|---|---|
| Backend layering | PASS | `IndexService` 未新增格式解析逻辑，parser 仍在 `rag/parser` |
| Frontend rules | PASS | 未修改 `frontend/**` |
| Agent / RAG rules | PASS | 未修改 retrieval/citation/trace contract |
| Security | PASS | 未新增依赖，未恢复 raw PDF fallback |
| API / Database | PASS | 未新增 API 或 DB migration |

## 9. 限制

- 真实 PDFBox/POI/Tess4J/OCR 未接入。
- 复杂 PDF/DOCX 工业级页码和章节识别仍待后续切片。
- 本切片只完成可替换 provider 边界和 disabled/noop OCR contract。
