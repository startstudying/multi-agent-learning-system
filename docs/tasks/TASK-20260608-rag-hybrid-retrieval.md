# TASK - P3-2 RAG Hybrid Retrieval / RRF / Reranker Fallback

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260608-rag-hybrid-retrieval.md`
- SPEC：`docs/specs/SPEC-20260608-rag-hybrid-retrieval.md`
- 任务编号：TASK-20260608-rag-hybrid-retrieval

## 2. 目标

实现无新增依赖、无 schema 变更的 RAG hybrid-ready retrieval：keyword + recency + RRF、状态化 reranker timeout/error fallback、以及安全 query log metadata。

## 3. 范围

### 纳入范围

- RED tests 先行。
- `ChunkService` keyword/recency/RRF。
- `RerankerService` 状态化 fallback。
- `RagQueryService` metadata 与 log 持久化调整。
- Orchestrator RAG_QA 相邻回归。
- Evidence / Acceptance / Memory / Changelog / TODO 更新。

### 排除范围

- 真实 embedding/vector DB。
- 外部 reranker SDK。
- 新 dependency。
- DB migration。
- frontend。
- parser/OCR。

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
- `backend/src/main/java/com/learningos/rag/application/RerankerService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/AdaptiveRagRouter.java`
- `backend/src/main/java/com/learningos/rag/application/RetrievalResult.java`
- `backend/src/main/java/com/learningos/rag/application/RerankResult.java`
- `backend/src/main/java/com/learningos/rag/application/RerankerStatus.java`
- `backend/src/main/java/com/learningos/rag/application/RrfRanker.java`
- `backend/src/test/java/com/learningos/rag/application/RrfRankerTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `docs/product/PRD-20260608-rag-hybrid-retrieval.md`
- `docs/requirements/REQ-20260608-rag-hybrid-retrieval.md`
- `docs/specs/SPEC-20260608-rag-hybrid-retrieval.md`
- `docs/plans/PLAN-20260608-rag-hybrid-retrieval.md`
- `docs/tasks/TASK-20260608-rag-hybrid-retrieval.md`
- `docs/context/CONTEXT-20260608-rag-hybrid-retrieval.md`
- `docs/evidence/EVIDENCE-20260608-rag-hybrid-retrieval.md`
- `docs/acceptance/ACCEPT-20260608-rag-hybrid-retrieval.md`
- `docs/retrospectives/RETRO-20260608-rag-hybrid-retrieval.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/rag-hybrid-retrieval.md`

## 5. 禁止修改的文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- 与 RAG hybrid retrieval 无关的 controller/service/entity。

## 6. 实施步骤

1. 写 RED tests 并运行，确认失败。
2. 新增内部 retrieval/rerank 类型与 RRF ranker。
3. 实现 `ChunkService` keyword + recency + RRF。
4. 实现 `RerankerService` timeout/error fallback。
5. 调整 `RagQueryService` 和 `AdaptiveRagRouter`。
6. 调整 Orchestrator 相关断言。
7. 运行聚焦、相邻、全量测试。
8. 写 Evidence / Acceptance / Retrospective。
9. 更新 Changelog / Memory / TODO / Skill Registry。

## 7. 测试命令

```powershell
cd backend
mvn --% -Dtest=RrfRankerTest,RagQueryServiceTest test
mvn --% -Dtest=RagQueryServiceTest,IndexServiceTest,OrchestratorWorkflowControllerTest test
mvn test
```

## 8. 完成标准

- [x] RED tests 已先失败。
- [x] keyword 相关性排序生效。
- [x] RRF 排序与去重生效。
- [x] reranker timeout/error fallback 不导致 query 失败。
- [x] no-source 与 permission 回归通过。
- [x] `sourcesJson` 新 metadata 不含 raw prompt/provider error/full chunk。
- [x] 不新增 dependency/schema/frontend 改动。
- [x] 聚焦、相邻、全量后端测试已运行或记录限制。
- [x] Evidence 和 Acceptance 已创建。
- [x] Changelog、Memory、TODO 已更新。

## 9. 状态

| 字段 | 值 |
|---|---|
| 状态 | 已完成 |
| 负责人 | Main Codex |
| 开始日期 | 2026-06-08 |
| 完成日期 | 2026-06-08 |

## 10. 完成说明

- 代码实现保持在 Context Pack 允许范围内，不新增 Maven dependency、Flyway migration 或 frontend 改动。
- `kb_query_log.rerankerStatus` 已改为稳定 reranker 状态枚举，业务路由语义保留在 `sourcesJson.retrieval.strategy`。
- 本任务只完成无新增依赖的 hybrid-ready 在线检索切片；真实 embedding service / VectorDB adapter 仍是 P3-2 独立后续项。
