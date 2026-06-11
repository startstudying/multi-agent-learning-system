# PRD - RAG Parser Adapter 最小生产切片

## 1. 背景

当前 RAG 索引链路已经补齐了 chunk 生产元数据、worker、恢复、MySQL smoke，但文档解析仍然是 `IndexService` 内部的轻量实现。PDF、DOCX、Markdown、TXT 的解析入口没有独立边界，后续一旦引入真实 parser / OCR / VectorDB，风险会被放大。

## 2. 目标

抽出一个最小可生产的 parser adapter 边界，在不新增依赖、不改变公开 RAG 查询 API 的前提下，把 TXT / Markdown / PDF / DOCX 的解析能力统一到单独的 parser 层，并保持当前索引与 worker 行为一致。

## 3. 范围

纳入：

- 抽出 `rag/parser/**` 解析边界。
- 统一 TXT / Markdown / PDF / DOCX 的 section 输出。
- 保留当前轻量 PDF / DOCX 解析能力，但不引入真实 OCR。
- 让 `IndexService` 与 worker 路径复用同一 parser 边界。
- 让 parser 失败输出安全错误码，不泄露原始异常或文件内容。
- 更新单测、聚焦回归和文档。

不纳入：

- 真实 OCR 引擎。
- Apache Tika / PDFBox / POI 等新依赖。
- VectorDB / embedding / hybrid retrieval / RRF / reranker。
- 公开 RAG query / citation API 改动。

## 4. 成功标准

- `IndexService` 不再内嵌全部 parser 逻辑。
- 同一文档在手动索引与 worker 索引路径上产出的 section 结构一致。
- Markdown heading hierarchy、page/section 输出、chunk 生产测试继续通过。
- parser 失败只返回安全错误码，不暴露原始 provider / 文件路径 / 文件内容。

## 5. 非目标

本切片不负责把所有生产级 parser/OCR 一次做完。它只完成 parser 边界抽取和统一输出结构，为后续 OCR、复杂 PDF/DOCX、VectorDB、embedding 铺路。

## 6. 交付状态

已完成。实现已补齐 Evidence、Acceptance 和 Retrospective；本切片只完成最小 parser adapter 边界，不包含真实 OCR、复杂 PDF/DOCX parser、新依赖、VectorDB 或公开 API 变更。
