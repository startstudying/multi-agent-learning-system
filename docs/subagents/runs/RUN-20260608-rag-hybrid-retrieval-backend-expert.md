# RUN-20260608 RAG Hybrid Retrieval Backend Expert 审计报告

## 任务范围

只读审计当前 RAG query / repository / service 边界，为 P3-2 Hybrid retrieval / RRF / reranker fallback 最小切片给出实现边界和测试影响。

审计重点文件：

- `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
- `backend/src/main/java/com/learningos/rag/application/RerankerService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/repository/KbDocChunkRepository.java`
- `backend/src/main/java/com/learningos/rag/domain/KbQueryLog.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`

本报告未修改业务代码。

## 结论摘要

P3-2 最小切片可以不变更现有外部 public API、DTO 和 MySQL 表结构：保持 `/api/rag/query` 请求/响应合同、`RagQueryService` 对 Controller / Orchestrator 的入口签名、`KbQueryLog` 现有字段不变，将 Hybrid/RRF/reranker fallback 的细节写入现有 `RetrievalMetadata` 和 `kb_query_log.sourcesJson` 即可。

推荐实现边界是：在 `ChunkService` 内收敛 keyword 候选 + 可插拔 vector 候选 + RRF 融合，仍只返回已授权 KB 的候选；在 `RerankerService` 内实现 timeout/异常 fallback，返回 rerank 结果和状态；`RagQueryService` 只负责串联、审计持久化、citation/trace/metrics，不应承载排序算法。

## 证据

### 当前检索链路

- `RagQueryService.queryResolved(...)` 当前顺序为：`chunkService.retrieveAllowedChunks(...)` -> `rerankerService.rerankOrFallback(...)` -> citation -> `adaptiveRagRouter.describe(...)` -> answer -> `persistQueryLog(...)` -> citation persistence -> metrics。
- `ChunkService.retrieveAllowedChunks(...)` 当前只调用 `KbDocChunkRepository.findTop20ByKbIdInOrderByCreatedAtDesc(...)`，然后按 `topK` 截断。
- `RerankerService.rerankOrFallback(...)` 当前直接原样返回 chunks。
- `KbDocChunkRepository` 当前只有 `findTop20ByKbIdInOrderByCreatedAtDesc(...)`、`findByDocumentIdOrderByChunkIndex(...)`、`deleteByDocumentId(...)`。
- `KbQueryLog` 当前已有 `retrievalCount`、`rerankerStatus`、`sourcesJson`、`latencyMs`、`requestId/requestHash/responseJson`，但无独立 `routing_strategy`、`candidate_count`、`fallback_reason`、`reranker_latency_ms` 列。

### 架构约束

- `docs/architecture/rag-architecture.md` 的在线链路明确要求：permission filter -> retrieve allowed chunks -> optional hybrid retrieval -> optional RRF -> optional reranker timeout fallback -> citations + traceId -> `kb_query_log`。
- 同文档要求：每次检索必须包含 `kb_id in allowedKbIds`，reranker failure 必须 fallback 到 fused TopN candidates。

### 当前测试断言

- `RagQueryServiceTest.returnsAnswerSourcesAndTraceIdFromAllowedChunks` 断言：
  - `retrieval.strategy() == "COURSE_RAG"`
  - `queryComplexity == "COMPLEX"`
  - `retrievalCount/candidateCount/citationCount == 1`
  - `downgraded == false`
  - `KbQueryLog.rerankerStatus == "COURSE_RAG"`
  - `sourcesJson` 包含 `"strategy":"COURSE_RAG"`、`candidateCount`、`citationCount`、`downgraded`
- `RagQueryServiceTest` 还覆盖 traceId 复用、requestId replay、NO_SOURCE、profile/history routing、forbidden/mixed KB、安全输入拦截。
- `OrchestratorWorkflowControllerTest.createsRagQaWorkflowAndReusesWorkflowTraceContext` 断言：
  - RAG_QA workflow 仍有 4 个步骤：`workflow_start`、`step_rag_safety`、`step_rag_retrieval`、`step_rag_answer`
  - `KbQueryLog.traceId` 等于 workflow traceId
  - `KbQueryLog.rerankerStatus == "COURSE_RAG"`
  - source citation traceId 等于 workflow traceId
- `OrchestratorWorkflowControllerTest.createsRagQaWorkflowWithNoSourceAndNoCitations` 断言 no-source 时 step summary 包含 `NO_SOURCE_REFUSAL`，`rerankerStatus == "NO_SOURCE_REFUSAL"`，citation 为 0。

## 1) 现有 public API / DTO / DB 是否需要变更

### Public API：不建议变更

不需要变更：

- `POST /api/rag/query`
- `GET /api/rag/query`
- Orchestrator `RAG_QA` payload 结构
- `RagQueryService` 当前对外入口：
  - `query(...)`
  - `queryWithTraceId(...)`
  - `queryWithRequestId(...)`
  - `queryWithTraceIdAndRequestId(...)`
  - `replayQueryIfPresent(...)`

原因：

- Hybrid/RRF/reranker fallback 属于服务端检索策略内部实现。
- 当前 `RagQueryResponse.retrieval` 已有 `strategy/candidateCount/retrievalCount/citationCount/downgraded/message`，足够承载最小审计信息。
- request replay hash 当前包含 `userId + allowedKbIds + question + topK + rag-query-v1`，如果检索策略升级但 API payload 不变，旧 requestId replay 仍会返回旧快照，符合当前 replay 语义。

### DTO：最小切片不建议变更

不建议新增外部 DTO 字段。可以通过现有字段表达：

- `retrieval.strategy()`：继续表示路由策略，如 `COURSE_RAG` / `RAG_WITH_PROFILE` / `RAG_WITH_HISTORY` / `NO_SOURCE_REFUSAL`。
- `retrieval.candidateCount()`：RRF 融合后的候选数量，或进入 reranker 前的候选数量，需在 SPEC 中固定定义。
- `retrieval.retrievalCount()`：最终用于生成 answer/citation 的 chunk 数。
- `retrieval.downgraded()`：最小切片建议在 reranker timeout/异常 fallback 时也置为 `true`，不只表示 no-source 或 final < candidate。
- `retrieval.message()`：可追加安全的 fallback 摘要，例如 `Reranker unavailable; used fused candidates.`，不得包含 chunk 原文、provider 原始错误、query 原文以外的敏感上下文。

如果未来要给前端显示更精细的检索诊断，再考虑新增内部/管理端 DTO；本切片不需要。

### DB：最小切片不建议变更

不需要新 migration。复用：

- `KbQueryLog.rerankerStatus`：建议仍存最终对外策略名，避免破坏现有测试；不要改成 `RERANKER_TIMEOUT`。
- `KbQueryLog.sourcesJson`：写入 `retrieval` 当前 DTO + 额外安全 metadata（如 `retrievalMode`, `rerankerStatus`, `fallbackUsed`, `candidateBreakdown`）更适合最小切片。
- `KbQueryLog.retrievalCount`：继续存最终 chunks 数。

不建议在最小切片新增列：

- `candidate_count`
- `routing_strategy`
- `fallback_reason`
- `reranker_latency_ms`
- `vector_candidate_count`

原因：当前 `sourcesJson` 已可承载审计细节；新增列会扩大 migration、analytics、ops alert、MySQL smoke 的影响面。

## 2) 推荐文件修改范围

### 必改业务文件

1. `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
   - 将 `retrieveAllowedChunks(List<String> allowedKbIds, int topK)` 从创建时间排序改为 hybrid retrieval 门面。
   - 内部先实现 keyword-only + RRF-ready 结构；如果当前无真实 vector adapter，可以提供空 vector candidate path，不引入外部依赖。
   - 必须确保所有 repository 查询都带 `allowedKbIds`。

2. `backend/src/main/java/com/learningos/rag/application/RerankerService.java`
   - 从原样返回改为返回带状态的结果对象。
   - 使用已有 `RagProperties.rerankerTimeoutMs` 作为 timeout 配置入口。
   - 无真实 reranker client 时可保留 deterministic fallback，但必须暴露 `SKIPPED` / `FALLBACK` 状态，便于测试。

3. `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
   - 适配新的 `ChunkService` / `RerankerService` 返回类型。
   - 保持 public 方法签名不变。
   - 持久化时继续保持 `rerankerStatus` 对现有断言兼容；详细 fallback metadata 放入 `sourcesJson`。
   - metrics tags 仍必须低基数，禁止引入 `kbId/documentId/question/traceId/requestId`。

4. `backend/src/main/java/com/learningos/rag/repository/KbDocChunkRepository.java`
   - 增加 keyword 检索方法，必须含 KB 授权条件。
   - 如使用 JPQL `lower(content) like lower(concat('%', :term, '%'))`，需要明确这是最小 keyword fallback，不是生产全文索引。

### 可选新增业务文件

建议新增小而纯的内部类型，减少 `RagQueryService` 膨胀：

- `backend/src/main/java/com/learningos/rag/application/RetrievalCandidate.java`
- `backend/src/main/java/com/learningos/rag/application/RetrievalResult.java`
- `backend/src/main/java/com/learningos/rag/application/RrfRanker.java`
- `backend/src/main/java/com/learningos/rag/application/RerankResult.java`

这些应是 package-private 或 application-layer 内部类型，不暴露到 API DTO。

### 不建议修改

- `RagQueryDtos.java`：最小切片不改。
- `KbQueryLog.java`：最小切片不改。
- Flyway migration：最小切片不新增。
- Controller / Orchestrator API 合同：不改。
- `SourceCitationRecord` 表结构：不改。

## 3) 需要新增/调整的方法签名

### `ChunkService`

推荐保留旧方法兼容现有调用，但调整内部返回为 richer result 后再逐步切换：

```java
public RetrievalResult retrieveAllowedChunks(
        List<String> allowedKbIds,
        String question,
        int topK
)
```

`RetrievalResult` 建议字段：

```java
record RetrievalResult(
        List<KbDocChunk> fusedCandidates,
        int keywordCandidateCount,
        int vectorCandidateCount,
        String retrievalMode
) {}
```

兼容策略：

- `RagQueryService` 改为调用新签名。
- 旧 `retrieveAllowedChunks(List<String>, int)` 可临时保留并委托 `retrieveAllowedChunks(allowedKbIds, "", topK).fusedCandidates()`，降低测试和其他调用点风险。

### `KbDocChunkRepository`

推荐新增：

```java
@Query("""
        select chunk
        from KbDocChunk chunk
        where chunk.kbId in :allowedKbIds
          and lower(chunk.content) like lower(concat('%', :term, '%'))
        order by chunk.createdAt desc
        """)
List<KbDocChunk> findKeywordCandidates(
        Collection<String> allowedKbIds,
        String term,
        Pageable pageable
);
```

同时保留现有：

```java
List<KbDocChunk> findTop20ByKbIdInOrderByCreatedAtDesc(Collection<String> allowedKbIds);
```

原因：现有方法可作为空 query / keyword 无命中 fallback，避免一次性改太多测试数据。

### `RerankerService`

推荐新增：

```java
public RerankResult rerankOrFallback(
        String question,
        List<KbDocChunk> fusedCandidates,
        int topK
)
```

`RerankResult` 建议字段：

```java
record RerankResult(
        List<KbDocChunk> chunks,
        String status,
        boolean fallbackUsed,
        Long latencyMs
) {}
```

状态建议：

- `SKIPPED_NO_CLIENT`
- `SUCCEEDED`
- `TIMEOUT_FALLBACK`
- `ERROR_FALLBACK`
- `EMPTY`

兼容策略：

- 保留旧 `rerankOrFallback(List<KbDocChunk> chunks)`，委托新方法：`rerankOrFallback("", chunks, chunks.size()).chunks()`。

### `RagQueryService`

不调整 public 方法签名。只调整 private `queryResolved(...)` 内部变量：

- `RetrievalResult retrievalResult = chunkService.retrieveAllowedChunks(allowedKbIds, question, limit);`
- `RerankResult rerankResult = rerankerService.rerankOrFallback(question, retrievalResult.fusedCandidates(), limit);`
- `adaptiveRagRouter.describe(...)` 当前只能基于 counts 和 question 生成 metadata；如需表达 reranker fallback，建议新增重载：

```java
public RetrievalMetadata describe(
        String question,
        int candidateCount,
        int retrievalCount,
        int citationCount,
        boolean rerankerFallbackUsed
)
```

或在 `RagQueryService` 构造 `RetrievalMetadata` 前后补 `downgraded/message`。更推荐新增 `AdaptiveRagRouter` 重载，保持 metadata 规则集中。

## 4) 可能破坏的现有测试

### 高风险断言

1. `RagQueryServiceTest.returnsAnswerSourcesAndTraceIdFromAllowedChunks`
   - 风险点：如果 keyword 检索基于 question term，现有 seed content 与 question token 匹配规则可能返回 0 或候选顺序变化。
   - 防护：keyword 无命中时 fallback 到当前 allowed chunks 创建时间检索；或测试新增第二个 chunk 明确断言 hybrid 排序。
   - 不应破坏的断言：`strategy == "COURSE_RAG"`、`candidateCount == 1`、`citationCount == 1`、`rerankerStatus == "COURSE_RAG"`。

2. `RagQueryServiceTest.returnsExplicitNoSourceMetadataAndDoesNotPersistCitationsWhenAllowedKbHasNoChunks`
   - 风险点：fallback 路径不能伪造 source；空 KB 必须仍是 `NO_SOURCE_REFUSAL`。
   - 防护：`RetrievalResult.fusedCandidates` 为空时 Reranker 返回 `EMPTY`，最终 sources 为空。

3. `RagQueryServiceTest.routesDiagnosticQuestionsWithLearnerProfileCluesToProfileRag`
   - 风险点：如果 `retrieval.strategy` 被误改为 `HYBRID_RRF`，会破坏现有 profile/history 路由语义。
   - 防护：`strategy` 继续表示业务路由策略；retrieval implementation mode 放入 `sourcesJson` 内部 metadata。

4. `RagQueryServiceTest.replaysExistingResponseWithSameRequestIdWithoutDuplicatingQueryArtifacts`
   - 风险点：改变 `requestHash` 版本或把检索策略加入 hash 会导致同 payload replay 冲突。
   - 防护：最小切片不改 `requestHash(...)` 的 `"rag-query-v1"`。

5. `RagQueryServiceTest.rejectsMixedAllowedAndForbiddenKnowledgeBasesWithoutPersistingArtifacts`
   - 风险点：vector/hybrid 候选若不经 allowed KB 过滤，可能返回 forbidden chunk 并写 citation。
   - 防护：ChunkService 和任何 Vector adapter 均只接受 `allowedKbIds`；RagQueryService 可加二次过滤作为 defense-in-depth。

### Orchestrator 相关风险

1. `OrchestratorWorkflowControllerTest.createsRagQaWorkflowAndReusesWorkflowTraceContext`
   - 风险点：`KbQueryLog.rerankerStatus` 当前断言为 `COURSE_RAG`。如果实现把它改成 `SUCCEEDED` / `TIMEOUT_FALLBACK` 会失败。
   - 建议：`rerankerStatus` 字段继续写业务策略名；reranker 细节写 `sourcesJson`。

2. `OrchestratorWorkflowControllerTest.createsRagQaWorkflowWithNoSourceAndNoCitations`
   - 风险点：no-source 的 workflow step summary 仍需包含 `NO_SOURCE_REFUSAL`。
   - 建议：`AdaptiveRagRouter` no-source 策略不变。

3. RAG_QA replay / conflict 测试
   - 风险点：如果新增 retrieval metadata 进入 `responseJson`，同一次 requestId replay 返回旧 response 应仍完全可反序列化。
   - 建议：不要新增 required DTO 字段；如果新增内部 metadata，只存在 `sourcesJson`，不进入 API response。

### 建议新增测试

新增/调整 `RagQueryServiceTest`：

- keyword 命中优先：两个 chunks 中只有一个包含 query keyword，最终 source 为该 chunk。
- RRF 去重：keyword 和 vector/fake 候选命中同一 chunk 时最终 sources 不重复。
- topK 上限：`topK=1` 时 fused/reranked 后只返回 1 个 source。
- reranker timeout fallback：fake slow reranker 超时后仍返回 fused candidates，citation 不丢，`sourcesJson` 标记 fallback。
- reranker 异常 fallback：异常不导致 RAG_QA 失败，除非候选为空。
- forbidden vector hit 防护：fake vector 返回未授权 chunk id 时被过滤，不写 citation。

新增/调整 `OrchestratorWorkflowControllerTest`：

- RAG_QA 在 reranker fallback 下仍 DONE，4 步不变，query log/citation traceId 仍等于 workflow traceId。
- RAG_QA no-source 在 hybrid 开启后仍不写 citation。

## 5) 最小 Maven 验证命令

在 `backend` 目录运行：

```powershell
mvn -Dtest=RagQueryServiceTest,OrchestratorWorkflowControllerTest test
```

如果修改了 repository JPQL 或 JPA entity 映射，再加：

```powershell
mvn -Dtest=ChatControllerTest,RagQueryServiceTest,OrchestratorWorkflowControllerTest test
```

如果新增 Flyway migration（本报告不建议最小切片新增），还必须运行项目已有 MySQL migration smoke 对应命令；具体命令应以 `docs/harness/TEST_COMMANDS.md` 为准。

## 推荐实施边界

### 最小可接受切片

1. `ChunkService`：
   - permission-scoped keyword retrieval；
   - RRF-ready fused candidate list；
   - 无 vector adapter 时 vector candidates 为 0；
   - 不返回未授权 KB chunk。

2. `RerankerService`：
   - 新 result type；
   - timeout/异常 fallback；
   - fallback 后保留 fused TopN。

3. `RagQueryService`：
   - public API 不变；
   - query log / citation / trace / metrics 不回退；
   - `rerankerStatus` 兼容旧断言；
   - `sourcesJson` 写安全 metadata。

4. 测试：
   - 先补 RED tests，再实现；
   - 优先覆盖权限过滤、fallback 不丢 citation、Orchestrator traceId 对齐。

### 明确不纳入最小切片

- 真实 VectorDB 接入；
- embedding 生成/持久化；
- 外部 reranker SDK/HTTP client；
- 新 DB columns；
- 前端 DTO 展示改造；
- RAG evaluation benchmark 自动化调度；
- 复杂 parser/OCR。

## 给 Planner 的开放问题

- 是否允许最小切片只做 keyword + RRF-ready 空 vector path，而不接真实 vector adapter？
- `retrieval.candidateCount` 在 hybrid 后应定义为 RRF 融合前总候选数、去重后 fused candidates 数，还是进入 reranker 前数量？
- `KbQueryLog.rerankerStatus` 是否必须保持现有业务策略名以兼容测试，reranker 细节统一放 `sourcesJson`？
- reranker timeout fallback 是否应把 `retrieval.downgraded` 置为 `true`，即使 final source 数等于 candidate 数？
