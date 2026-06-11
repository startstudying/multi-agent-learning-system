# Acceptance - P3-2 RAG Hybrid Retrieval / RRF / Reranker Fallback

## 1. 验收结论

状态：Accepted。

本切片已按 PRD / REQ / SPEC / PLAN / TASK 完成最小生产化目标：RAG 在线检索从单纯 recency 升级为 allowed KB 内 `keyword + recency + RRF`，并补齐 reranker timeout/error fallback 与安全 metadata。实现未新增 dependency、schema、frontend 或 API contract 变更。

## 2. Done Definition

| 项目 | 状态 | 证据 |
|---|---|---|
| PRD 存在 | PASS | `docs/product/PRD-20260608-rag-hybrid-retrieval.md` |
| REQ 存在 | PASS | `docs/requirements/REQ-20260608-rag-hybrid-retrieval.md` |
| SPEC 存在并已更新验收状态 | PASS | `docs/specs/SPEC-20260608-rag-hybrid-retrieval.md` |
| PLAN / TASK 存在并已关闭 | PASS | `docs/plans/PLAN-20260608-rag-hybrid-retrieval.md`、`docs/tasks/TASK-20260608-rag-hybrid-retrieval.md` |
| Context Pack 存在 | PASS | `docs/context/CONTEXT-20260608-rag-hybrid-retrieval.md` |
| Evidence 存在 | PASS | `docs/evidence/EVIDENCE-20260608-rag-hybrid-retrieval.md` |
| Changelog / Memory / TODO 更新 | PASS | `docs/changelog/CHANGELOG.md`、`docs/memory/*.md`、`docs/planning/backend-architecture-todolist.md` |
| Retrospective / Skill Extraction | PASS | `docs/retrospectives/RETRO-20260608-rag-hybrid-retrieval.md`、`docs/skills/project-specific/rag-hybrid-retrieval.md` |

## 3. 功能验收

| 验收项 | 结果 | 说明 |
|---|---|---|
| 权限过滤先于 retrieval | PASS | mixed allowed/forbidden KB 返回 `FORBIDDEN`，不写 query/citation。 |
| keyword 相关性排序 | PASS | 旧但相关 chunk 可排在较新的无关 chunk 前。 |
| recency fallback | PASS | keyword 无命中或普通单 chunk 场景仍可返回已授权最近 chunk。 |
| RRF 排序与去重 | PASS | `RrfRankerTest` 覆盖多 branch fusion、dedupe、topK。 |
| vector disabled metadata | PASS | `sourcesJson.hybrid.vectorEnabled=false`、`vectorCandidateCount=0`。 |
| reranker 状态化 | PASS | `NOT_CONFIGURED`、`SKIPPED_NO_CANDIDATES`、`TIMEOUT_FALLBACK`、`ERROR_FALLBACK` 已覆盖。 |
| timeout/error fallback | PASS | fallback 使用 fused candidates，query 不因 reranker 超时/异常失败。 |
| no-source | PASS | 返回 no-source 语义，不伪造 citation。 |
| `rerankerStatus` | PASS | query log 写稳定 reranker 状态枚举。 |
| metadata 脱敏 | PASS | 新增 metadata 不写 raw prompt、provider raw error、candidate full text 或 secret。 |

## 4. 验证结果

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=RrfRankerTest,RagQueryServiceTest test` | BUILD SUCCESS，17 tests，0 failures，0 errors |
| `mvn --% -Dtest=RagQueryServiceTest,IndexServiceTest,OrchestratorWorkflowControllerTest test` | BUILD SUCCESS，51 tests，0 failures，0 errors |
| `mvn test` | BUILD SUCCESS，280 tests，0 failures，0 errors，1 skipped |

## 5. 非目标确认

- 未实现真实 embedding service。
- 未接入 VectorDB adapter。
- 未接入外部 reranker SDK / provider。
- 未新增 Flyway migration。
- 未修改 frontend。
- 未改变 `/api/rag/query` 或 Orchestrator `RAG_QA` API contract。

## 6. 验收备注

本验收只关闭 P3-2 `hybrid retrieval、RRF、reranker timeout fallback` 最小切片。`embedding service 和可选 VectorDB adapter` 仍保持 TODO 未完成，不能把本切片理解为完整向量检索生产化。
