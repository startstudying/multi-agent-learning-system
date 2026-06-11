# REQ - P3-2 RAG Hybrid Retrieval / RRF / Reranker Fallback

## 1. 追踪

- PRD：`docs/product/PRD-20260608-rag-hybrid-retrieval.md`
- 需求编号：REQ-20260608-rag-hybrid-retrieval

## 2. 功能需求

| 编号 | 需求描述 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | RAG 检索必须在 `PermissionService.requireReadableKbIds(...)` 之后执行。 | 必须 | mixed allowed/forbidden KB 返回 `FORBIDDEN`，不写 query/citation |
| FR-02 | `ChunkService` 必须基于 question tokens 对 allowed KB chunk 做 keyword 相关性排序。 | 必须 | 旧但相关 chunk 可排在新但无关 chunk 前 |
| FR-03 | 检索必须包含 recency fallback branch，keyword 无命中时仍可使用已授权最近 chunk。 | 必须 | 普通单 chunk 场景保持可回答 |
| FR-04 | 多分支候选必须通过 RRF 融合并按 chunk 去重。 | 必须 | RRF 单测覆盖排序和去重 |
| FR-05 | 没有真实 vector adapter 时不得伪造 vector score；metadata 应记录 vector disabled/count=0。 | 必须 | `sourcesJson.hybrid.vectorEnabled=false` |
| FR-06 | `RerankerService` 必须返回状态化结果，不得只返回 list。 | 必须 | timeout/error/no-candidate/not-configured 均有固定状态 |
| FR-07 | reranker timeout/error 时 RAG query 不应整体失败，必须 fallback 到 fused candidates。 | 必须 | query 返回 OK，`downgraded=true`，sources 不丢失 |
| FR-08 | no-source 场景不得伪造 citations。 | 必须 | 空 KB 返回 `NO_SOURCE_REFUSAL`，citation 0 |
| FR-09 | `kb_query_log.rerankerStatus` 必须写稳定 reranker 状态枚举。 | 必须 | 测试断言 `NOT_CONFIGURED` / `TIMEOUT_FALLBACK` / `ERROR_FALLBACK` / `SKIPPED_NO_CANDIDATES` |
| FR-10 | `sourcesJson` 必须写 hybrid/reranker 安全 metadata。 | 必须 | 包含枚举、计数、latency；不含 raw prompt/provider error/full chunk |

## 3. 非功能需求

| 编号 | 需求描述 | 优先级 |
|---|---|---|
| NFR-01 | 不新增依赖。 | 必须 |
| NFR-02 | 不新增或修改 DB schema。 | 必须 |
| NFR-03 | 不修改 frontend。 | 必须 |
| NFR-04 | metrics tags 不得包含 question、prompt、answer、excerpt、sourcesJson、responseJson、kbId、documentId、chunkId、userId、traceId、requestId。 | 必须 |
| NFR-05 | topK 必须保持有界，避免 hybrid 后扩大检索成本。 | 必须 |

## 4. 用户流程

### 流程 1：正常课程问答

```text
学生提交 question + kbIds
-> 后端检查安全输入
-> 后端校验 KB 权限
-> keyword/recency retrieval
-> RRF fusion
-> reranker 未配置则跳过
-> 返回 answer + sources + retrieval metadata
-> 写 query log 和 source citation
```

### 流程 2：reranker timeout

```text
学生提交 question
-> allowed candidates 已融合
-> reranker 超时
-> fallback 到 fused candidates
-> response 标记 downgraded
-> query log 写 TIMEOUT_FALLBACK
```

## 5. 输入 / 输出

### 输入

沿用既有 RAG query 输入，不新增字段。

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| `kbIds` | `List<String>` | 是 | 非空；必须全部可读 |
| `question` | `String` | 是 | 非空；通过 content safety |
| `topK` | `Integer` | 否 | 小于等于 0 时使用默认；服务端上限 20 |
| `requestId` | `String` | 否/Orchestrator 必填 | 不超过 120 |

### 输出

沿用既有 `RagQueryResponse`。

| 字段 | 类型 | 说明 |
|---|---|---|
| `answer` | `String` | RAG answer 或 no-source answer |
| `sources` | `List<SourceCitation>` | 引用来源 |
| `traceId` | `String` | trace id |
| `retrieval` | `RetrievalMetadata` | 业务 routing、候选数、引用数、降级状态 |

## 6. 边界情况

| 场景 | 预期行为 |
|---|---|
| `kbIds` 混合 allowed/forbidden | 返回 `FORBIDDEN`，不写 query/citation |
| allowed KB 无 chunk | 返回 `NO_SOURCE_REFUSAL`，不写 citation |
| keyword 无命中但有 chunk | 使用 recency fallback |
| reranker 未配置 | query 成功，`rerankerStatus=NOT_CONFIGURED` |
| reranker 超时 | query 成功，`downgraded=true`，`rerankerStatus=TIMEOUT_FALLBACK` |
| reranker 异常 | query 成功，`downgraded=true`，`rerankerStatus=ERROR_FALLBACK` |
| same requestId replay | 返回首次 `responseJson`，不重新写 query/citation |

## 7. 依赖关系

- 上游依赖：RAG foundation、permission strict hardening、query replay snapshot、citation persistence。
- 下游影响：后续 embedding/vector/reranker provider 接入。

## 8. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | Approved |

## 9. 完成状态

| 项 | 状态 | 证据 |
|---|---|---|
| FR-01 - FR-10 | 已完成 | `docs/acceptance/ACCEPT-20260608-rag-hybrid-retrieval.md` |
| NFR-01 不新增依赖 | 已完成 | 未修改 `backend/pom.xml` |
| NFR-02 不改 DB schema | 已完成 | 未新增或修改 Flyway migration |
| NFR-03 不改 frontend | 已完成 | 未修改 `frontend/**` |
| NFR-04 metrics tag 脱敏 | 已完成 | `RagQueryServiceTest` 回归 |
| NFR-05 topK 有界 | 已完成 | `RrfRankerTest` 与 service 实现 |

完成日期：2026-06-08。
