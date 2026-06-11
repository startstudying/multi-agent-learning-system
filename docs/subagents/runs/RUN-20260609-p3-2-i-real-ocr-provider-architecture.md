# RUN-20260609 P3-2-I Real OCR Provider Architecture

## 1. 结论

P3-2-I 的最小真实 OCR provider 应继续放在 `backend/src/main/java/com/learningos/rag/parser/`，作为 `OcrFallbackProvider` 实现接入现有 `ConfigurableOcrFallbackService`。

本切片不新增第二个 `OcrFallbackService`，不修改 `IndexService`、API、DB migration、retrieval/citation、embedding 或 VectorDB。

推荐实现 `process` provider：使用 JDK `ProcessBuilder` 调用显式配置的外部 OCR 命令，默认关闭；命令未配置、超时或失败时返回安全 OCR failure/unavailable 状态。

## 2. 边界

- 输入：`ParseInput`。
- 输出：`OcrFallbackResult`。
- Provider 名称：`process`。
- 注册方式：Spring `OcrFallbackProvider` bean，由 `learning-os.rag.parser.ocr.provider=process` 选择。
- PDF 无文本时仍由 `PdfBoxDocumentFormatParser` 调用 OCR fallback。
- OCR 成功后仍只产生现有 `ParsedSection`，不扩展 citation schema。

## 3. 依赖策略

本切片不新增 Maven 依赖。

外部 OCR 工具属于运行时依赖：

- 默认不启用。
- 不从 PATH 或用户输入隐式推导命令。
- 命令路径必须通过配置显式提供。
- 后续 Tess4J/JNA/cloud OCR provider 必须另起 dependency review。

## 4. 影响控制

必须保持零合同变更：

- 不改 `IndexService`。
- 不改 controller/API DTO。
- 不改 DB migration。
- 不改 frontend。
- 不改 retrieval/citation/VectorDB/embedding。
- 不扩展 `ParsedSection` / `OcrFallbackResult` 元数据模型。

## 5. 测试建议

- Provider disabled 时不执行命令。
- Provider enabled 但 command 缺失时返回 `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`。
- 命令成功时返回 `SUCCEEDED / OCR_PROVIDER_SUCCEEDED / text`。
- 命令失败、超时、异常时返回 `FAILED / OCR_PROVIDER_FAILED / ""`。
- stderr、命令路径、临时路径、secret、raw OCR text 不进入 reasonCode 或错误消息。
- image-only PDF + process provider 成功时产生 OCR section。
- process provider 失败时 image-only PDF 保持空 sections，不索引 raw PDF bytes。
- OCR 输出超长时仍由 parser 映射安全 `DOCUMENT_PARSE_FAILED`。

## 6. 风险与缓解

| Risk | Mitigation |
|---|---|
| 进程命令注入 | 使用 `ProcessBuilder(List<String>)`，禁止 shell 字符串拼接 |
| 资源耗尽 | timeout、最大输入、最大输出、临时文件清理 |
| 误启用 | 默认 `enabled=false/provider=none`，缺 command 返回 unavailable |
| 敏感信息泄露 | stderr/raw exception/path/secret 不进入 result 或 task error |
| 范围漂移 | 页级 OCR、confidence、layout/table/TOC 留作后续切片 |

## 7. 参考证据

- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackProvider.java`
- `backend/src/main/java/com/learningos/rag/parser/ConfigurableOcrFallbackService.java`
- `backend/src/main/java/com/learningos/rag/parser/PdfBoxDocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `docs/skills/project-specific/rag-parser-boundary.md`
