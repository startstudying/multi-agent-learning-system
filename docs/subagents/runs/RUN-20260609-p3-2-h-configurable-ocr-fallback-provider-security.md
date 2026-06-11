# Subagent Run - P3-2-H Configurable OCR Fallback Provider Security

## Summary

安全结论：本切片不建议新增 OCR SDK/native/service。当前目标是“可配置 provider 状态边界”，不是 OCR 能力交付；新增真实 OCR 会把范围扩大到不可信文件解析、native runtime、隐私外传、密钥、计费、超时和运维治理。

风险级别：不新增依赖时为 LOW；直接引入 native OCR 或外部 OCR service 时为 MEDIUM/HIGH。

## 1. 依赖结论

本切片不新增依赖。

理由：

- 现有 `NoopOcrFallbackService` 已能返回 `DISABLED / OCR_DISABLED / ""`，不会抽取文本、不会外呼、不会引入 native runtime。
- P3-2-G 只接入 PDFBox/POI，真实 OCR 仍是后续独立切片。
- Tess4J/Tesseract、JNA、OpenCV、PaddleOCR、云 OCR SDK 均需要独立 dependency/security/runtime/privacy review。

## 2. Dependency / Security Review 要点

- Dependency：None。
- Default：OCR 默认关闭，生产环境除非显式配置并完成独立安全审查，否则不得启用。
- Privacy：OCR 输入可能包含课件、试卷、学生信息、教师资料、手写笔记；不得把原文件、页面图片、OCR 原文、storage key、bucket、路径、用户标识、raw provider response 写入日志、trace、errorMessage、metadataJson。
- Native Risk：Tess4J/Tesseract/JNA/tessdata 需要独立评估 native CVE、容器镜像体积、字体/语言包来源、sandbox、CPU/内存限制、临时文件清理。
- Service Risk：外部 OCR 服务需要独立评估 TLS、endpoint 固定、SSRF 防护、密钥管理、租户隔离、数据保留条款、区域合规、超时/重试/熔断、计费上限。
- Resource Limits：必须定义最大输入大小、最大页数、最大 OCR 页数、最大输出字符数、单任务超时、并发数、队列长度、失败重试次数。
- Observability：只记录低敏状态字段：`ocrEnabled`、`ocrProvider`、`ocrStatus`、`reasonCode`、`latencyMs`、`pageCount`、`outputCharCount`；禁止 raw text/error。
- Approval：真实 OCR 依赖必须单独 PRD/REQ/SPEC/PLAN/TASK 和 dependency review，不能混入本边界切片。

## 3. 安全状态 / reasonCode 建议

建议使用现有状态：

- `DISABLED`
- `UNAVAILABLE`
- `SUCCEEDED`
- `FAILED`

建议 reasonCode：

- `OCR_DISABLED`
- `OCR_PROVIDER_UNAVAILABLE`
- `OCR_PROVIDER_FAILED`
- `OCR_OUTPUT_LIMIT_EXCEEDED`
- `OCR_TIMEOUT`
- `OCR_UNSUPPORTED_FORMAT`

对外与持久化建议：

- parser/index 失败仍统一收口为 `DOCUMENT_PARSE_FAILED` 或短状态码。
- 不保存 provider exception message。
- `OcrFallbackResult.reasonCode` 只能使用固定安全码或安全格式，不能透传 raw provider message。

## 4. 测试建议

- 默认关闭：未配置 OCR 时，扫描 PDF 无文本返回空 sections，状态为 `DISABLED/OCR_DISABLED`。
- 配置边界：显式 provider 未配置完整时返回 `UNAVAILABLE`，不外呼、不抛 raw exception。
- 成功路径：fake OCR 返回短文本，进入 parser 后正常 section。
- 超限路径：fake OCR 返回超长文本，必须映射为 `DOCUMENT_PARSE_FAILED`。
- 泄漏防护：fake provider 抛出包含 `apiKey=sk-live-secret`、storage path、raw file text 的异常，断言 `OcrFallbackResult` 不含这些字符串。

## 5. 不可做事项

- 不在本切片新增 Tess4J、Tesseract、JNA、Tika、OpenCV、云 OCR SDK。
- 不让 OCR provider 默认启用。
- 不把 OCR 原文、文件路径、storage key、bucket、临时文件路径、raw exception、API key、endpoint 写入日志、trace、metadata 或 errorMessage。
- 不让用户请求参数控制 OCR provider endpoint/base URL。
- 不在 frontend 调用 OCR/LLM/provider API。
- 不无上限处理页数、图片大小、输出字符、并发和重试。
- 不把 OCR 成功等同于 RAG citation 可信；OCR 文本仍需来源页码、质量状态和后续引用治理。

