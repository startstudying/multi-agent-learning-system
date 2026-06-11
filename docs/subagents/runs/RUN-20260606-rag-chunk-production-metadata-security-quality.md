# RUN - RAG Chunk Production Metadata - Security & Quality

## 1. 角色

Security & Quality Reviewer。

## 2. 结论

本切片可以实现，但必须避免把原始正文、hash 原料、overlap 片段或异常详情写入 `metadataJson`。新增 schema 应使用文档维度唯一约束，不得使用全局 hash 唯一约束。

## 3. 安全要求

- `chunkHash` 使用 SHA-256，不使用 Java `hashCode()`、MD5 或可预测弱摘要。
- `metadataJson` 只保存结构化元数据：parser、embeddingModel、contentLength、chunkingStrategy、chunkTokenCount、overlapTokenCount、headingLevel、headingPath。
- 不在 metadata 中保存原文片段、hash 输入材料、storage key、对象路径、异常文本或用户私密数据。
- 唯一约束使用 `(document_id, document_version, chunk_hash)`，避免跨文档泄露相同内容存在性。
- 不新增公开 chunk 详情 API，不改变 RAG query response。
- 不新增依赖；若未来引入 tokenizer、PDF parser、VectorDB adapter，应单独走 dependency review。

## 4. 质量要求

- RED 测试必须先覆盖：稳定 hash、overlap、heading hierarchy、重复正文不同标题不误去重、V17 migration。
- worker 路径与手动 `processIndexTask` 路径必须共享相同 chunk 生产逻辑。
- 失败错误仍使用现有安全错误码，不持久化原始异常。

## 5. 已知非本切片问题

已有 dependency-check 报告中存在无关 high/critical CVE；本切片不引入新依赖，也不在本切片修复这些历史依赖问题。若需要处理，应另立安全治理任务。
