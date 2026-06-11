# Context Pack - P3-2 子任务：Vector embedding payload contract

日期：2026-06-10

## Related Memory / Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/skills/project-specific/rag-embedding-vector-adapter.md`
- `docs/subagents/runs/RUN-20260610-p3-2-vector-embedding-payload-contract-review.md`

## Selected Skills

- feature-development-workflow
- educational-rag-pipeline
- spring-ai-agent-backend
- rag-embedding-vector-adapter
- test-driven-development

## Allowed Files

Production:

- `backend/src/main/java/com/learningos/rag/application/EmbeddingVector.java`
- `backend/src/main/java/com/learningos/rag/application/QueryEmbeddingResult.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingBatchResult.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorChunkReference.java`
- `backend/src/main/java/com/learningos/rag/application/VectorSearchRequest.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/ChunkService.java`

Tests:

- `backend/src/test/java/com/learningos/rag/application/EmbeddingServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/NoopVectorIndexAdapterTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceVectorPayloadTest.java`
- `backend/src/test/java/com/learningos/rag/application/ChunkServiceVectorRetrievalTest.java`

Docs:

- `docs/evidence/EVIDENCE-20260610-p3-2-vector-embedding-payload-contract.md`
- `docs/acceptance/ACCEPT-20260610-p3-2-vector-embedding-payload-contract.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/project-specific/rag-embedding-vector-adapter.md`

## Disallowed Files

- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- parser/OCR files
- Agent/Orchestrator files
- `.env` or secrets
- unrelated P3-4/RBAC modules

## Test Commands

RED / GREEN focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceVectorPayloadTest,ChunkServiceVectorRetrievalTest test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceEmbeddingVectorTest,IndexServiceTest,ChunkServiceVectorRetrievalTest,RagQueryServiceTest test
```

Full backend:

```powershell
cd D:\多元agent\backend
mvn test
```

## Boundary

本任务只补内部 vector payload contract。不得接入真实 VectorDB provider，不得新增依赖，不得关闭 P3-2 real VectorDB parent item。

