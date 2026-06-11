# REQ - P3-2-C RAG 无依赖 Parser 加固

## 1. 追踪

- PRD：`docs/product/PRD-20260608-rag-parser-hardening.md`
- 需求编号：REQ-20260608-rag-parser-hardening

## 2. 功能需求

| 编号 | 需求描述 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | PDF parser 不得在文本对象抽取失败时 fallback 原始二进制内容 | 必须 | 无文本 PDF 不生成 chunk，chunk 内容不包含 `%PDF` / object stream 垃圾 |
| FR-02 | PDF parser 支持现有 `Tj` 和简单 `TJ` array 文本抽取 | 必须 | 单测覆盖 `(...) Tj` 与 `[(...)(...)] TJ` |
| FR-03 | DOCX parser 必须限制 zip entry 数和 `word/document.xml` 最大读取字节数 | 必须 | 超限 DOCX 抛 `DocumentParseException`，safeCode 为 `DOCUMENT_PARSE_FAILED` |
| FR-04 | DOCX parser 按 `<w:p>` 分段并识别 `Heading1` 至 `Heading6` | 必须 | section 输出包含 `title`、`headingLevel`、`headingPath` |
| FR-05 | DOCX parser best-effort 识别 page break 并输出 `pageNum` | 必须 | page break 后正文 section 的 `pageNum` 递增 |
| FR-06 | TXT/Markdown 对明显二进制或损坏 UTF-8 内容安全拒绝 | 应该 | parser 不产生垃圾 section，异常为 `DOCUMENT_PARSE_FAILED` |
| FR-07 | parser 失败路径不得持久化 raw exception、路径、storage key、secret 或原文片段 | 必须 | `IndexServiceParserFailureTest` 覆盖敏感 cause，task/document 只保存安全错误码 |
| FR-08 | parser 输出的 `pageNum` / `headingPath` 继续写入 chunk 与 metadata | 必须 | `IndexServiceTest` 覆盖 DOCX heading/page metadata |

## 3. 非功能需求

| 编号 | 需求描述 | 优先级 |
|---|---|---|
| NFR-01 | 不新增 Maven dependency | 必须 |
| NFR-02 | 不新增 DB schema / migration | 必须 |
| NFR-03 | 不改变公开 API / frontend | 必须 |
| NFR-04 | parser 处理不可信输入时必须有资源上限 | 必须 |
| NFR-05 | 测试需覆盖 RED/GREEN 行为与全量 Maven 回归 | 必须 |

## 4. 用户流程

### 流程 1：可解析 DOCX 索引

```text
教师上传 DOCX
-> parser 读取 word/document.xml
-> 识别 heading / paragraph / page break
-> IndexService 生成 chunk 和 metadata
-> RAG 检索使用结构化 section 信息
```

### 流程 2：不可解析 PDF

```text
教师上传扫描 PDF
-> parser 未抽取到有效文本
-> 返回空 section
-> IndexService 标记 DOCUMENT_EMPTY_OR_UNAVAILABLE
-> 不生成垃圾 chunk
```

## 5. 输入 / 输出

### 输入

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| `KbDocument.name` | string | 是 | 用于 parser 类型辅助判断 |
| `KbDocument.contentType` | string | 是 | 用于 parser 类型辅助判断 |
| `bytes` | byte[] | 是 | parser 内做文本有效性与 DOCX 资源边界校验 |

### 输出

| 字段 | 类型 | 说明 |
|---|---|---|
| `ParsedDocument.parser` | enum | `MARKDOWN` / `TXT` / `PDF` / `DOCX` |
| `ParsedSection.title` | string | 当前 section 标题，可为空 |
| `ParsedSection.headingLevel` | integer | heading 层级，可为空 |
| `ParsedSection.headingPath` | array | heading 路径 |
| `ParsedSection.content` | string | 清洗后的可索引文本 |
| `ParsedSection.pageNum` | integer | best-effort 页码，可为空 |

## 6. 边界情况

| 场景 | 预期行为 |
|---|---|
| PDF 无 `Tj` / `TJ` 可读文本 | 返回空 section，不索引 raw bytes |
| DOCX 缺少 `word/document.xml` | 返回空 section |
| DOCX malformed zip | `DOCUMENT_PARSE_FAILED` |
| DOCX zip entries 超限 | `DOCUMENT_PARSE_FAILED` |
| DOCX XML 超限 | `DOCUMENT_PARSE_FAILED` |
| TXT/Markdown 明显二进制 | `DOCUMENT_PARSE_FAILED` |
| Markdown heading-only | 不产生空内容 section |

## 7. 依赖关系

- 上游依赖：`DocumentStorageService` 提供 bytes。
- 下游影响：`IndexService` 使用 parser sections 生成 chunk/hash/metadata。

## 8. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | Accepted |
