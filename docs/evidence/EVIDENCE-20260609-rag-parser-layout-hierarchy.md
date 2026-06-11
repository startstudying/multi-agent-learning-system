# EVIDENCE - P3-2-E RAG Parser Layout / Page Hierarchy

## 1. 范围

本证据记录 P3-2-E 的无新增依赖 parser layout/page hierarchy 最小切片：

- PDF simple `/Type /Page` best-effort 分页。
- DOCX 同段 page break 拆 section。
- DOCX `w:tab` / 非分页 `w:br` 文本分隔。
- `IndexService` 继续把 `ParsedSection.pageNum()` 写入 chunk。

非目标：OCR fallback、真实 PDF/DOCX SDK、真实 VectorDB、API/DB/frontend/Spring Security 修改。

## 2. 代码证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java` | `parse(...)` 对 PDF 改走 `parsePdf(...)`；新增 simple page marker 分段；DOCX 按文本/tab/br/page-break token 输出 page-aware segments。 |
| `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java` | 覆盖 PDF simple multi-page、DOCX 同段 page break、DOCX tab / line break 分隔、PDF no-text 不 raw fallback。 |
| `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java` | 覆盖 multi-page PDF pageNum 进入 `KbDocChunk.pageNum`。 |

## 3. RED 证据

已在实现前运行新增测试并观察到预期失败：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest test
mvn --% -Dtest=IndexServiceTest#processSimpleMultiPagePdfPreservesPageNumbersInChunks test
```

关键失败原因：

- PDF 多页文本被合并为 1 个 section，`pageNum=1`。
- DOCX 同段 page break 前后文本被合并为 1 个 section。
- IndexService 只生成 1 个 PDF chunk，无法保留 page 1 / page 2。

## 4. GREEN / Regression 证据

### 4.1 Parser focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest test
```

结果：

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T01:14:10+08:00
```

### 4.2 Index page metadata focused

```powershell
mvn --% -Dtest=IndexServiceTest#processSimpleMultiPagePdfPreservesPageNumbersInChunks test
```

结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T01:14:50+08:00
```

### 4.3 Adjacent index/parser regression

```powershell
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
```

结果：

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T01:15:44+08:00
```

### 4.4 RAG adjacent regression

```powershell
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test
```

结果：

```text
Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T01:16:41+08:00
```

### 4.5 Full backend verification

```powershell
mvn test
```

结果：

```text
Tests run: 361, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-09T01:20:22+08:00
```

## 5. 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| 新增依赖 | PASS | 未修改 `backend/pom.xml`。 |
| DB schema | PASS | 未新增 migration。 |
| API / frontend | PASS | 未修改 controller / DTO / frontend。 |
| RAG parser 边界 | PASS | parser 增强集中在 `DocumentParserService`。 |
| 安全边界 | PASS | PDF 无文本仍不 fallback raw bytes；parser 异常仍走安全错误码。 |

## 6. 限制

- PDF 页码识别仍是 best-effort，只适合简单 `/Type /Page` marker 和 `Tj` / `TJ` 文本对象。
- DOCX 仍只读取 `word/document.xml`，不等价于完整 Word 排版解析。
- OCR fallback、复杂 PDF/DOCX SDK、真实 VectorDB 仍是后续独立任务。
