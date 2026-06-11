# Context Pack - P3-2 RAG Hybrid Retrieval / RRF / Reranker Fallback

## 当前任务

执行 `TASK-20260608-rag-hybrid-retrieval`：在不新增依赖、不改 DB schema、不改 frontend 的前提下，为 RAG query 增加 keyword + recency + RRF、状态化 reranker fallback 和安全 query log metadata。

## 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`

## 关联文档

- PRD：`docs/product/PRD-20260608-rag-hybrid-retrieval.md`
- REQ：`docs/requirements/REQ-20260608-rag-hybrid-retrieval.md`
- SPEC：`docs/specs/SPEC-20260608-rag-hybrid-retrieval.md`
- PLAN：`docs/plans/PLAN-20260608-rag-hybrid-retrieval.md`
- TASK：`docs/tasks/TASK-20260608-rag-hybrid-retrieval.md`
- 集成评审：`docs/subagents/runs/RUN-20260608-rag-hybrid-retrieval-integration-review.md`
- Backend Expert：`docs/subagents/runs/RUN-20260608-rag-hybrid-retrieval-backend-expert.md`
- Security & Quality：`docs/subagents/runs/RUN-20260608-rag-hybrid-retrieval-security-quality.md`
- Test Plan：`docs/subagents/runs/RUN-20260608-rag-hybrid-retrieval-test-plan.md`

## 已选 Skills

- `feature-development-workflow`
- `educational-rag-pipeline`
- `agent-trace-governance`
- `multi-agent-coder`
- `subagent-driven-development`
- `test-driven-development`
- `verification-before-completion`
- `Confidence Check`

## Subagent 计划

### 是否启用 Subagent

是。

### 原因

任务涉及 RAG、backend service、security、testing，且用户明确要求专家 subagent 并行开发。

### 任务复杂度

| 影响模块数 | 涉及 Agent/RAG | 涉及安全 |
|---|---|---|
| 3+ | 是 | 是 |

### 选中的专家

- Backend Expert
- Agent/RAG Expert
- Security & Quality
- Test Engineer
- Integration Reviewer

### 并行级别

- [x] L1 - 并行分析
- [x] L2 - 并行设计
- [ ] L3 - worktree 并行实现

### 执行模式

主 Codex 串行实现；subagent 仅分析/设计/测试计划。

### 文件归属

| 领域 | 负责人 | 允许修改的文件 |
|---|---|---|
| RAG backend | Main Codex | `backend/src/main/java/com/learningos/rag/application/**` 中本任务列出的文件 |
| RAG tests | Main Codex | `RrfRankerTest.java`、`RagQueryServiceTest.java`、`OrchestratorWorkflowControllerTest.java` |
| 文档 | Main Codex | 本任务列出的 workflow/evidence/memory 文件 |

## 关联代码区域

- `backend/src/main/java/com/learningos/rag/application/`
- `backend/src/test/java/com/learningos/rag/application/`
- `backend/src/test/java/com/learningos/orchestrator/api/`

## 允许修改的文件

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

## 禁止修改的文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- 任何 secrets、credentials、用户私有文件。

## 测试命令

```powershell
cd backend
mvn --% -Dtest=RrfRankerTest,RagQueryServiceTest test
mvn --% -Dtest=RagQueryServiceTest,IndexServiceTest,OrchestratorWorkflowControllerTest test
mvn test
```

## 任务边界

只完成 P3-2 `hybrid retrieval、RRF、reranker timeout fallback` 最小切片。不得顺手处理 embedding/vector DB、复杂 parser/OCR、真实 Spring AI provider、RBAC/JWT。

## 完成状态

- 状态：已完成。
- 完成日期：2026-06-08。
- Evidence：`docs/evidence/EVIDENCE-20260608-rag-hybrid-retrieval.md`
- Acceptance：`docs/acceptance/ACCEPT-20260608-rag-hybrid-retrieval.md`
- Retrospective：`docs/retrospectives/RETRO-20260608-rag-hybrid-retrieval.md`
