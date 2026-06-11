# REQ - P3-2-H Configurable OCR Fallback Provider

## 1. 功能需求

| ID | Requirement |
|---|---|
| R1 | 系统应提供 `learning-os.rag.parser.ocr.enabled` 配置，默认 `false` |
| R2 | 系统应提供 `learning-os.rag.parser.ocr.provider` 配置，默认 `none` |
| R3 | 默认关闭时，OCR fallback 应返回 `DISABLED / OCR_DISABLED / ""` |
| R4 | OCR 启用但配置的 provider 不存在或不可用时，应返回 `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE / ""` |
| R5 | OCR provider 执行异常时，应返回 `FAILED / OCR_PROVIDER_FAILED / ""`，不得透传 raw exception |
| R6 | fake OCR provider 返回短文本时，PDF image-only fallback 应生成一个 `ParsedSection` |
| R7 | fake OCR provider 返回超长文本时，PDF provider 应继续映射为安全 `DOCUMENT_PARSE_FAILED` |
| R8 | `NoopOcrFallbackService` 应继续可被单元测试直接实例化并保持 disabled 行为 |

## 2. 非功能需求

| ID | Requirement |
|---|---|
| N1 | 不新增 Maven 依赖 |
| N2 | 不修改 API / DB migration / frontend |
| N3 | 不修改 `IndexService`、retrieval、citation、embedding、VectorDB |
| N4 | OCR provider 不得访问 Mapper / Repository / storage secret |
| N5 | provider raw exception、文件路径、storage key、API key、OCR 原文不得进入错误消息 |
| N6 | 配置边界必须保留未来接入真实 provider 的扩展点 |

## 3. 验收需求

- PRD / REQ / SPEC / PLAN / TASK / Context / Dependency Review 存在。
- TDD RED 已记录。
- focused parser tests 通过。
- adjacent index tests 通过。
- full backend `mvn test` 通过或限制说明清晰。
- Evidence / Acceptance / Changelog / Memory / Retro 完成。

