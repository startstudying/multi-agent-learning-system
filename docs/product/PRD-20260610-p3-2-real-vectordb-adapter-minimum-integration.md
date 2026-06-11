# PRD：P3-2 real VectorDB adapter minimum integration

## 背景

P3-2 RAG 索引生产化已经完成 parser boundary、OCR fallback、chunk metadata、embedding service、vector payload contract、hybrid retrieval / RRF / fallback 等基础能力。当前 RAG 检索仍缺少真实 VectorDB adapter，在线检索主要依赖 keyword + recency + RRF。

本子任务补齐真实 VectorDB adapter 的最小生产集成，使后端在显式配置 Qdrant 时可以将 document/query embedding vectors 接入 VectorDB，同时保持默认 Noop、安全降级和权限过滤。

## 目标

1. 引入官方 Spring AI Qdrant 相关依赖，但仍通过项目自有 `VectorIndexAdapter` 封装。
2. 默认配置下不连接外部 VectorDB。
3. 启用 Qdrant 时支持：
   - document chunks vector upsert / delete by document
   - query vector search
   - `allowedKbIds` filter 下推
4. 保持 MySQL chunk 为 citation 和内容事实源。
5. 保持 VectorDB failure 不破坏在线 RAG fallback。

## 非目标

- 不新增 REST API。
- 不改前端。
- 不改数据库 schema。
- 不新增 Testcontainers / Docker smoke。
- 不实现 VectorDB 管理 UI。
- 不做多 provider 抽象扩展。
- 不让 VectorDB 存 raw chunk content。

## 用户价值

- 课程资料检索可从纯 keyword/recency 升级到语义向量候选。
- 权限过滤仍由后端主链路控制。
- 生产环境可通过配置启用真实 VectorDB；开发/测试默认不受外部服务影响。

## 成功标准

1. 默认 `mvn test` 不需要真实 Qdrant。
2. `learning-os.rag.vector.enabled=false` 时 adapter 仍为 Noop。
3. 启用且配置完整时真实 Qdrant adapter 可被装配。
4. `VectorUpsertRequest` 不携带 raw content，adapter 也不向 VectorDB 写 raw content。
5. search 请求下推 `allowedKbIds`，服务层继续二次过滤。
6. provider 异常返回安全错误码。
7. focused、adjacent、full backend 测试通过。
