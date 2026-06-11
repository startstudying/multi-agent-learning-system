# REQ - P3-2-F Parser Provider Boundary + OCR Fallback Contract

## 1. 追踪

- PRD：`docs/product/PRD-20260609-p3-2-f-parser-provider-boundary.md`
- 需求编号：REQ-20260609-p3-2-f-parser-provider-boundary

## 2. 功能需求

| 编号 | 需求描述 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | 新增 `ParseInput`，作为 parser provider 的统一输入 | 必须 | 包含文档 id/name/contentType/sizeBytes/bytes，不包含 storage key/path |
| FR-02 | 新增 `DocumentFormatParser` contract | 必须 | provider 暴露 `format()` 和 `parse(ParseInput)` |
| FR-03 | `DocumentParserService` 通过 provider registry 分发解析 | 必须 | 测试可注入 fake provider 并验证被调用 |
| FR-04 | Markdown/TXT/PDF/DOCX 轻量解析迁移到独立 provider | 必须 | 既有 parser 行为测试继续通过 |
| FR-05 | 新增 OCR fallback contract 和 noop 实现 | 必须 | noop 返回 disabled/no text |
| FR-06 | image-only PDF 在无真实 OCR 时继续返回空 sections | 必须 | 不索引 raw PDF bytes，不产生 chunk |
| FR-07 | provider 异常统一映射为 `DOCUMENT_PARSE_FAILED` | 必须 | 非安全异常不暴露 raw message/path/key |

## 3. 非功能需求

| 编号 | 需求描述 | 优先级 |
|---|---|---|
| NFR-01 | 不新增 Maven dependency | 必须 |
| NFR-02 | 不修改 DB schema / migration | 必须 |
| NFR-03 | 不修改公开 API / frontend | 必须 |
| NFR-04 | 不修改 RAG retrieval/citation/vector 边界 | 必须 |
| NFR-05 | 保持 parser failure safe code | 必须 |
| NFR-06 | focused、adjacent、full backend tests 必须运行或说明限制 | 必须 |

## 4. 边界情况

| 场景 | 预期行为 |
|---|---|
| 未注册 provider | `DOCUMENT_PARSE_FAILED` |
| provider 抛出 `IllegalArgumentException` | `DOCUMENT_PARSE_FAILED` |
| provider 抛出 `DocumentParseException` | 保留 safeCode |
| OCR disabled | 返回 disabled/no text |
| PDF 无 text operator | 返回空 sections |
| DOCX malformed zip | `DOCUMENT_PARSE_FAILED` |

## 5. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-09 | Accepted |
