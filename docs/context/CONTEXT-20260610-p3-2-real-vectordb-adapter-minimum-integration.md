# CONTEXT：P3-2 real VectorDB adapter minimum integration

## Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/project-specific/rag-embedding-vector-adapter.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/subagents/runs/RUN-20260610-p3-2-real-vectordb-adapter-minimum-integration.md`
- `docs/security/DEPENDENCY-REVIEW-20260610-p3-2-real-vectordb-adapter-minimum-integration.md`

## Selected Skills

- feature-development-workflow
- educational-rag-pipeline
- spring-ai-agent-backend
- rag-embedding-vector-adapter
- security-review
- dependency-review
- test-driven-development

## Subagent Plan

已完成并行专家分析：

- Dependency/Security Expert
- RAG Architecture Expert
- Test/Verification Expert

实现由 Main Codex 单线集成，避免并行修改同一 Java package。

## Allowed Files

- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/learningos/config/RagVectorProperties.java`
- `backend/src/main/java/com/learningos/rag/application/NoopVectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorConfiguration.java`
- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorOperations.java`
- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorPoint.java`
- `backend/src/main/java/com/learningos/rag/vector/NativeQdrantVectorOperations.java`
- `backend/src/test/java/com/learningos/rag/vector/QdrantVectorIndexAdapterTest.java`
- `backend/src/test/java/com/learningos/rag/vector/RagVectorConfigurationTest.java`
- `docs/product/PRD-20260610-p3-2-real-vectordb-adapter-minimum-integration.md`
- `docs/requirements/REQ-20260610-p3-2-real-vectordb-adapter-minimum-integration.md`
- `docs/specs/SPEC-20260610-p3-2-real-vectordb-adapter-minimum-integration.md`
- `docs/plans/PLAN-20260610-p3-2-real-vectordb-adapter-minimum-integration.md`
- `docs/tasks/TASK-20260610-p3-2-real-vectordb-adapter-minimum-integration.md`
- `docs/context/CONTEXT-20260610-p3-2-real-vectordb-adapter-minimum-integration.md`
- `docs/evidence/EVIDENCE-20260610-p3-2-real-vectordb-adapter-minimum-integration.md`
- `docs/acceptance/ACCEPT-20260610-p3-2-real-vectordb-adapter-minimum-integration.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## Disallowed Files

- `backend/src/main/java/**/controller/**`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `backend/src/main/java/com/learningos/orchestrator/**`
- `backend/src/main/java/com/learningos/rag/parser/**`
- unrelated P3-4 permission/auth code

## Test Commands

```powershell
cd backend
mvn --% -Dtest=QdrantVectorIndexAdapterTest,RagVectorConfigurationTest test
mvn --% -Dtest=NoopVectorIndexAdapterTest,IndexServiceVectorPayloadTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest,RagQueryServiceTest test
mvn test
```

## Current Boundary

本任务只实现 Qdrant 最小 adapter 与配置装配，不做真实服务 smoke、collection 管理或 dimension validation、health/ops endpoint、gRPC/Netty 风险处理、DB/API/frontend 改动。

## Post-Implementation Notes

- `NoopVectorIndexAdapter` 保留为普通实现类，不作为 component-scanned conditional service。
- 默认禁用场景下的 Noop bean 由 `QdrantVectorConfiguration` 通过 `@ConditionalOnMissingBean(VectorIndexAdapter.class)` 提供。
- `NativeQdrantVectorOperations` 的 upsert 使用 `setVectors(...)` + `putAllPayload(...)`，search 使用 `setWithPayload(...)` include `chunkId` + `setWithVectors(...enable=false)`；真实服务 smoke、collection dimension validation、health/ops、gRPC/Netty 风险处理仍是后续独立子任务。
