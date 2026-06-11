# PLAN - P3-2-A RAG Embedding Service 与可选 VectorDB Adapter 边界

## 1. 追踪

- PRD：`docs/product/PRD-20260608-rag-embedding-vector-adapter.md`
- REQ：`docs/requirements/REQ-20260608-rag-embedding-vector-adapter.md`
- SPEC：`docs/specs/SPEC-20260608-rag-embedding-vector-adapter.md`

## 2. Skill Selection Report

### Task Type

RAG / retrieval / indexing 边界增强；后端 service 内部行为变更；安全与测试增强。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 用户目标属于后端系统增强，必须走完整 Spec-first 闭环。 |
| `educational-rag-pipeline` | 涉及 RAG indexing、embedding、retrieval、citation。 |
| `agent-trace-governance` | RAG query log、trace、安全 metadata 与 AI 治理相关。 |
| `test-driven-development` | 新 contract 和回归必须 RED/GREEN。 |
| `verification-before-completion` | 完成前必须 fresh verification。 |
| `Confidence Check` | 实施前确认无重复、架构合规、根因清晰。 |
| `rag-hybrid-retrieval` | 本切片直接衔接 keyword/recency/RRF/vector-disabled metadata。 |
| `rag-parser-boundary` | `IndexService` 继续消费 parser boundary 输出，不回退到格式解析。 |
| `model-gateway-boundary` | embedding provider 后续必须遵循模型调用边界和安全错误码。 |
| `dependency-review` | 本切片不新增依赖；后续 provider/VectorDB 必须启用。 |
| `security-review` | vector search 权限、metadata 最小化和 secret 防泄漏。 |

### Missing Skills

无。本轮不需要新增真实 provider/VectorDB 选型技能。

### GitHub Research Needed

No。当前切片不选型外部 SDK、不新增依赖。真实 provider/VectorDB 接入时再开启 GitHub Reference Gate 和官方文档核验。

### New Project-Specific Skill To Create

完成后沉淀 `rag-embedding-vector-adapter.md`。

## 3. Subagent Decision

| 项 | 决策 |
|---|---|
| Use Subagents | Yes |
| Reason | 用户明确要求专家 subagent 并行；任务涉及 RAG、backend、security、testing。 |
| Parallelism Level | L1/L2 |
| Selected Subagents | Backend Expert、Agent/RAG/Test、Security & Quality、Integration Reviewer |
| Implementation Mode | Main Codex 串行实现；不启用 L3 并行编码 |

已落盘报告：

- `docs/subagents/runs/RUN-20260608-rag-embedding-vector-backend-expert.md`
- `docs/subagents/runs/RUN-20260608-rag-embedding-vector-test-plan.md`
- `docs/subagents/runs/RUN-20260608-rag-embedding-vector-integration-plan.md`
- `docs/subagents/runs/RUN-20260608-rag-embedding-vector-security-quality.md`
- `docs/subagents/runs/RUN-20260608-rag-embedding-vector-integration-review.md`

## 4. Confidence Check

| Check | Result | Evidence |
|---|---|---|
| No duplicate implementations | PASS | 搜索到仅有占位 `EmbeddingService` 与新边界草稿，无真实 provider/VectorDB 实现。 |
| Architecture compliance | PASS | 符合 `Controller -> Service -> Adapter` 与 RAG 权限先行规则。 |
| Official docs verified | N/A/PASS | 不接外部 SDK/API；真实 provider 后续再查官方文档。 |
| OSS implementation referenced | N/A/PASS | 不引入外部实现；本轮只写本地 contract/noop。 |
| Root cause identified | PASS | P3-2 TODO 明确剩余 embedding service / optional VectorDB adapter；当前 vector 只能 disabled。 |

Confidence：0.92。可进入实现，但必须补足 vector candidate 二次过滤与 metadata 最小化测试。

## 5. 实施阶段

| 阶段 | 内容 | 状态 |
|---|---|---|
| 1 | 补齐集成评审与 PRD/REQ/SPEC/PLAN/TASK/Context | 已完成 |
| 2 | 补 RED tests：embedding/noop/vector retrieval/index metadata | 已完成 |
| 3 | 收敛 embedding/vector boundary 代码 | 已完成 |
| 4 | 运行 focused/adjacent/full Maven tests | 已完成 |
| 5 | Evidence/Acceptance/Changelog/Memory/TODO/Retro/Skill | 已完成 |

## 6. 允许修改文件

代码：

- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/ChunkEmbeddingInput.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingBatchResult.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingStatus.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/application/NoopVectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexStatus.java`
- `backend/src/main/java/com/learningos/rag/application/VectorSearchRequest.java`
- `backend/src/main/java/com/learningos/rag/application/VectorSearchResult.java`
- `backend/src/main/java/com/learningos/rag/application/VectorUpsertRequest.java`
- `backend/src/main/java/com/learningos/rag/application/VectorUpsertResult.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
- `backend/src/main/java/com/learningos/rag/application/RetrievalResult.java`

测试：

- `backend/src/test/java/com/learningos/rag/application/EmbeddingServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/NoopVectorIndexAdapterTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/ChunkServiceVectorRetrievalTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/RrfRankerTest.java`

文档：

- 本切片 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/EVIDENCE/ACCEPT/RETRO
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/rag-embedding-vector-adapter.md`

## 7. 禁止修改文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- 与本切片无关的 Agent/Assessment/Resource/Knowledge 模块。

## 8. 测试命令

```powershell
cd backend
mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceTest,ChunkServiceVectorRetrievalTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest,IndexTaskWorkerSchedulerTest,RagQueryServiceTest,RrfRankerTest test
mvn --% -Dtest=RagQueryServiceTest,DocumentControllerTest,OrchestratorWorkflowControllerTest,IndexServiceTest,IndexTaskWorkerSchedulerTest test
mvn test
```

## 9. 风险与缓解

| 风险 | 缓解 |
|---|---|
| vector adapter 返回越权 chunk | 服务层按 `allowedKbIds` 二次过滤，并测试 forbidden candidate。 |
| adapter 接收或记录 raw chunk | upsert request 尽量只携带最小 chunk 标识；metadata 不写 content/vector。 |
| 配置 embedding model 后索引全部失败 | 这是本切片预期的安全失败；真实 provider 后续切片解决。 |
| 当前已有部分代码先于文档存在 | 纳入本 Context Pack 审查，后续改动按 Spec-first 和 TDD 补齐。 |

## 10. 回滚策略

- 回滚 application-layer 新增类型与 `IndexService`/`ChunkService` 接入点。
- 因不改 schema/dependency/frontend，回滚无需数据库迁移。

## 11. 完成状态

状态：已完成。

完成证据：

- `docs/evidence/EVIDENCE-20260608-rag-embedding-vector-adapter.md`
- `docs/acceptance/ACCEPT-20260608-rag-embedding-vector-adapter.md`
- `docs/retrospectives/RETRO-20260608-rag-embedding-vector-adapter.md`
- `docs/skills/project-specific/rag-embedding-vector-adapter.md`
