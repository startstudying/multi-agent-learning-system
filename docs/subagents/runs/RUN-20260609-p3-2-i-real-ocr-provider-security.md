# RUN-20260609 P3-2-I Real OCR Provider Security

## 1. 结论

P3-2-I 推荐采用本地外部命令型 OCR provider，默认关闭，通过现有 `OcrFallbackProvider` SPI 接入，不新增 Maven OCR 依赖。

外部命令 OCR 必须被视为处理不可信文件的隔离子系统：显式配置命令、禁止 shell 拼接、限制输入输出、设置超时、清理临时文件，并只返回固定安全 reasonCode。

## 2. 首选方案

- Provider name：`process`。
- Runtime tool：由部署环境提供，例如 Tesseract CLI 或兼容脚本。
- Java 侧：JDK `ProcessBuilder`，不新增 Tess4J/JNA/cloud OCR SDK。
- 默认配置：`learning-os.rag.parser.ocr.enabled=false`、`provider=none`。

## 3. 安全边界

- OCR 只能在 `rag/parser` 内执行。
- 不访问 Mapper / Repository / storage secret。
- 不使用原始文件名作为临时文件名。
- 命令路径必须来自受控配置。
- 命令参数必须来自 allowlist，不接受用户输入拼接。
- stdout 只作为候选 OCR text。
- stderr/raw exception/path/API key/storage key/OCR 原文不得进入 `reasonCode`、task error 或日志。
- 进程必须有 timeout，超时后销毁。
- 成功、失败、超时都必须清理临时文件。

## 4. 依赖评审

本切片不新增 Maven dependency。

| Candidate | Decision | Notes |
|---|---|---|
| External process OCR | Adopt | 无 Maven 依赖；运行时由部署环境安装；默认关闭 |
| Tess4J / JNA | Defer | native/JNA/runtime 复杂，需独立 dependency review |
| Cloud OCR SDK | Defer | 隐私、密钥、计费、跨境和网络错误面更大 |
| Apache Tika OCR path | Reject for this slice | 依赖面过大，超出最小 OCR provider 目标 |

## 5. 必须测试

- 默认 disabled 不调用 provider。
- `enabled=true + provider=process + command missing` 返回 `OCR_PROVIDER_UNAVAILABLE`。
- command 成功时返回安全成功和 OCR text。
- command non-zero / timeout / exception 返回 `OCR_PROVIDER_FAILED`。
- stderr/path/secret 不进入 result。
- OCR 输出超长被安全失败或由 parser 映射为 `DOCUMENT_PARSE_FAILED`。
- image-only PDF 成功 OCR 后生成 section；失败时仍为空 sections。
- 未新增 Tess4J/JNA/cloud OCR Maven 依赖。

## 6. 验收限制

本切片只能声明真实 OCR provider boundary 接入完成，不能声明：

- 工业级 OCR 完成。
- 页级 OCR confidence 完成。
- PDF/DOCX layout/table/TOC/reading order 完成。
- P3-2 全部完成。
