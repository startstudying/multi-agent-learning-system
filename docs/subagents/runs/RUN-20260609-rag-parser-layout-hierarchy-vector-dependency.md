# P3-2-E / P3-2-D VectorDB 依赖专家报告摘要

## 1. 现有向量边界

- `VectorIndexAdapter` / `NoopVectorIndexAdapter` 已存在。
- `EmbeddingService` 已能通过 Spring AI OpenAI-compatible `EmbeddingModel` 生成真实向量，但当前 contract 不把向量对象暴露给 VectorDB adapter。
- `ChunkService` 已有 vector branch gate，并在 adapter 返回候选后执行 allowed-KB 二次过滤。

## 2. 对本轮 P3-2-E 的影响

- P3-2-E parser layout/page hierarchy 不需要新增 VectorDB 依赖。
- P3-2-D Real VectorDB adapter 应作为后续独立切片。

## 3. 后续 VectorDB 建议

- 优先考虑 Spring AI Qdrant VectorStore starter 或 Qdrant 官方 Java client。
- 新增依赖前必须创建 `docs/security/DEPENDENCY-REVIEW-*.md`，审查许可证、维护状态、CVE、传递依赖、TLS/timeout/secret 配置和部署治理。
- 最小实现需扩展 embedding contract，让内部 `EmbeddingService` 返回 `chunkId + vector`，但禁止将 raw vector 写入 metadata、sourcesJson、trace 或日志。

## 4. 结论

真实 VectorDB 不并入 P3-2-E。当前轮继续保持无新增依赖。
