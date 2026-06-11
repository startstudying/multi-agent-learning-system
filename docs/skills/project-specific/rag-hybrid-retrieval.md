# RAG Hybrid Retrieval

## 使用场景

当实现或审查 RAG 在线检索、keyword retrieval、recency fallback、RRF fusion、reranker timeout/error fallback、检索 metadata、citation query log 时使用。

## 核心规则

1. retrieval 只能接收已通过 `PermissionService.requireReadableKbIds(...)` 的 allowed KB ids。
2. 不允许从 prompt、frontend payload 或 reranker provider 判断权限。
3. keyword branch 和 recency branch 必须在 allowed KB 范围内构造候选。
4. RRF fusion 必须按 chunk 去重，并在服务端 topK 上限内裁剪。
5. 没有真实 vector adapter 时必须明确写 `vectorEnabled=false` 和 `vectorCandidateCount=0`，不得伪造 vector score。
6. reranker 必须返回状态化结果，而不是裸 `List<KbDocChunk>`。
7. reranker timeout/error 应 fallback 到 fused candidates，不能让 RAG query 整体失败。
8. no-source 场景不得伪造 citation。
9. `kb_query_log.rerankerStatus` 应写稳定 reranker 状态枚举；业务路由策略继续写在 `sourcesJson.retrieval.strategy`。
10. `sourcesJson` 新增 metadata 只写枚举、计数、latency、boolean 和 source 白名单字段，不写 raw prompt、raw provider error、candidate full text、secret。

## 状态枚举建议

```text
NOT_CONFIGURED
SKIPPED_NO_CANDIDATES
SUCCEEDED
TIMEOUT_FALLBACK
ERROR_FALLBACK
```

## 测试建议

- `RrfRankerTest` 覆盖多 branch 排序、去重和 topK。
- `RagQueryServiceTest` 覆盖：
  - keyword 相关 chunk 优先于较新的无关 chunk；
  - keyword 无命中时 recency fallback；
  - mixed allowed/forbidden KB 检索前拒绝；
  - timeout fallback；
  - provider error fallback；
  - no-source 不写 citation；
  - `sourcesJson` 不含 raw provider error、secret、full chunk；
  - metrics tag 不包含高基数或敏感字段。
- Orchestrator `RAG_QA` 相邻测试要同步 `rerankerStatus` 新枚举。

## 反模式

- 在未实现 embedding/vector adapter 时声称 vector retrieval 已生效。
- 把 `rerankerStatus` 继续当 routing strategy 使用。
- 在 `sourcesJson` 中保存完整 chunk content、raw reranker request/response 或 raw exception message。
- 为了接真实 provider 顺手新增 dependency，却没有 dependency review / SPEC / PLAN / TASK。
- 修改 RAG API contract 但不更新 SPEC 与前端影响评估。
