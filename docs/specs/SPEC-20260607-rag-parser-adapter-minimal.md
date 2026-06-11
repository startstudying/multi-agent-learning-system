# SPEC - RAG Parser Adapter 最小生产切片

## 1. 数据与边界

本切片不新增数据库 schema。

新增或调整的仅是后端解析边界：

```text
backend/src/main/java/com/learningos/rag/parser/**
backend/src/main/java/com/learningos/rag/application/IndexService.java
```

## 2. Parser 边界

新增统一 parser service / adapter 层，负责根据文档类型输出解析结果。

### 2.1 输入

- `KbDocument`
- 原始文件字节

### 2.2 输出

每个解析结果至少包含：

- `parser`
- `title`
- `headingLevel`
- `headingPath`
- `content`
- `pageNum`

### 2.3 支持范围

- `MARKDOWN`
- `TXT`
- `PDF`
- `DOCX`

### 2.4 解析规则

- Markdown 必须识别 `#` 到 `######` 标题层级。
- TXT 输出单 section，保持当前简化行为。
- PDF / DOCX 保留当前轻量提取能力，不接真实 OCR。
- 空内容返回空 section 列表。
- 解析失败必须转成安全错误码，不暴露底层异常细节。

## 3. 服务边界

- `IndexService` 只负责索引流程编排、chunk 生产、hash、metadata、任务状态。
- parser 边界负责解析与 section 组织。
- worker/manual 路径必须复用同一 parser 边界。

## 4. 测试规则

- `DocumentParserServiceTest` 覆盖 parser selection 和各格式输出。
- `IndexServiceTest` 保留 chunk / hash / heading / worker 一致性回归。
- 若 parser 失败路径调整，测试必须断言安全错误码而非原始异常文本。

## 5. 实现结果

已实现。`DocumentParserService` 提供统一解析入口，`ParsedDocument` / `ParsedSection` 提供 section 输出结构，`IndexService` 继续负责 chunk/hash/metadata 和任务状态编排。失败路径通过 `DOCUMENT_PARSE_FAILED` 安全错误码收口。
