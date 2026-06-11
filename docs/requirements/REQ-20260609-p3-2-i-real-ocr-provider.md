# REQ - P3-2-I Real OCR Provider

## 1. 功能需求

| ID | Requirement |
|---|---|
| R1 | 系统应提供一个 `process` OCR fallback provider，实现 `OcrFallbackProvider` |
| R2 | 默认 `learning-os.rag.parser.ocr.enabled=false` 时不得调用外部命令 |
| R3 | `enabled=true` 且 `provider=process` 但 command 未配置时，应返回 `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE / ""` |
| R4 | command 执行成功且 stdout 含文本时，应返回 `SUCCEEDED / OCR_PROVIDER_SUCCEEDED / text` |
| R5 | command 非零退出、执行异常或超时时，应返回 `FAILED / OCR_PROVIDER_FAILED / ""` |
| R6 | process provider 不得通过 shell 字符串拼接执行命令，必须使用参数数组 |
| R7 | process provider 不得把 stderr、命令路径、临时路径、raw exception、secret 或 OCR 原文写入 reasonCode |
| R8 | image-only PDF 在 process provider 成功时应生成 OCR `ParsedSection` |
| R9 | image-only PDF 在 process provider 失败时应保持空 sections，不 fallback 到 raw PDF bytes |
| R10 | OCR 输出超过 parser 限制时应映射为安全 parser failure |

## 2. 非功能需求

| ID | Requirement |
|---|---|
| N1 | 不新增 Maven dependency，不修改 `backend/pom.xml` |
| N2 | 不修改 API、DB migration、frontend |
| N3 | 不修改 `IndexService`、retrieval、citation、embedding、VectorDB |
| N4 | 外部命令超时必须可配置，且默认有保守值 |
| N5 | OCR 输出字符上限必须可配置，且不得超过 parser 总输出限制 |
| N6 | 配置必须位于 `learning-os.rag.parser.ocr.*` 命名空间 |
| N7 | provider 启用失败不得导致应用启动失败 |

## 3. 验收需求

- PRD / REQ / SPEC / PLAN / TASK / Context / Dependency Review 存在。
- Subagent 架构与安全报告存在。
- TDD RED 已记录。
- focused provider/parser tests 通过。
- adjacent index/parser tests 通过。
- no-OCR-dependency tree check 通过。
- full backend `mvn test` 通过或限制说明清楚。
- Evidence / Acceptance / Changelog / Memory / TODO / Retro 更新。
