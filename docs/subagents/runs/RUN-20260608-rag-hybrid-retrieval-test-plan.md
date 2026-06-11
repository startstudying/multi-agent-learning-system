# RUN-20260608 P3-2 Hybrid retrieval/RRF/reranker timeout fallback TDD 测试计划

角色：Test Engineer 子代理  
范围：只审计并制定测试计划，不改业务代码。  
允许写入：仅本文件。

## 0. Skill Selection Gate

1. 任务类型：测试策略 / TDD RED 测试计划 / 现有测试审计。
2. Selected skills：
   - `test-driven-development`：本任务要求为 Hybrid retrieval/RRF/reranker timeout fallback 制定 RED -> GREEN 验证路径。
   - `rag-project-review`：目标切片属于 RAG 检索链路。
   - `security-review`：覆盖权限过滤与 query log metadata 脱敏。
   - `test-generator`：输出可落地的 JUnit/Spring Boot 测试清单。
3. 选择原因：该切片会改变 RAG 检索排序、降级语义、审计 metadata 与 Orchestrator RAG_QA 链路，必须先用失败测试锁定行为。
4. Missing skills：无。
5. GitHub research needed：否。当前项目已有 RAG 服务、测试基线与子代理边界报告，最小切片无需外部参考。
6. New project-specific skill：暂不需要；若本切片实施后形成固定 Hybrid/RRF 测试模式，可后续沉淀 `rag-hybrid-retrieval-testing`。

## 1. 审计对象与当前覆盖

审计文件：

- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- 关联实现边界：
  - `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
  - `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
  - `backend/src/main/java/com/learningos/rag/application/RerankerService.java`

现有测试结论：

- `RagQueryServiceTest` 已覆盖：
  - RAG query 返回 answer/source/traceId/query log/citation。
  - `queryWithTraceId` 复用 Orchestrator traceId。
  - `requestId` response snapshot、重放、不一致 payload 409。
  - no-source refusal 不写 citation。
  - `RAG_WITH_PROFILE` / `RAG_WITH_HISTORY` 路由。
  - forbidden KB、mixed allowed+forbidden KB 不写 query/citation。
  - metrics tags 不含 `traceId/userId/requestId/kbId/documentId/question/prompt/source/errorMessage` 等敏感 tag。
- `IndexServiceTest` 主要覆盖文档解析、chunk token window、overlap、stable hash、heading metadata、worker/lease/retry；与 RAG query 相关的现有用例是 `processTxtPdfAndDocxIndexTasksProduceSearchableChunks`，只证明解析结果可检索，不覆盖 Hybrid retrieval/RRF/reranker。
- `OrchestratorWorkflowControllerTest` 已覆盖：
  - `RAG_QA` workflow 4 步：`workflow_start`、`step_rag_safety`、`step_rag_retrieval`、`step_rag_answer`。
  - query log/citation 复用 workflow traceId。
  - RAG_QA replay/conflict/exact envelope。
  - RAG_QA no-source 不写 citation。
  - RAG_QA forbidden runtime failure 持久化失败 evidence，但不写成功 query/citation artifact。

当前关键缺口：

- `ChunkService.retrieveAllowedChunks(...)` 当前仅按 `createdAt desc` 拉 allowed chunks，不根据 question 做关键词相关性排序。
- `RerankerService.rerankOrFallback(...)` 当前是 passthrough，没有 timeout/error fallback 可观测状态。
- `RagQueryService.queryResolved(...)` 当前没有把 retrieval mode、RRF、reranker fallback metadata 写入 `sourcesJson`。
- 现有 `rerankerStatus` 实际写入 `retrieval.strategy()`，不是 reranker 状态；后续断言需要明确兼容策略。

## 2. RED 测试清单

> 原则：每个测试只验证一个行为。先写 RED 并运行确认按预期失败，再实施 GREEN。

### A. `RagQueryServiceTest` 建议新增/调整

#### 1. `prefersKeywordRelevantChunkWhenQueryTermsMatchOnlyOneCandidate`

- 覆盖边界：关键词相关性。
- 构造数据：
  - 同一 allowed KB `kb_sql` 下 seed 两个 indexed chunk。
  - `doc_new_noise`：`createdAt` 较新，内容为 `"Graph traversal basics and breadth first search."`
  - `doc_old_join`：`createdAt` 较旧，内容为 `"SQL JOIN duplicates come from one-to-many cardinality."`
  - query：`"Why does SQL JOIN duplicate rows?"`
  - `topK=1`
- 期望行为：
  - `response.sources()` 只返回 `doc_old_join`。
  - `response.answer()` 包含 `"SQL JOIN duplicates"`。
  - `queryLog.sourcesJson` 包含安全 metadata，例如 `"retrievalMode":"HYBRID_RRF"` 或同等内部字段。
- 当前期望失败点：
  - 当前 `ChunkService` 按 `createdAt desc` 取 chunk，`topK=1` 很可能返回较新的噪声 chunk `doc_new_noise`。

#### 2. `deduplicatesChunksThatAppearInMultipleRetrievalChannelsDuringRrf`

- 覆盖边界：RRF 排序 / 去重。
- 构造数据：
  - 使用 fake/stub retrieval channel 或新增包内纯函数测试辅助，使 keyword candidates 与 vector candidates 都命中同一 chunk `chunk_join_core`。
  - keyword rank：`chunk_join_core`, `chunk_join_detail`
  - vector rank：`chunk_join_core`, `chunk_other`
  - `topK=3`
- 期望行为：
  - final sources 中 `documentId/chunkId` 不重复。
  - `candidateCount` 反映去重后的 fused candidate 数，或 `sourcesJson` 中明确记录 `keywordCandidateCount/vectorCandidateCount/fusedCandidateCount`。
  - `retrievalCount <= topK`。
- 当前期望失败点：
  - 当前没有 RRF/fusion 类型与去重逻辑；若直接追加 channel 结果，会重复 citation。

#### 3. `ordersFusedCandidatesByRrfScoreBeforeReranking`

- 覆盖边界：RRF 排序。
- 构造数据：
  - 建议新增 `RrfRankerTest` 作为纯单元测试；若必须放在 `RagQueryServiceTest`，则用 test-only fake channel。
  - keyword rank：A(1), B(2), C(3)
  - vector rank：B(1), C(2), A(10)
  - 预期 B 的 RRF 分数最高。
- 期望行为：
  - fused candidates 顺序以 B 开头，且不依赖输入 list 原顺序。
- 当前期望失败点：
  - 当前无 RRF ranker；只能 passthrough/createdAt 排序。

#### 4. `rerankerTimeoutFallsBackToFusedCandidatesAndMarksDowngraded`

- 覆盖边界：reranker timeout fallback。
- 构造数据：
  - 在 test config 中注入 fake `RerankerService` 或 fake reranker client，使 reranker 超过配置 timeout。
  - allowed KB 中 seed 2 个可命中 chunk。
  - `topK=2`
- 期望行为：
  - query 返回 OK，不抛 500。
  - sources 仍来自 fused candidates，数量不为 0。
  - `response.retrieval().downgraded()` 为 `true`。
  - `response.retrieval().message()` 只包含稳定降级语义，例如 `reranker timeout fallback`，不包含 raw exception。
  - `queryLog.sourcesJson` 包含固定状态 `TIMEOUT_FALLBACK`。
  - `sourceCitationRepository.count()` 等于最终 sources 数。
- 当前期望失败点：
  - 当前 `RerankerService` 没有 timeout 概念与 fallback metadata；`downgraded` 只由 no-source 或 `retrievalCount < candidateCount` 推导。

#### 5. `rerankerProviderErrorIsSanitizedAndFallsBackToFusedCandidates`

- 覆盖边界：reranker error fallback / query log metadata 脱敏。
- 构造数据：
  - fake reranker 抛出：`"provider apiKey=sk-test raw chunk very sensitive"`。
  - allowed KB 中 seed 1 个可命中 chunk。
- 期望行为：
  - query 返回 OK，不整体失败。
  - `sourcesJson` 包含固定状态 `ERROR_FALLBACK`。
  - `sourcesJson` / `response.retrieval().message()` 不包含 `sk-test`、`raw chunk`、`provider apiKey`、异常类堆栈。
- 当前期望失败点：
  - 当前 reranker passthrough 无异常分支；实现时若未 catch 会导致 query 500，或把 raw error 写入 metadata。

#### 6. `returnsNoSourceWhenHybridRetrievalFindsNoAllowedCandidates`

- 覆盖边界：no-source。
- 构造数据：
  - allowed KB `kb_empty` 无 chunk。
  - query 任意课程问题，`topK=5`。
- 期望行为：
  - 保持现有 no-source 行为：
    - `strategy == "NO_SOURCE_REFUSAL"`
    - `noSource == true`
    - `retrievalCount/candidateCount/citationCount == 0`
    - `sourceCitationRepository.count() == 0`
  - `sourcesJson` 可包含 `SKIPPED_NO_CANDIDATES`，但不能伪造 source。
- 当前期望失败点：
  - 如果 Hybrid fallback 错误地回退到全局最新 chunk，会破坏 no-source 与权限边界。

#### 7. `rejectsMixedAllowedAndForbiddenKnowledgeBasesBeforeHybridRetrieval`

- 覆盖边界：权限过滤。
- 构造数据：
  - `kb_allowed` 属于 `alice`，有 chunk。
  - `kb_hidden` 属于 `bob`，有 chunk。
  - query 请求 `List.of("kb_allowed", "kb_hidden")`。
- 期望行为：
  - 抛 `ApiException(FORBIDDEN)`。
  - `queryLogRepository.count() == 0`。
  - `sourceCitationRepository.count() == 0`。
  - fake keyword/vector/reranker 未被调用（若有 spy）。
- 当前期望失败点：
  - 现有 mixed KB 测试已覆盖 artifact 不写；Hybrid 实现若把 retrieval 放到权限前，新增 spy 断言会失败。

#### 8. `doesNotSendForbiddenVectorHitsToRrfOrReranker`

- 覆盖边界：权限过滤 / forbidden vector hit 防御。
- 构造数据：
  - fake vector channel 返回 allowed hit `chunk_allowed` 与 forbidden hit `chunk_hidden`。
  - 用户 `alice` 只请求/只允许 `kb_allowed`。
  - fake reranker 记录收到的 chunk ids。
- 期望行为：
  - response sources 不含 `doc_hidden`。
  - fake reranker received chunk ids 不含 `chunk_hidden`。
  - `sourcesJson` 不含 `chunk_hidden`、`kb_hidden`、hidden content。
- 当前期望失败点：
  - 当前无 vector channel；实现时若缺少二次过滤，此测试会捕获越权候选进入融合/重排。

#### 9. `queryLogStoresOnlyWhitelistedHybridMetadata`

- 覆盖边界：query log metadata 脱敏。
- 构造数据：
  - query 包含普通学习问题。
  - fake reranker 构造内部 prompt / provider request / raw candidate text。
- 期望行为：
  - `sourcesJson` 包含允许字段：`retrievalMode`、`keywordCandidateCount`、`vectorCandidateCount`、`fusedCandidateCount`、`rerankerStatus`、`fallbackUsed`。
  - `sourcesJson` 不包含：`rerankerPrompt`、`systemPrompt`、`rawProviderRequest`、`rawProviderResponse`、`candidateRawText`、`apiKey`、`secret`、`sk-`。
  - metrics tags 继续不包含高基数字段。
- 当前期望失败点：
  - 当前只断言 metrics tag 脱敏；没有针对 hybrid/reranker metadata 的白名单断言。

#### 10. `replaysHybridRagQueryWithoutRecomputingRetrievalOrReranker`

- 覆盖边界：query replay 与 fallback metadata 稳定。
- 构造数据：
  - 第一次 query 使用 `requestId=req_hybrid_replay`，fake reranker 返回 `TIMEOUT_FALLBACK`。
  - 第二次同 payload 同 requestId，但 fake reranker 若被调用则抛异常。
- 期望行为：
  - 第二次返回第一次 response snapshot。
  - `queryLogRepository.count() == 1`。
  - `sourceCitationRepository.count()` 不增加。
  - fake reranker 第二次未被调用。
- 当前期望失败点：
  - 现有 replay 测试只验证不重复 artifact；新增 spy 才能锁定不会重跑 retrieval/reranker。

### B. `OrchestratorWorkflowControllerTest` 建议新增/调整

#### 11. `createsRagQaWorkflowWhenRerankerTimeoutFallsBack`

- 覆盖边界：reranker timeout fallback + Orchestrator RAG_QA。
- 构造数据：
  - seed allowed KB/chunk。
  - 注入 fake reranker timeout。
  - POST `/api/orchestrator/workflows`，`workflowType=RAG_QA`，带 `X-Trace-Id=trc_orchestrator_rag_reranker_timeout`。
- 期望行为：
  - HTTP 200，`code=OK`。
  - workflow `status == DONE`。
  - steps 仍为 4 步，不新增破坏性步骤。
  - `steps[2].summary` 或 query log metadata 包含安全 fallback 状态。
  - `KbQueryLog.traceId == trc_orchestrator_rag_reranker_timeout`。
  - citation traceId 同 workflow traceId。
- 当前期望失败点：
  - 当前无 timeout fallback 状态；若未来实现 timeout 直接抛错，workflow 会 FAILED/500。

#### 12. `createsRagQaWorkflowWithHybridNoSourceAndNoCitations`

- 覆盖边界：no-source + Orchestrator。
- 构造数据：
  - seed empty allowed KB。
  - RAG_QA query。
- 期望行为：
  - 维持现有 `createsRagQaWorkflowWithNoSourceAndNoCitations` 断言。
  - 若新增 metadata，只允许 no-source/fallback 枚举状态，不写 raw candidate。
- 当前期望失败点：
  - Hybrid retrieval 若错误 fallback 到历史 latest chunks，会产生 citation。

#### 13. `ragQaWorkflowDoesNotPersistRawQuestionInTaskInputWhenHybridMetadataExists`

- 覆盖边界：query log metadata 脱敏 / workflow input 脱敏。
- 构造数据：
  - RAG_QA payload question：`"My private SQL JOIN learning question"`。
  - allowed KB 有 chunk。
- 期望行为：
  - `AgentTask.inputJson` 仍只含 `questionHash/questionLength/kbIdsHash/kbCount/topK`，不含 raw question。
  - `AgentTask.outputJson` 不含 raw question。
  - hybrid/reranker metadata 不被写入 task input 的 raw form。
- 当前期望失败点：
  - 现有 RAG_QA 创建测试已断言 `inputJson` 不含一个固定问题；新增测试更聚焦 Hybrid metadata 不能扩大敏感面。

### C. `IndexServiceTest` 建议保持为相邻回归，不承载 Hybrid 行为

#### 14. `processTxtPdfAndDocxIndexTasksProduceSearchableChunks`（现有测试保留）

- 覆盖边界：索引产物可被后续 keyword/hybrid 检索消费。
- 构造数据：现有 TXT/PDF/DOCX 三类文档。
- 期望行为：chunk content 与 parser metadata 仍存在。
- 当前期望失败点：
  - 本切片不应修改 indexing；若失败说明 Hybrid 实现越界影响索引生产链路。

不建议把 RRF/reranker timeout 放进 `IndexServiceTest`：这些属于 query-time retrieval/rerank 行为，不属于 index-time chunk production。

## 3. GREEN 后验证命令

推荐按 RED 粒度逐步验证：

```powershell
cd backend
mvn -Dtest=RagQueryServiceTest test
```

Orchestrator 集成回归：

```powershell
cd backend
mvn -Dtest=OrchestratorWorkflowControllerTest test
```

RAG 相邻回归：

```powershell
cd backend
mvn -Dtest=RagQueryServiceTest,IndexServiceTest,OrchestratorWorkflowControllerTest test
```

若新增 RRF 纯单元测试类：

```powershell
cd backend
mvn -Dtest=RrfRankerTest,RagQueryServiceTest test
```

最终后端回归：

```powershell
cd backend
mvn test
```

若实施中新增 Flyway migration（最小切片不建议）才需要追加：

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1
```

## 4. 必须覆盖的边界与建议断言

### 4.1 关键词相关性

- 至少两个候选 chunk，创建时间与相关性故意相反。
- `topK=1` 时必须返回关键词命中的旧 chunk，而不是最新噪声 chunk。
- query terms 需要可解释，例如 `SQL/JOIN/duplicates/cardinality`。

### 4.2 RRF 排序

- 用纯函数/小服务单元测试固定 rank 输入与输出顺序。
- 断言：
  - 同一 chunk 多 channel 命中只出现一次。
  - 排序由 RRF score 决定，不由输入顺序或 createdAt 决定。
  - `topK` 在 fused/reranked 后生效。

### 4.3 reranker timeout fallback

- fake slow reranker 必须稳定触发 timeout，不用 `Thread.sleep` 造成 flaky；建议用可控 `CompletableFuture` / latch / fake clock 或超短 timeout 配置。
- 断言：
  - RAG query/RAG_QA 不因 reranker timeout 失败。
  - final sources 来自 fused candidates。
  - `downgraded == true`。
  - metadata 是固定枚举 `TIMEOUT_FALLBACK`，不含 raw exception。

### 4.4 no-source

- 空 allowed KB 仍返回 `NO_SOURCE_REFUSAL`。
- 不写 `source_citation`。
- 不伪造 fallback source。
- Orchestrator step summary 仍能看出 `NO_SOURCE_REFUSAL`。

### 4.5 权限过滤

- mixed allowed+forbidden `kbIds` 整体拒绝，不能静默过滤后继续。
- vector/fake channel 返回 forbidden hit 时，必须在 RRF/reranker 前二次过滤。
- forbidden query 不写 `kb_query_log` / `source_citation`。
- reranker 输入不能包含 forbidden chunk。

### 4.6 query log metadata 脱敏

- 允许写入低敏枚举/计数：`retrievalMode`、`keywordCandidateCount`、`vectorCandidateCount`、`fusedCandidateCount`、`rerankerStatus`、`fallbackUsed`、`fallbackReasonCode`。
- 禁止写入：raw prompt、system prompt、raw provider request/response、raw error、candidate full text、forbidden ids/content、secret/API key/token。
- metrics tags 继续禁止高基数字段：`question/prompt/answer/excerpt/sourcesJson/responseJson/kbId/documentId/chunkId/userId/traceId/requestId`。

## 5. 现有断言可能需要同步的点

### 5.1 `RagQueryServiceTest.returnsAnswerSourcesAndTraceIdFromAllowedChunks`

当前强断言：

- `retrieval.strategy() == "COURSE_RAG"`
- `candidateCount == 1`
- `retrievalCount == 1`
- `citationCount == 1`
- `downgraded == false`
- `rerankerStatus == "COURSE_RAG"`
- `sourcesJson` 包含 `"strategy":"COURSE_RAG"`、`candidateCount`、`citationCount`、`downgraded`

同步建议：

- 若最小切片保持 `strategy` 表示业务路由，则 `COURSE_RAG` 不应改。
- 若 `candidateCount` 定义改为 fused 前总候选数，现有单 chunk seed 不应受影响，仍应为 1。
- 不建议把 `rerankerStatus` 改成 `SUCCEEDED/TIMEOUT_FALLBACK` 后直接破坏该断言；更安全做法：
  - 保持 `KbQueryLog.rerankerStatus` 兼容写业务 strategy；或
  - 同步断言为固定新语义，并在所有 Orchestrator 断言中一起修改。
- 更推荐将 reranker 细节放入 `sourcesJson` 新 metadata，减少旧断言波动。

### 5.2 `RagQueryServiceTest.returnsExplicitNoSourceMetadataAndDoesNotPersistCitationsWhenAllowedKbHasNoChunks`

当前强断言：

- `strategy == "NO_SOURCE_REFUSAL"`
- `queryComplexity == "SIMPLE"`
- `retrievalCount/candidateCount/citationCount == 0`
- `downgraded == true`
- `rerankerStatus == "NO_SOURCE_REFUSAL"`

同步建议：

- no-source 语义不应改变。
- 如果新增 `rerankerStatus=SKIPPED_NO_CANDIDATES`，需要同步该断言及 Orchestrator no-source 断言；否则继续保持 `rerankerStatus` 为业务 strategy，具体 reranker skip 状态放 `sourcesJson`。

### 5.3 `RagQueryServiceTest.routesDiagnosticQuestionsWithLearnerProfileCluesToProfileRag`

当前强断言：

- `strategy == "RAG_WITH_PROFILE"`
- `queryComplexity == "DIAGNOSTIC"`
- `rerankerStatus == "RAG_WITH_PROFILE"`

同步建议：

- Hybrid/RRF 是检索实现模式，不应覆盖业务路由策略。
- 不建议把 `strategy` 改为 `HYBRID_RRF`，否则会破坏 profile/history 路由语义。

### 5.4 `RagQueryServiceTest.routesQuestionsWithRecentAnswerHistoryCluesToHistoryRag`

同步建议同上：`RAG_WITH_HISTORY` 应保留为业务策略。

### 5.5 `RagQueryServiceTest.replaysExistingResponseWithSameRequestIdWithoutDuplicatingQueryArtifacts`

当前强断言：

- 第二次 replay 返回首次 traceId/answer。
- query/citation 不重复。
- replay 只记录 query count，不记录 duration timer。

同步建议：

- 不要把 retrieval implementation version 加入 `requestHash`，否则同 payload 可能变 409。
- 新增 hybrid metadata 若进入 `responseJson`，必须保持 Jackson 反序列化兼容。

### 5.6 `RagQueryServiceTest.rejectsMixedAllowedAndForbiddenKnowledgeBasesWithoutPersistingArtifacts`

当前强断言：

- mixed KB 抛 `FORBIDDEN`。
- query/citation 为 0。

同步建议：

- 增强为 spy/fake retriever 未调用，以证明权限过滤先于 Hybrid retrieval。

### 5.7 `OrchestratorWorkflowControllerTest.createsRagQaWorkflowAndReusesWorkflowTraceContext`

当前强断言：

- RAG_QA steps length 为 4。
- step ids 固定。
- `KbQueryLog.rerankerStatus == "COURSE_RAG"`。
- citation traceId 等于 workflow traceId。

同步建议：

- 不建议因为 reranker fallback 新增 workflow step；否则大量 workflow 查询断言要同步。
- 若 `rerankerStatus` 语义改为真实 reranker 状态，此处要同步；更推荐保持兼容，在 `sourcesJson` 断言新增 fallback/retrieval metadata。

### 5.8 `OrchestratorWorkflowControllerTest.createsRagQaWorkflowWithNoSourceAndNoCitations`

当前强断言：

- step summary 包含 `NO_SOURCE_REFUSAL`。
- `rerankerStatus == "NO_SOURCE_REFUSAL"`。
- citation 为 0。

同步建议：

- no-source 的外部 workflow 语义不应改变。
- 可新增 `sourcesJson` 中 `SKIPPED_NO_CANDIDATES` 断言，但不要移除 `NO_SOURCE_REFUSAL`。

## 6. 测试健康风险

- reranker timeout 测试最容易 flaky。必须避免真实等待；用 fake client/latch/可控 timeout 配置。
- 不建议在 `RagQueryServiceTest` 中引入真实外部 VectorDB/reranker 依赖；用 deterministic fake/stub。
- 如果新增 package-private `RrfRanker`，RRF 排序应优先用纯单元测试覆盖，避免 Spring 上下文成本与数据准备噪声。
- `queryLog.question` 当前仍持久化 raw question，这是既有设计；本切片测试重点是不扩大敏感面，尤其不能记录 raw prompt/provider/chunk full text。

## 7. 交付建议

最小 TDD 顺序：

1. 先写 `RrfRankerTest`：RRF 排序与去重 RED。
2. 写 `RagQueryServiceTest.prefersKeywordRelevantChunkWhenQueryTermsMatchOnlyOneCandidate`：证明当前 createdAt 排序错误。
3. 写 `RagQueryServiceTest.rerankerTimeoutFallsBackToFusedCandidatesAndMarksDowngraded`：锁定 fallback 合同。
4. 写权限与脱敏 RED：forbidden vector hit、query log 白名单。
5. 最后写 Orchestrator timeout fallback 集成 RED：证明 RAG_QA 仍 DONE 且 trace/citation 对齐。

