# SPEC - P3-2-I Real OCR Provider

## 1. 概述

本规格定义 P3-2-I：在现有 `OcrFallbackProvider` SPI 后新增最小真实 OCR provider。该 provider 通过外部命令进程读取 PDF bytes 并返回 OCR 文本，默认关闭，不新增 Maven 依赖，不改变 RAG 索引与查询合同。

## 2. 配置

扩展现有配置：

```yaml
learning-os:
  rag:
    parser:
      ocr:
        enabled: ${RAG_PARSER_OCR_ENABLED:false}
        provider: ${RAG_PARSER_OCR_PROVIDER:none}
        process:
          command: ${RAG_PARSER_OCR_PROCESS_COMMAND:}
          timeout: ${RAG_PARSER_OCR_PROCESS_TIMEOUT:10s}
          max-output-chars: ${RAG_PARSER_OCR_PROCESS_MAX_OUTPUT_CHARS:200000}
```

说明：

- `provider=process` 时才可能选择 process provider。
- `command` 为空时 provider 返回 unavailable，不启动失败。
- `timeout` 默认 10 秒。
- `max-output-chars` 不得超过 `ParserResourceLimits.MAX_EXTRACTED_CHARS`。

## 3. Provider 行为

新增 `ProcessOcrFallbackProvider implements OcrFallbackProvider`。

| 场景 | 输出 |
|---|---|
| command 为空 | `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE / ""` |
| command 成功且 stdout 有文本 | `SUCCEEDED / OCR_PROVIDER_SUCCEEDED / stdoutText` |
| command 成功但 stdout 为空 | `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE / ""` |
| command 非零退出 | `FAILED / OCR_PROVIDER_FAILED / ""` |
| command 超时 | `FAILED / OCR_PROVIDER_FAILED / ""` |
| command 执行异常 | `FAILED / OCR_PROVIDER_FAILED / ""` |

## 4. 命令执行约束

- 使用 `ProcessBuilder(List<String>)`。
- 不通过 `cmd /c`、PowerShell、shell 字符串或拼接命令执行。
- 命令字段按空白拆分为参数列表；不支持用户上传文件名参与命令构造。
- PDF bytes 通过 stdin 输入给进程，避免创建以原始文件名命名的临时文件。
- stdout 以 UTF-8 读取，最多读取 `max-output-chars + 1` 字符。
- stderr 必须消费以避免进程阻塞，但不得进入 result。
- timeout 后销毁进程。
- 当前线程中断时恢复 interrupt flag 并返回 safe failed。

## 5. PDF fallback 集成

不修改 `PdfBoxDocumentFormatParser` 合同：

- PDFBox 抽取不到文本时调用 `OcrFallbackService`。
- process provider 成功时生成 `ParsedSection(pageNum=1)`。
- process provider 失败、不可用或 disabled 时返回空 sections。
- OCR text 超过 parser 限制时由 parser 层映射 `DOCUMENT_PARSE_FAILED`。

## 6. 边界约束

- 不修改 `IndexService`。
- 不修改 API。
- 不修改 DB migration。
- 不修改 frontend。
- 不修改 retrieval/citation/VectorDB/embedding。
- 不新增 Maven dependency。
- 不持久化 raw command、stderr、exception、path、secret 或 OCR 原文。

## 7. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | provider 在 parser boundary 内 |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 Agent/RAG query/citation 合同 |
| Security | PASS | 默认关闭、无 Maven 依赖、safe reasonCode |
| API / Database | PASS | 不改 API/schema |

## 8. 测试策略

```powershell
cd backend
mvn --% -Dtest=ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,RealParserProviderTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
mvn --% dependency:tree -Dincludes=net.sourceforge.tess4j:tess4j,net.java.dev.jna:jna,org.bytedeco:*,com.aliyun:*,com.google.cloud:google-cloud-vision,software.amazon.awssdk:textract
mvn test
```

## 9. 非目标

- Tess4J/JNA/cloud OCR。
- 页级 OCR confidence。
- 工业级 PDF/DOCX layout/table/TOC。
- OCR 结果结构化 metadata。
- P3-2 全部完成声明。
