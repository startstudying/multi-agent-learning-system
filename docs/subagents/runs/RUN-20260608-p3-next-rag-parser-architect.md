# RUN-20260608 P3 Next RAG Parser Architect

## 1. 当前 parser/index 代码入口和能力地图

结论：当前链路已经完整，但能力停在“轻量文本抽取 + best-effort 结构元数据”，不是“复杂 PDF/DOCX + OCR + 真实页码”。

- 上传入口：`DocumentController.upload -> DocumentService.upload -> IndexService.createPendingTask`。
- worker/manual 共用入口：`IndexTaskWorkerScheduler.runOnce` 调用 `IndexService.claimDuePendingTasks` / `processIndexTask`。
- parser 边界：`IndexService.processIndexTask` 读取存储对象后调用 `DocumentParserService.parse(document, bytes)`，然后进入 chunk/hash/metadata/embedding/vector 流水线。
- 当前能力：
  - Markdown：识别 `#` 到 `######` 并维护 `headingPath`。
  - TXT：单 section，空白和二进制文本安全失败。
  - PDF：只抽取简单 `Tj/TJ` 文本，`pageNum` 固定为 `1`，没有 page tree / OCR / layout。
  - DOCX：只读 `word/document.xml`，依赖 `<w:t>` 拼文本，`Heading1-6` 与 page break 为 best-effort。
- `KbDocChunk` 已有 `pageNum`、`sectionTitle`、`chunkHash`、`metadataJson` 字段，`IndexService` 已写入 parser/heading/pageNum 元数据。

## 2. 不新增依赖 vs 新增依赖

| 方案 | 优点 | 缺点 |
|---|---|---|
| 不新增依赖 | 风险低、改动小、无需 dependency review、不改 query/citation API | 只能 best-effort，无法真正完成 scanned PDF、OCR、真实页树页码 |
| 新增依赖 | 可接近真实 PDF/DOCX/OCR 目标 | 需要 dependency review，测试/安全/部署面扩大 |

## 3. 推荐最小可验收切片

推荐先做“不新增依赖”的 parser 结构硬化切片，但不能宣称“真实 OCR / 真实页码已完成”。

验收边界：

- DOCX：强化 `Heading1-6` 章节树和 page break best-effort `pageNum`。
- PDF：明确只支持文本对象抽取；扫描件/无文本对象安全空结果或 parse failure，不做 raw bytes fallback。
- TXT/Markdown：保留安全失败与 heading hierarchy。
- chunk/citation：继续沿用现有 `pageNum`、`sectionTitle`、`metadataJson` 链路。

## 4. 允许修改文件建议

- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`
- `backend/src/main/java/com/learningos/rag/parser/ParsedSection.java`（仅在需要补充元数据时）
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java`

不建议本切片修改 `RagQueryService`、`ChunkService`、`DocumentController` 或数据库 schema。

## 5. 测试建议

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest,IndexTaskWorkerSchedulerTest test
```

覆盖：

- DOCX heading/page break。
- PDF `Tj/TJ` 正常抽取。
- PDF 无文本对象不回退 raw binary。
- DOCX malformed zip / oversized / unsupported zip 安全失败。
- parser 的 `pageNum` / `headingPath` 能继续落到 chunk。

## 6. 是否需要依赖审查

- 不新增依赖：不需要。
- 新增依赖：需要，且是前置条件。`rag-parser-boundary` 明确新增 parser/OCR 依赖前必须做 dependency review。

## 7. 根因

根因不是缺少 parser 入口，而是当前 parser 只实现 JDK 级轻量抽取：PDF 无 page tree / OCR，DOCX 只看 `document.xml`。在当前依赖边界内不能真正完成“复杂 PDF/DOCX、OCR fallback、真实页码”。
