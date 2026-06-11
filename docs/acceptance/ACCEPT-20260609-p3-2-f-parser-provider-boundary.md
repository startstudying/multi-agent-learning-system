# Acceptance - P3-2-F Parser Provider Boundary + OCR Fallback Contract

## 1. 验收结论

状态：Accepted。

P3-2-F 已完成 parser provider boundary 与 disabled/noop OCR fallback contract。既有 Markdown/TXT/PDF/DOCX 轻量 parser 行为未回退，`IndexService` 仍只消费 `ParsedDocument/ParsedSection`，未新增依赖、API、DB schema、frontend 或 VectorDB/retrieval/citation 变更。

## 2. 验收清单

| 项 | 结果 | 证据 |
|---|---|---|
| PRD/REQ/SPEC/PLAN/TASK/Context 存在 | PASS | 对应 `docs/*/20260609-p3-2-f-*` 文件 |
| Subagent reports 存在 | PASS | `docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-*.md` |
| Dependency review 存在 | PASS | `docs/security/DEPENDENCY-REVIEW-20260609-p3-2-f-parser-provider-boundary.md` |
| RED 已验证 | PASS | `testCompile` 因缺少 contract/provider 失败 |
| `ParseInput` 已新增 | PASS | `backend/src/main/java/com/learningos/rag/parser/ParseInput.java` |
| `DocumentFormatParser` 已新增 | PASS | `backend/src/main/java/com/learningos/rag/parser/DocumentFormatParser.java` |
| `DocumentParserService` 使用 provider registry | PASS | provider list / `ObjectProvider` registry |
| OCR fallback contract 已新增 | PASS | `OcrFallbackService` / `OcrFallbackResult` |
| noop OCR disabled/no text | PASS | `NoopOcrFallbackServiceTest` |
| image-only PDF 不产生 section | PASS | `DocumentParserServiceTest` |
| parser failure 安全映射 | PASS | `DocumentParserServiceTest` / `IndexServiceParserFailureTest` |
| 不新增依赖/schema/API/frontend | PASS | `backend/pom.xml`、migration、frontend 未修改 |
| full backend verification | PASS | `mvn test`：`371 run, 0 failures, 0 errors, 1 skipped` |

## 3. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test
mvn test
```

结果：

- Focused：`19 run, 0 failures, 0 errors`
- Adjacent：`15 run, 0 failures, 0 errors`
- Extended adjacent：`49 run, 0 failures, 0 errors`
- Full backend：`371 run, 0 failures, 0 errors, 1 skipped`

## 4. 明确非验收范围

以下内容未完成，不能因本切片误标为完成：

- 真实 PDFBox / POI parser 接入。
- Tess4J/native OCR 或外部 OCR 服务接入。
- 复杂 PDF/DOCX 工业级页码、目录、章节层级识别。
- RAG VectorDB 真实客户端。
- P3-4 broader class/course 或 formal OAuth2/JWK/Spring Security。

## 5. 后续建议

下一步可继续 P3-2 的真实 parser/OCR 生产化切片，或转向 P3-4 broader class/course / formal OAuth2-JWK-Spring Security 权限矩阵。
