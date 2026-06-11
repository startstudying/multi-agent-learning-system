# Subagent Report - P3-2-F Parser Provider Boundary Integration Review

## 1. 集成结论

架构、测试、依赖三个方向一致：本切片应执行单一后端 parser 内部重构，不新增外部依赖，不改变索引输出模型，不改 API/DB/frontend。

## 2. 冲突处理

| 问题 | 决策 |
|---|---|
| 是否接入真实 PDFBox/POI | 否。仅保留 future dependency review。 |
| 是否实现真实 OCR | 否。仅新增 `OcrFallbackService` + noop。 |
| 是否修改 `IndexService` | 原则上不改业务逻辑；只通过现有测试验证边界仍然稳定。 |
| 是否修改 parser 输出模型 | 否。`ParsedDocument` / `ParsedSection` 保持兼容。 |

## 3. 统一实施计划

1. 补齐 PRD/REQ/SPEC/PLAN/TASK/CONTEXT。
2. 添加 RED 测试：provider 分发、noop OCR、image-only PDF 空结果、安全错误码。
3. 重构 `DocumentParserService` 为 provider registry。
4. 拆分轻量格式 provider。
5. 运行 focused、adjacent、full backend verification。
6. 更新 evidence、acceptance、changelog、memory、TODO、retro。

## 4. 验收口径

只允许声明“parser provider boundary + OCR fallback contract 已完成”。不得声明复杂 PDF/DOCX/OCR 生产能力已完成。
