# RUN-20260608 RAG Hybrid Retrieval 集成评审

## 1. 评审目标

本评审汇总 P3-2 `hybrid retrieval / RRF / reranker timeout fallback` 并行专家输出，形成可实施的最小切片边界。

相关专家报告：

- `docs/subagents/runs/RUN-20260608-rag-hybrid-retrieval-backend-expert.md`
- `docs/subagents/runs/RUN-20260608-rag-hybrid-retrieval-security-quality.md`
- `docs/subagents/runs/RUN-20260608-rag-hybrid-retrieval-test-plan.md`

Agent/RAG 专家报告因子代理只读约束未落盘，其结论已通过通知返回并纳入本评审。

## 2. 冲突与决策

| 议题 | 专家意见 | 集成决策 |
|---|---|---|
| 是否新增依赖 | 全部建议不新增依赖 | 不新增依赖，不接真实 VectorDB / reranker provider |
| 是否改 DB schema | 全部建议最小切片不改 schema | 不新增 migration，复用 `kb_query_log.reranker_status` 和 `sources_json` |
| `retrieval.strategy` 语义 | 测试专家建议继续表达业务路由 | 保持 `COURSE_RAG` / `RAG_WITH_PROFILE` / `NO_SOURCE_REFUSAL` 等业务路由语义 |
| `kb_query_log.rerankerStatus` 语义 | Backend 建议兼容旧 strategy；Security 建议改稳定 reranker 枚举 | 改为稳定 reranker 状态枚举；routing strategy 继续写在 `sourcesJson.retrieval.strategy`，同步测试与文档 |
| 是否实现真实 hybrid vector | 专家均指出没有 embedding/vector 前置 | 实现 `HYBRID_RRF` ready：keyword + recency fallback + RRF；vector count 记录为 0 / disabled |
| query log 脱敏 | Security 指出现有 `question/responseJson/sourcesJson` 是敏感面 | 本切片不改变既有 replay snapshot 合同；新增 hybrid/reranker metadata 只写枚举、计数、latency，不写 raw prompt、candidate full text、provider error |

## 3. 最终实施边界

纳入范围：

1. `ChunkService` 从“最近 20 条”升级为：
   - allowed KB 内 bounded candidate pool；
   - question keyword scoring；
   - recency fallback branch；
   - RRF fusion；
   - vector branch disabled metadata。
2. `RerankerService` 增加状态化结果：
   - `NOT_CONFIGURED`
   - `SKIPPED_NO_CANDIDATES`
   - `SUCCEEDED`
   - `TIMEOUT_FALLBACK`
   - `ERROR_FALLBACK`
3. `RagQueryService` 写入安全 metadata：
   - `sourcesJson.retrieval`
   - `sourcesJson.hybrid`
   - `sourcesJson.reranker`
   - `sourcesJson.sources` 仅保留 source metadata，不新增 raw prompt / raw reranker request / raw provider error。
4. TDD 覆盖：
   - RRF 排序去重；
   - keyword 相关性优先于 recency；
   - reranker timeout/error fallback；
   - no-source 不伪造 citation；
   - mixed forbidden KB 仍检索前拒绝；
   - metrics tag 脱敏。

不纳入范围：

- 真实 Spring AI embedding；
- VectorDB adapter；
- 外部 reranker SDK / HTTP client；
- DB migration；
- frontend 展示；
- parser/OCR 增强。

## 4. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller / Orchestrator public API 不变，RAG 策略位于 application service |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 权限过滤仍先于 retrieval；RAG response 保持 sources |
| Security | PASS | 不新增依赖；新增 metadata 白名单化 |
| API / Database | PASS | 不改 API contract，不改 schema |

## 5. 后续实施建议

按 TDD 顺序执行：

1. 新增 RED tests：`RrfRankerTest` 与 `RagQueryServiceTest` 聚焦行为。
2. 实现内部 retrieval/rerank 类型和最小算法。
3. 调整相邻 Orchestrator 断言。
4. 运行聚焦、相邻、完整 Maven 验证。
