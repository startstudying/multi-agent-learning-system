# Dependency Review - P3-2-F Parser Provider Boundary + OCR Fallback Contract

## Dependency

| Field | Value |
|---|---|
| Name | None |
| Version | N/A |
| Package Manager | Maven |
| Added By | TASK-20260609-p3-2-f-parser-provider-boundary |

## Justification

本切片只新增 parser provider contract 和 noop OCR contract，不新增外部依赖。真实 PDF/DOCX/OCR 能力必须在后续独立切片中单独审查。

## Alternatives Considered

| Alternative | Pros | Cons | Decision |
|---|---|---|---|
| Apache PDFBox | 适合后续真实 PDF 文本/页码抽取 | 需要依赖审查、资源限制、真实 PDF 测试 | 延后 |
| Apache POI | 适合后续真实 DOCX 结构解析 | 依赖体积和复杂格式处理需独立验证 | 延后 |
| Tess4J / Tesseract | 可做本地 OCR | native/JNA/tessdata 部署和安全风险高 | 延后 |
| 外部 OCR 服务 | 可快速接入 OCR 能力 | 涉及密钥、计费、网络、隐私和失败重试策略 | 延后 |
| No dependency provider boundary | 风险最小，先稳定架构 | 不提升真实解析能力 | 采用 |

## License

| Field | Value |
|---|---|
| License | N/A |
| Compatible | Yes |

## Security

- [x] No known critical CVEs because no dependency is added.
- [x] Maintained actively is not applicable because no dependency is added.
- [x] Trusted publisher is not applicable because no dependency is added.

## Impact

- Bundle size impact: none.
- Transitive dependencies concern: none.
- Runtime requirement: none.

## Approval

| Role | Date | Status |
|---|---|---|
| Main Codex | 2026-06-09 | APPROVED |
