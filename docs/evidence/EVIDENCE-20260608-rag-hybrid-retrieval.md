# Evidence - P3-2 RAG Hybrid Retrieval / RRF / Reranker Fallback

## 1. 证据摘要

本切片完成无新增依赖、无 DB schema 变更、无 frontend 变更的 RAG 在线检索增强：

- allowed KB 内 `keyword + recency + RRF` 检索。
- `vectorEnabled=false` / `vectorCandidateCount=0`，不伪造 vector score。
- `RerankerService` 返回稳定状态枚举并支持 timeout/error fallback。
- `sourcesJson` 仅写白名单 retrieval / hybrid / reranker / source metadata。

## 2. TDD RED 证据

实施阶段先运行：

```powershell
cd backend
mvn --% -Dtest=RrfRankerTest,RagQueryServiceTest test
```

结果：`BUILD FAILURE`，符合预期。失败原因集中在目标能力尚未实现：

- 缺少 `RrfRanker`。
- `RerankerService` 构造器 / 状态化 API 未实现。
- 测试 fake reranker 所需 protected hook 尚不存在。

该失败证明测试先于实现暴露了本切片缺口。

## 3. Fresh GREEN 验证

### 3.1 聚焦测试

命令：

```powershell
cd backend
mvn --% -Dtest=RrfRankerTest,RagQueryServiceTest test
```

结果：

```text
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-08T02:47:13+08:00
```

覆盖点：

- `RrfRankerTest.fusesRankedBranchesByRrfScoreAndDeduplicatesChunks`
- `RrfRankerTest.appliesTopKAfterFusion`
- `RagQueryServiceTest.prefersKeywordRelevantChunkWhenQueryTermsMatchOnlyOneCandidate`
- `RagQueryServiceTest.rerankerTimeoutFallsBackToFusedCandidatesAndMarksDowngraded`
- `RagQueryServiceTest.rerankerProviderErrorIsSanitizedAndFallsBackToFusedCandidates`
- no-source、permission、replay、metrics tag 回归。

### 3.2 相邻回归

命令：

```powershell
cd backend
mvn --% -Dtest=RagQueryServiceTest,IndexServiceTest,OrchestratorWorkflowControllerTest test
```

结果：

```text
Tests run: 51, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-08T02:48:26+08:00
```

覆盖点：

- RAG query service。
- IndexService 相邻索引链路。
- Orchestrator `RAG_QA` query log / no-source 相邻断言。

### 3.3 全量后端测试

命令：

```powershell
cd backend
mvn test
```

结果：

```text
Tests run: 280, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-08T02:46:00+08:00
```

说明：Maven 输出包含 Mockito / ByteBuddy 动态 agent 的 JDK 未来兼容性 warning；本次不影响测试结果，后续可单独治理。

## 4. 需求到证据映射

| 需求 | 证据 | 状态 |
|---|---|---|
| FR-01 权限过滤先于 retrieval | `RagQueryServiceTest.rejectsMixedAllowedAndForbiddenKnowledgeBasesWithoutPersistingArtifacts` | PASS |
| FR-02 keyword 相关性排序 | `RagQueryServiceTest.prefersKeywordRelevantChunkWhenQueryTermsMatchOnlyOneCandidate` | PASS |
| FR-03 recency fallback | 单 chunk / 普通回答路径保持通过，`returnsAnswerSourcesAndTraceIdFromAllowedChunks` | PASS |
| FR-04 RRF 排序去重 | `RrfRankerTest` 两个用例 | PASS |
| FR-05 vector disabled metadata | `sourcesJson` 断言包含 `vectorEnabled=false` | PASS |
| FR-06 reranker 状态化结果 | `rerankerStatus` 断言覆盖 `NOT_CONFIGURED`、`SKIPPED_NO_CANDIDATES`、`TIMEOUT_FALLBACK`、`ERROR_FALLBACK` | PASS |
| FR-07 timeout/error fallback | timeout/error 用例均返回 sources 且 `downgraded=true` | PASS |
| FR-08 no-source 不伪造 citation | no-source 用例断言 citation 0 | PASS |
| FR-09 `rerankerStatus` 稳定枚举 | RAG 与 Orchestrator 测试断言新枚举 | PASS |
| FR-10 metadata 脱敏 | provider raw error 测试断言不含 `sk-test`、`raw chunk`、`provider said` | PASS |

## 5. 依赖 / Schema / Frontend Gate

| Gate | 结果 | 说明 |
|---|---|---|
| Dependency | PASS | 未修改 `backend/pom.xml`，未新增 Maven dependency。 |
| DB schema | PASS | 未新增或修改 `backend/src/main/resources/db/migration/**`。 |
| Frontend | PASS | 未修改 `frontend/**`。 |
| API contract | PASS | `/api/rag/query` 和 Orchestrator `RAG_QA` payload / response 不变。 |
| Query log | PASS | 复用 `kb_query_log.rerankerStatus` 和 `sourcesJson`。 |

## 6. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller / public API 不变；检索策略在 RAG application service 内；repository 仍只被 service 使用。 |
| Frontend rules | PASS | 未修改 frontend，未引入任何前端 LLM 调用。 |
| Agent / RAG rules | PASS | permission 先于 retrieval；no-source 不伪造 citation；RAG_QA trace/query/citation 合同保持。 |
| Security | PASS | 权限在后端校验；新增 metadata 白名单化；未新增依赖或 secret。 |
| API / Database | PASS | API 与 SPEC 一致；无 schema 变更；无未记录 endpoint。 |

## 7. 剩余边界

- 真实 embedding service 与 VectorDB adapter 未实现，仍归 P3-2 后续独立 TODO。
- 外部 reranker provider 未接入；当前 provider hook 仅用于受控测试与后续扩展。
- 既有 `question` / `responseJson` replay snapshot 合同未在本切片改变；query log retention / 脱敏治理需另立切片。
