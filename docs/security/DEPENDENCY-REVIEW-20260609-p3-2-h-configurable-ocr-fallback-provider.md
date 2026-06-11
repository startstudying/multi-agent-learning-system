# Dependency Review - P3-2-H Configurable OCR Fallback Provider

## 1. Dependencies

本切片不新增 Maven dependency。

| Field | Value |
|---|---|
| Added Dependency | None |
| Package Manager | Maven |
| Added By | `TASK-20260609-p3-2-h-configurable-ocr-fallback-provider` |

## 2. Justification

P3-2-H 目标是配置与 provider 选择边界，不是 OCR SDK 接入。使用 fake provider 测试成功路径即可证明 contract，不需要引入 Tess4J/Tesseract/JNA/OpenCV/PaddleOCR/cloud OCR SDK。

## 3. Alternatives Considered

| Alternative | Pros | Cons | Decision |
|---|---|---|---|
| No new dependency + configurable boundary | 范围小，安全默认，后续可扩展 | 不提供真实 OCR 能力 | Adopt |
| Tess4J / Tesseract | 可本地 OCR | native runtime、JNA、语言包、镜像、CVE、CPU/内存风险 | Defer |
| Cloud OCR SDK | OCR 能力成熟 | 隐私外传、密钥、计费、区域合规、超时/重试/SSRF 风险 | Defer |
| Apache Tika OCR path | 格式能力广 | 依赖面过大，不适合本切片 | Reject |

## 4. Security

- [x] No new dependency。
- [x] No secrets required。
- [x] OCR default disabled。
- [x] Provider unavailable/failure uses safe fixed reasonCode。
- [x] No raw provider exception in `OcrFallbackResult`。
- [ ] Full SCA：无新增依赖，本切片不运行独立 SCA；后续真实 OCR SDK 接入时必须补。

## 5. Required Runtime Safeguards

本切片：

- 默认 `learning-os.rag.parser.ocr.enabled=false`。
- `provider=none` 不触发真实 OCR。
- provider 抛异常只返回 `OCR_PROVIDER_FAILED`。
- OCR 输出仍受 `ParserResourceLimits.MAX_EXTRACTED_CHARS` 约束。

后续真实 OCR 切片必须补充：

- 最大 OCR 页数。
- 最大图片大小。
- timeout。
- concurrency。
- queue/backoff。
- temp file cleanup。
- provider endpoint allowlist。
- credential management。

## 6. Approval

| Role | Date | Status |
|---|---|---|
| Security Expert / Peirce | 2026-06-09 | APPROVED WITH CONDITIONS |
| Main Codex | 2026-06-09 | APPROVED WITH CONDITIONS |

## 7. Conditions

1. 不新增 OCR dependency。
2. 不修改 `pom.xml`。
3. 不默认启用 OCR。
4. 不暴露 raw OCR/provider error。
5. 不声明真实 OCR 完成。

