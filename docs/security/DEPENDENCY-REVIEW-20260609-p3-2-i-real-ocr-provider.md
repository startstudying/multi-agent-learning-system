# Dependency Review - P3-2-I Real OCR Provider

## 1. Dependencies

本切片不新增 Maven dependency。

| Field | Value |
|---|---|
| Added Maven Dependency | None |
| Package Manager | Maven |
| Runtime Tool | Optional external OCR command, disabled by default |
| Added By | `TASK-20260609-p3-2-i-real-ocr-provider` |

## 2. Justification

P3-2-I 目标是接入最小真实 OCR provider，而不是引入 Java OCR SDK。外部命令型 provider 可通过部署环境显式配置 OCR command，保持 Java dependency tree 不变，并由 default-disabled 配置避免未准备环境误调用。

## 3. Alternatives Considered

| Alternative | Pros | Cons | Decision |
|---|---|---|---|
| Process-based provider, no Maven dependency | 范围小，不污染 Java dependency tree，可接本地 OCR 工具或脚本 | 需要部署环境安装 runtime command；需处理 timeout 和安全边界 | Adopt |
| Tess4J / Tesseract / JNA | Java 内集成更直接 | native/JNA/runtime/镜像/语言包/CVE 面更大 | Defer |
| Cloud OCR SDK | 识别能力成熟 | 隐私、密钥、计费、跨境、网络错误泄漏风险 | Defer |
| Apache Tika OCR path | 格式能力广 | 依赖面过大，超出最小切片 | Reject |

## 4. Security

- [x] No new Maven dependency。
- [x] OCR default disabled。
- [x] External command must be explicit.
- [x] No shell string execution.
- [x] Provider unavailable/failure uses safe fixed reasonCode.
- [x] No raw stderr/path/secret/OCR text in reasonCode.
- [ ] SCA for native OCR runtime：由部署环境负责；后续若内置 runtime 或 Java SDK，必须单独补充。

## 5. Required Runtime Safeguards

- `learning-os.rag.parser.ocr.enabled=false` 默认关闭。
- `provider=process` 且 command 为空时只返回 unavailable。
- process timeout。
- output max chars。
- stderr consumed but not persisted。
- provider exception 只返回 `OCR_PROVIDER_FAILED`。
- OCR 输出仍受 `ParserResourceLimits.MAX_EXTRACTED_CHARS` 约束。

## 6. Approval

| Role | Date | Status |
|---|---|---|
| Architecture Expert | 2026-06-09 | APPROVED WITH CONDITIONS |
| Security Expert | 2026-06-09 | APPROVED WITH CONDITIONS |
| Main Codex | 2026-06-09 | APPROVED WITH CONDITIONS |

## 7. Conditions

1. 不新增 Maven OCR dependency。
2. 不修改 `pom.xml`。
3. 不默认启用 OCR。
4. 不使用 shell 字符串拼接。
5. 不暴露 raw OCR/provider error。
6. 不声明工业级 OCR / P3-2 全部完成。
