# REQ - RAG Parser Adapter 最小生产切片

## 1. 用户需求

系统在索引课程文档时，需要一个统一、可测试、可替换的解析边界，而不是把 Markdown / TXT / PDF / DOCX 的解析逻辑全部堆在 `IndexService` 里。

## 2. 业务需求

1. 文档解析必须有统一 adapter/service 边界。
2. TXT / Markdown / PDF / DOCX 必须通过同一条解析入口产出 section 数据。
3. 索引失败必须返回安全错误码，不能泄露原始异常、文件路径、storage key 或原文。
4. worker 与手动 reindex 必须复用同一 parser 路径。
5. 不新增外部解析依赖。

## 3. 约束

- 不引入新依赖。
- 不修改公开 RAG query / citation API。
- 不改变现有 chunk hash、overlap、heading hierarchy 行为。
- 不把 OCR 做满，只预留后续扩展边界。

## 4. 验收口径

- RED 测试先失败，GREEN 后通过。
- 单测覆盖 parser selection、Markdown / TXT / PDF / DOCX 解析、空文档和失败脱敏。
- `IndexServiceTest` 或等价集成测试证明 worker/manual 一致。
- `mvn test` 和聚焦测试通过。

## 5. 交付状态

已完成。parser adapter 边界、失败安全码、worker/manual 共用路径和相关测试已交付；复杂 OCR / PDF / DOCX 增强仍留作后续独立切片。
