# Subagent Report - P3-2-F Parser Provider Boundary Dependency Review

## 1. 结论

本切片不新增 Maven dependency。PDFBox、POI、Tess4J/OCR 仅作为后续候选方向记录，不在本轮接入。

## 2. 候选依赖判断

| 候选 | 用途 | 本切片决策 | 原因 |
|---|---|---|---|
| Apache PDFBox | 后续真实 PDF 文本/页码抽取 | 延后 | 需要独立 dependency review、测试和资源限制 |
| Apache POI | 后续真实 DOCX 结构解析 | 延后 | 当前只做 provider 边界，不改变解析能力承诺 |
| Tess4J / Tesseract | 后续 OCR fallback | 延后 | native/JNA/tessdata 部署风险高，不适合本切片 |
| 外部 OCR 服务 | 后续 OCR fallback | 延后 | 涉及密钥、计费、网络、隐私和失败重试策略 |

## 3. 本切片依赖策略

- 保持 `backend/pom.xml` 不变。
- 只新增 Java interface / record / service classes。
- OCR 只落 `OcrFallbackService` contract 和 noop implementation。
- 后续如果引入真实 parser/OCR，必须单独创建 `docs/security/DEPENDENCY-REVIEW-*.md` 并更新 SPEC/PLAN/TASK。

## 4. 风险

- 过早接入 PDF/OCR 依赖会放大部署、license、安全 advisory、native runtime 和资源消耗风险。
- 没有 provider 边界时直接接入依赖，会让 `DocumentParserService` 膨胀并影响 `IndexService` 稳定性。

## 5. 建议

先完成 provider boundary。真实 PDFBox/POI/OCR 以独立切片实施，并在该切片做 fresh dependency review。
