# PRD - P3-2-C RAG 无依赖 Parser 加固

## 1. 问题陈述

当前 RAG parser 已有独立边界，但 PDF 与 DOCX 仍是轻量启发式实现。PDF 在无法抽取 `Tj` 文本时会 fallback 为原始 ISO-8859-1 字符串，存在把压缩流、对象流或伪造二进制内容写入 chunk 的风险。DOCX 只读取 `word/document.xml` 并拼接 `<w:t>`，缺少 zip 资源上限、段落、heading style 与 page break 的最小结构识别。

该问题会影响 RAG 索引质量、检索准确性和 citation 可信度，也可能让不可信文件内容以垃圾文本形式进入后续 RAG prompt。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 教师 | 课程资料上传者 | 上传 Markdown/TXT/PDF/DOCX 后，系统能稳定索引可读文本，无法解析时给出安全失败 |
| 学生 | RAG 问答使用者 | 检索结果不被二进制垃圾污染，引用内容更接近真实课程资料 |
| 运维/管理员 | 平台治理者 | parser 不引入未审查依赖，不因 DOCX zip 滥用导致 worker 资源失控 |

## 3. 用户故事

- 作为教师，我希望扫描 PDF 或不可解析 PDF 不会被系统误索引为乱码，以便课程问答不被垃圾内容污染。
- 作为教师，我希望 DOCX 的标题层级与分页能被尽量保留，以便 chunk metadata 更适合检索和引用。
- 作为管理员，我希望 parser 对 zip/DOCX 输入有资源边界，以便恶意或异常文件不会拖垮索引 worker。

## 4. MVP 范围

### 纳入范围

- PDF 仅索引明确抽取出的轻量文本，不再 fallback 原始二进制。
- PDF 支持 `Tj` 与简单 `TJ` 文本数组的无依赖抽取。
- DOCX 限制 zip entry 数与 `word/document.xml` 最大读取字节数。
- DOCX 按段落解析，best-effort 识别 `Heading1` 至 `Heading6`。
- DOCX best-effort 识别 page break 并输出 `pageNum`。
- TXT/Markdown 明显二进制或损坏 UTF-8 输入不产生 chunk。
- 通过测试验证 parser failure 只写安全错误码。

### 非目标

- 不做真实 OCR。
- 不接入 Apache PDFBox、Apache POI、Tika、docx4j、iText、Tesseract 或云 OCR。
- 不新增 Maven dependency。
- 不新增 DB schema、公开 API、前端页面。
- 不宣称复杂 PDF/DOCX 工业级解析完成。

## 5. 成功指标

| 指标 | 目标值 | 衡量方式 |
|---|---|---|
| PDF raw fallback | 0 | 无文本 PDF 测试不生成 raw `%PDF` chunk |
| DOCX 安全边界 | 有 | 超限 `word/document.xml` 测试触发 `DOCUMENT_PARSE_FAILED` |
| DOCX heading metadata | 有 | `Heading1/Heading2` 测试产生 `headingPath` |
| 回归稳定性 | 通过 | focused、adjacent、full backend Maven tests |
| 依赖漂移 | 无 | `backend/pom.xml` 不变，dependency tree 无 parser/OCR 新依赖 |

## 6. 用户流程

```text
- 用户上传文档
-> 索引任务读取文件 bytes
-> DocumentParserService 按类型解析
-> 可读文本产出 section / 不可信内容安全失败或空结果
-> IndexService 生成 chunk 与 metadata
-> 检索与引用只使用已验证 chunk
```

## 7. 依赖关系

- 依赖：现有 `rag/parser` 边界、`IndexService` chunk/hash/metadata 流程、现有安全错误码路径。
- 阻塞：无。

## 8. 待澄清问题

| 问题 | 负责人 | 状态 |
|---|---|---|
| 真实 OCR/复杂 PDF/DOCX 是否后续单独立项 | 产品/技术负责人 | 本次排除 |

## 9. 审批

| 角色 | 姓名 | 日期 | 状态 |
|---|---|---|---|
| Main Codex | Codex | 2026-06-08 | Accepted |
