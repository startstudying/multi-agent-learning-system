# P3-2-C no-dependency parser hardening 安全与质量审查报告

**角色:** Security & Quality Expert  
**日期:** 2026-06-08  
**范围:** RAG 文档解析与索引边界，重点审查 `DocumentParserService`、`IndexService` parser 消费路径、parser/index 相关测试、依赖/API/schema 漂移。  
**结论:** **有条件通过，需补齐高风险 ZIP/DOCX 防护与二进制垃圾拒绝测试后再验收。**

## 已检查上下文

- `AGENTS.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/skills/project-specific/rag-parser-boundary.md`
- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexTaskWorkerSchedulerTest.java`
- `backend/pom.xml`

## 验证命令

- `rg` secrets scan：未发现 parser/index 生产代码中的真实硬编码密钥；命中主要为测试用例中的假 secret/token 与配置字段名。
- `mvn --% dependency:tree -Dscope=compile`：通过；未发现 Apache Tika/POI/OCR 等新增 parser 依赖。
- `mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest test`：通过，34 tests, 0 failures。
- `rg "(dependency-check|owasp|audit|snyk|cyclonedx)" ...`：未发现已配置的 Maven CVE audit 插件；本次只做 dependency tree 漂移检查，未完成 CVE 数据库审计。

备注：工作目录不是 git repository，`git status` / `git diff` 无法用于确认改动边界。

## 风险清单

### 1. DOCX ZIP 解压缺少大小与条目限制，存在压缩炸弹/内存 DoS 风险

**严重级别:** HIGH  
**OWASP:** A05 Security Misconfiguration / A04 Insecure Design  
**位置:** `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java:124-137`  
**证据:** `extractDocxText` 使用 `ZipInputStream` 遍历条目，并对 `word/document.xml` 直接 `zip.readAllBytes()`；未限制总压缩前大小、解压后大小、条目数量、单条目大小或读取字符数。  
**利用条件:** 可上传 DOCX 的已认证用户；若共享/课程 KB 由教师上传，影响课程 RAG 索引 worker。  
**Blast Radius:** worker JVM 内存压力、索引线程阻塞、任务反复失败重试，影响同一后端实例的 RAG 索引可用性。  
**建议修复示例:**

```java
private static final int MAX_DOCX_ENTRIES = 64;
private static final int MAX_DOCX_XML_BYTES = 2 * 1024 * 1024;

private String extractDocxText(byte[] bytes) throws IOException {
    int entries = 0;
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (++entries > MAX_DOCX_ENTRIES) {
                throw new DocumentParseException("DOCUMENT_PARSE_FAILED");
            }
            if (!"word/document.xml".equals(entry.getName())) {
                continue;
            }
            byte[] xmlBytes = zip.readNBytes(MAX_DOCX_XML_BYTES + 1);
            if (xmlBytes.length > MAX_DOCX_XML_BYTES) {
                throw new DocumentParseException("DOCUMENT_PARSE_FAILED");
            }
            return extractWordText(new String(xmlBytes, StandardCharsets.UTF_8));
        }
    }
    return "";
}
```

### 2. PDF 轻量解析失败时回退索引原始二进制内容，存在二进制垃圾索引与检索污染

**严重级别:** MEDIUM  
**OWASP:** A04 Insecure Design / A03 Injection（下游 RAG 上下文污染）  
**位置:** `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java:143-154`  
**证据:** `extractPdfText` 未匹配 `(... ) Tj` 时返回 `raw`，随后 `parseText` 清理控制字符后仍可能生成大量 `%PDF`、对象流、压缩流、随机字节文本 chunk。  
**利用条件:** 上传不可解析 PDF 或伪装 PDF 的二进制文件。  
**Blast Radius:** KB chunk 被垃圾内容污染，影响 keyword/hybrid retrieval；若垃圾文本包含提示注入片段，可能进入后续 RAG prompt。权限过滤仍能限制跨 KB 泄露，但不能防止同 KB 质量污染。  
**建议修复示例:**

```java
private String extractPdfText(byte[] bytes) {
    String raw = new String(bytes, StandardCharsets.ISO_8859_1);
    Matcher matcher = PDF_TEXT_OBJECT.matcher(raw);
    StringBuilder builder = new StringBuilder();
    while (matcher.find()) {
        builder.append(unescapePdfText(matcher.group(1))).append(' ');
    }
    String extracted = clean(builder.toString());
    if (extracted.isBlank() || looksLikeBinaryGarbage(extracted)) {
        return "";
    }
    return extracted;
}

private boolean looksLikeBinaryGarbage(String value) {
    long lettersOrDigits = value.chars().filter(Character::isLetterOrDigit).count();
    return value.length() > 0 && lettersOrDigits * 100 / value.length() < 30;
}
```

### 3. TXT/Markdown 未做二进制垃圾判定，伪装扩展名可被索引

**严重级别:** MEDIUM  
**OWASP:** A04 Insecure Design  
**位置:** `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java:27-31`, `157-165`  
**证据:** parser 选择仅依赖 `contentType` / 文件名；TXT/Markdown 直接 UTF-8 解码，`clean` 只移除控制字符，不判断 `U+FFFD` 比例、可打印字符比例、最大有效文本长度或 MIME magic。  
**利用条件:** 上传 `.txt` / `.md` 但内容为二进制。  
**Blast Radius:** 与 PDF 垃圾索引类似；还可能让超长无意义 token 进入 chunk/hash/embedding 阶段。  
**建议:** no-dependency 前提下增加轻量 allowlist：文件大小上限、可打印字符比例、replacement char 比例、最小有效文本长度、最大解析文本长度。失败统一返回空 section 或 `DOCUMENT_PARSE_FAILED`，不要保存原始错误。

### 4. DOCX 解析未校验 ZIP 结构与 XML 语义，测试覆盖不足

**严重级别:** MEDIUM  
**OWASP:** A04 Insecure Design  
**位置:** `DocumentParserService.java:124-140`; `DocumentParserServiceTest.java:79-105`  
**证据:** 当前测试只覆盖 happy path DOCX；未覆盖 malformed zip、缺失 `word/document.xml`、超大 entry、多 entry、XML entity/escape、损坏 UTF-8、空 DOCX。当前实现不使用 XML parser，因此没有 XXE 直接风险，但 regex 提取对复杂 DOCX 语义有限。  
**建议:** 增加 parser-level 安全回归，确保异常安全映射到 `DOCUMENT_PARSE_FAILED`，空内容不会生成 chunk。

### 5. Parser 只捕获 IOException，部分解析异常依赖 IndexService 兜底

**严重级别:** LOW-MEDIUM  
**OWASP:** A09 Security Logging and Monitoring Failures / A04 Insecure Design  
**位置:** `DocumentParserService.java:26-35`; `IndexService.java:216-218`, `519-532`  
**证据:** `DocumentParserService` 只把 `IOException` 包装为 `DocumentParseException`。`RuntimeException` 会到 `IndexService.safeError`，多数映射为 `DOCUMENT_INDEX_UNEXPECTED_ERROR`，不会泄露 raw message，但失去 parser failure 分类。  
**已满足:** `IndexServiceParserFailureTest` 已验证 `DocumentParseException` 会写入安全错误码。  
**建议:** parser 边界内统一将可预期格式错误、zip 限制、垃圾内容判定映射为 `DOCUMENT_PARSE_FAILED`，保留 `IndexService` 作为最后兜底。

### 6. 上传入口缺少 parser 级文件类型/大小验收证据

**严重级别:** MEDIUM  
**OWASP:** A05 Security Misconfiguration  
**位置:** `DocumentController.java:30-45`; `DocumentService.java:101-125`  
**证据:** 当前上传路径将 `MultipartFile` 交给 storage，并保存 `contentType`/`name`；本次未看到 parser-specific allowlist、最大文件大小、最大可解析文本长度的测试证据。即使全局 multipart limit 存在，也仍需 parser 层防御，因为 worker 读取的是已存储 byte[]。  
**建议:** parser/IndexService 必须独立限制解析输入，不依赖前端或 HTTP multipart 配置。

## 正向结论

- **raw exception 泄露控制基本合格。** `IndexService.safeError` 对 `DocumentParseException`、`IOException`、`IllegalStateException` 等映射为固定码；`IndexServiceParserFailureTest` 验证了 document/task errorMessage 为 `DOCUMENT_PARSE_FAILED`，不会持久化 `"parser failed"`。
- **边界分层基本合格。** 格式解析位于 `rag/parser`，`IndexService` 消费 `ParsedDocument` 并负责 chunk/hash/metadata/持久化，符合 `rag-parser-boundary.md`。
- **依赖漂移未发现。** `backend/pom.xml` 未新增 Tika/POI/OCR parser 依赖；`dependency:tree` 成功。
- **API/schema 漂移未发现。** 本次 parser 加固未要求新增 endpoint 或 migration；`SchemaConvergenceMigrationTest` 通过。
- **核心回归已存在。** Markdown heading hierarchy、TXT blank skip、PDF/DOCX happy path、parser failure safe code、chunk/hash/metadata、worker 共享 IndexService 均已有一定测试覆盖。

## 必测回归

### Parser 安全回归

1. `DocumentParserServiceTest`：二进制 `.txt` / `.md` 不生成 section，或抛 `DOCUMENT_PARSE_FAILED`。
2. `DocumentParserServiceTest`：不可解析 PDF 不回退 raw `%PDF` / object stream，不生成垃圾 chunk。
3. `DocumentParserServiceTest`：PDF 文本提取成功时仍保留 pageNum=1 和有效文本。
4. `DocumentParserServiceTest`：DOCX malformed zip 映射为 `DOCUMENT_PARSE_FAILED`，不泄露 zip/raw exception。
5. `DocumentParserServiceTest`：DOCX 缺失 `word/document.xml` 返回空 section，并由索引层安全失败。
6. `DocumentParserServiceTest`：DOCX 超大 `word/document.xml` / 超多 entries / zip bomb 样本触发安全失败。
7. `DocumentParserServiceTest`：XML escape `&lt; &gt; &amp; &quot; &apos;` 仍按预期处理。

### IndexService / Worker 回归

1. `IndexServiceParserFailureTest`：parser 抛带敏感信息的 cause，例如 `"C:\\path\\file.docx apiKey=sk-test raw text"`，task/document 均只保存 `DOCUMENT_PARSE_FAILED`。
2. `IndexServiceTest`：空解析结果不保存 chunk，document/task 安全失败为 `DOCUMENT_EMPTY_OR_UNAVAILABLE` 或明确固定码。
3. `IndexServiceTest`：二进制垃圾文档不会写入 `kb_doc_chunk.content`。
4. `IndexTaskWorkerSchedulerTest`：worker 路径与手动 reindex 使用相同 parser service，失败隔离不影响下一任务。
5. `SchemaConvergenceMigrationTest`：确认无 schema 变更。
6. 全量 `mvn test`：确认 parser 加固不破坏已有 RAG/retrieval/embedding/vector 流程。

### 依赖与漂移回归

1. `mvn --% dependency:tree -Dscope=compile`：确认无新增 parser/OCR 依赖。
2. 若后续新增依赖：必须新增 `docs/security/` dependency review，并记录 license、维护状态、安全公告。
3. API 文档检查：确认无新增/变更上传、reindex、index-task endpoint contract。

## 禁止事项

- 禁止引入 Apache Tika、Apache POI、OCR、native parser 或云解析 SDK，除非先完成 dependency review、SPEC/PLAN/TASK 更新和显式批准。
- 禁止在 `IndexService` 继续堆格式解析细节；格式解析必须留在 `rag/parser` 边界。
- 禁止 PDF 解析失败时索引原始二进制内容。
- 禁止 DOCX 使用无上限 `readAllBytes()` 或无 entry/size 限制的 zip 读取。
- 禁止把 raw exception message、storage key、bucket、文件路径、原文片段、provider secret 写入 `kb_document.error_message` 或 `kb_index_task.error_message`。
- 禁止仅依赖文件扩展名或客户端 `contentType` 判断安全性。
- 禁止为解析质量修改 RAG query/citation 公共 API 或数据库 schema，除非 SPEC/PLAN/TASK 明确批准。
- 禁止跳过 Agent/RAG 既有规则：权限过滤、citation、trace、max loop 等不能被 parser 改动旁路。

## 验收建议

**建议验收门槛:** 暂不作为最终验收通过；完成以下项后可通过 P3-2-C Security & Quality Gate。

1. 修复 DOCX zip 读取边界：entry count、`word/document.xml` 解压后字节上限、解析文本长度上限。
2. 修复 PDF raw fallback：解析不到有效文本时返回空或安全失败，不能把原始 PDF bytes 作为文本进入 chunk。
3. 增加 TXT/Markdown/PDF/DOCX 二进制垃圾判定，并以固定错误码或空解析结果处理。
4. 补齐上述“必测回归”，至少覆盖安全失败路径、worker 路径、无 raw exception 泄露、无 chunk 持久化。
5. 保持 `backend/pom.xml`、API contract、DB migration 不变；若改变，必须回到 dependency/security/schema/API gate。
6. 最终执行：
   - `mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest test`
   - `mvn test`
   - `mvn --% dependency:tree -Dscope=compile`

## Security Checklist

- [x] No hardcoded real secrets in reviewed parser/index production scope
- [ ] All parser inputs safely validated against binary garbage and decompression abuse
- [x] Raw parser exception persistence avoided in verified `DocumentParseException` path
- [x] Authentication/authorization not changed by parser hardening
- [x] API/schema drift not observed in reviewed files/tests
- [x] No parser dependency drift observed by dependency tree
- [ ] Full CVE dependency audit configured/run
- [ ] Regression tests sufficient for zip/docx/PDF/binary hardening
