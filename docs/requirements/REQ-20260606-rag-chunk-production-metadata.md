# REQ - RAG Chunk 生产化元数据补齐

## 1. 用户需求

系统在索引课程文档时，需要生成稳定、可解释、便于后续检索优化的 chunk 元数据，而不是只依赖简单字符长度切分。

## 2. 业务需求

1. 索引过程必须支持固定窗口切分和 overlap。
2. 索引过程必须为每个 chunk 生成稳定 hash。
3. Markdown 文档必须保留 heading 层级上下文。
4. chunk 生产逻辑必须在手动索引和 worker 索引之间保持一致。
5. schema 必须能支持 chunk hash 的持久化和文档版本维度唯一约束。

## 3. 约束

- 不引入新依赖。
- 不改变公开查询 API。
- 不泄露原文、hash 原料或 overlap 原文到 metadata。
- 错误信息仍需保持安全脱敏。

## 4. 验收口径

- RED 测试先失败，GREEN 后通过。
- 单元测试覆盖 chunk 切分、hash、heading、worker 一致性。
- schema 测试覆盖 V17 文本。
- MySQL smoke 能验证最终表结构。

## 5. 交付状态

已完成。当前验收证据与回顾见 `docs/evidence/EVIDENCE-20260606-rag-chunk-production-metadata.md`、`docs/acceptance/ACCEPT-20260606-rag-chunk-production-metadata.md`、`docs/retrospectives/RETRO-20260606-rag-chunk-production-metadata.md`。
