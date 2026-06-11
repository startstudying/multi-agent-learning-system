# Context Pack - P3-2-A RAG Embedding Service 与可选 VectorDB Adapter 边界

## 1. 当前任务

执行 `TASK-20260608-rag-embedding-vector-adapter`：为 RAG 离线索引和在线检索补齐 embedding service 与 optional VectorDB adapter 边界，默认 disabled/noop，不接真实外部 provider。

状态：已完成。

## 2. 相关记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 3. 相关文档

- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/architecture/rag-architecture.md`
- `docs/security/SECRET_POLICY.md`
- `docs/security/DEPENDENCY_REVIEW_TEMPLATE.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/rag-hybrid-retrieval.md`
- `docs/skills/project-specific/rag-parser-boundary.md`
- `docs/skills/project-specific/model-gateway-boundary.md`

## 4. Selected Skills

- `feature-development-workflow`
- `educational-rag-pipeline`
- `agent-trace-governance`
- `test-driven-development`
- `verification-before-completion`
- `Confidence Check`
- `rag-hybrid-retrieval`
- `rag-parser-boundary`
- `model-gateway-boundary`
- `dependency-review`
- `security-review`

## 5. Subagent Plan

| 项 | 决策 |
|---|---|
| Use Subagents | Yes |
| Level | L1/L2 analysis/design |
| Implementation | Main Codex 串行 |
| Reports | `docs/subagents/runs/RUN-20260608-rag-embedding-vector-*.md` |

集成评审：`docs/subagents/runs/RUN-20260608-rag-embedding-vector-integration-review.md`。

## 6. 允许修改文件

### Code

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

### Tests

- `backend/src/test/java/com/learningos/rag/application/EmbeddingServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/NoopVectorIndexAdapterTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceEmbeddingVectorTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/ChunkServiceVectorRetrievalTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/RrfRankerTest.java`

### Docs

- `docs/product/PRD-20260608-rag-embedding-vector-adapter.md`
- `docs/requirements/REQ-20260608-rag-embedding-vector-adapter.md`
- `docs/specs/SPEC-20260608-rag-embedding-vector-adapter.md`
- `docs/plans/PLAN-20260608-rag-embedding-vector-adapter.md`
- `docs/tasks/TASK-20260608-rag-embedding-vector-adapter.md`
- `docs/context/CONTEXT-20260608-rag-embedding-vector-adapter.md`
- `docs/evidence/EVIDENCE-20260608-rag-embedding-vector-adapter.md`
- `docs/acceptance/ACCEPT-20260608-rag-embedding-vector-adapter.md`
- `docs/retrospectives/RETRO-20260608-rag-embedding-vector-adapter.md`
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
- `.env` 或任何 credentials 文件。

## 8. Test Commands

```powershell
cd backend
mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest,IndexTaskWorkerSchedulerTest,RagQueryServiceTest,RrfRankerTest test
mvn --% -Dtest=RagQueryServiceTest,DocumentControllerTest,OrchestratorWorkflowControllerTest,IndexServiceTest,IndexTaskWorkerSchedulerTest test
mvn test
```

## 9. 当前边界

- 本切片只完成 boundary/noop/fake。
- 不处理复杂 PDF/DOCX/OCR。
- 不处理真实 Spring AI provider。
- 不处理真实 VectorDB dependency 或部署。
- 不把本切片解释为整个 `backend-architecture-todolist.md` 完成。

## 10. 接管说明

接管时工作区已存在部分 embedding/vector boundary 代码与测试。本轮已完成测试、修正安全边界、运行验证和证据闭环。后续真实 provider/VectorDB 接入必须新建 Context Pack 并重新执行 dependency/security review。
