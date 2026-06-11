# Spring Boot RAG Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the phase-1 backend foundation for the AI Learning OS and leave a clear phase-by-phase path toward Course RAG, multi-agent resource generation, assessment feedback, mastery updates, and learning-path replanning.

**Architecture:** Use a Spring Boot modular monolith. Phase 1 creates the backend base, common infrastructure, security context, dependency configuration, health checks, and empty-but-compilable module boundaries; Phase 2 implements the Course RAG domain and keeps offline indexing separate from online query.

**Tech Stack:** Java 21, Spring Boot 3.x, Spring AI, Maven, Spring Web, Validation, Spring Security or Sa-Token-compatible auth adapter, Data JPA, Flyway, PostgreSQL + PGVector, Redis 7, MinIO, Docker Compose, JUnit 5, Spring MockMvc.

> Update 2026-06-05: The runtime database decision has changed. Current implementation uses MySQL 8 as the primary database with `flyway-mysql` and `mysql-connector-j`; vector storage is optional and adapter-based. Treat PostgreSQL/PGVector references below as historical plan context, not current implementation direction.

---

## Source Documents Read

Highest priority project documents:

- `docs/research/reference-priority.md`
- `docs/architecture/overview.md`
- `docs/architecture/rag-architecture.md`
- `docs/api/contract.md`
- `docs/data/model.md`
- `docs/superpowers/plans/2026-06-04-learning-agent-initialization.md`
- `docs/superpowers/specs/2026-06-04-learning-agent-initialization-design.md`
- `README.md`

Additional user-provided planning prompt:

- `C:\Users\wonderful\.codex\attachments\09cfb7b2-c12a-4862-a6b6-98ba63c3a646\pasted-text.txt`

## Current Repository Baseline

- Current repository contains `docs/` and `frontend/`.
- `README.md` states that backend business code has not been generated yet.
- `frontend/` is a Vue 3 + Vite + TypeScript placeholder workbench.
- `docs/api/contract.md` defines the current backend API boundary.
- `docs/data/model.md` defines table-level model direction and says RAG implementation should prefer `kb_*` table names.
- There is no `backend/` directory yet.

## Full Product Phase Map

### Phase 0: Project Initialization And Documentation Baseline

Status from current repository evidence: mostly complete.

Scope:

- Vue placeholder frontend.
- README and architecture documents.
- API contract draft.
- Data model draft.
- RAG architecture draft.
- Initial plan/spec documents.

Verification:

```powershell
cd frontend
npm test -- --run
npm run build
```

### Phase 1: Backend Infrastructure

Primary scope for immediate execution.

Goal:

- Create Spring Boot backend foundation that can compile, start, expose health checks, attach trace IDs, build a user context, and connect to local infrastructure placeholders.

Deliverables:

- `backend/` Maven project.
- Common API envelope.
- Global exception handling.
- Trace ID filter.
- Auth/UserContext skeleton compatible with Sa-Token/JWT replacement.
- Health controller.
- Docker Compose for PostgreSQL + PGVector, Redis 7, and MinIO.
- Dev/test profiles.
- Empty module packages for future RAG, Agent, Learning, Assessment work.

Out of scope for Phase 1:

- Real document parsing.
- Real embeddings.
- Vector retrieval.
- LLM generation.
- Agent execution.
- Frontend rewrite.

### Phase 2: Knowledge Base And Course RAG Foundation

Goal:

- Implement knowledge base creation, document upload, async index tasks, chunk persistence, hybrid retrieval skeleton, SSE RAG answers, and citations.

Key constraint:

- Offline indexing and online query pipelines must be separate services.

### Phase 3: Learner Profile And Learning Path

Goal:

- Add learner profile, learning goals, knowledge state/mastery, and learning path generation with traceable reasons.

### Phase 4: Multi-Agent Resource Generation

Goal:

- Add Planner, Teacher, Resource, Question, Critic, and Tutor agent execution with auditable traces and review states.

### Phase 5: Assessment, Feedback, And Path Replanning

Goal:

- Add question answering, grading, wrong-cause analysis, mastery update, and learning-path replanning.

### Phase 6: Frontend Workbench Completion

Goal:

- Upgrade Vue placeholder into an operational workbench for knowledge bases, RAG chat, citations, learning paths, resources, traces, assessment, and mastery.

### Phase 7: Engineering, Observability, And Deployment

Goal:

- Add integration tests, permission tests, Agent trace tests, logs, metrics, Dockerfiles, seed data, API docs, environment templates, and deployment docs.

Current repository evidence:

- `backend/Dockerfile` defines the backend container image build.
- `backend/.env.example` and root `.env.example` provide environment templates.
- `docs/api/reference.md`, `docs/operations/deployment.md`, `docs/architecture/observability.md`, and `docs/data/seed-data.md` cover API, deployment, observability, and seed-data guidance.
- `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java` covers knowledge-base permission behavior.
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java` covers resource generation and Agent trace exposure.
- Metrics dashboards, structured log policy, durable Agent trace persistence, and production seed loaders remain future Phase 7 hardening work.

## Phase 1 Acceptance Criteria

- `backend/` exists and builds with Java 21 and Spring Boot 3.x.
- `GET /api/health` returns app, database, Redis, MinIO, and model configuration status fields.
- Every request receives or creates a trace ID and returns it in `X-Trace-Id`.
- Errors use the common `{code,message,data}` response envelope.
- Dev authentication creates a `UserContext` from headers while leaving a clear seam for Sa-Token/JWT.
- Docker Compose starts PostgreSQL + PGVector, Redis 7, and MinIO.
- `dev` and `test` profiles are present and documented.
- Empty module packages exist for `common`, `security`, `trace`, `config`, `user`, `knowledge`, `rag`, `agent`, `learning`, and `assessment`.
- Phase 1 tests pass.

## Phase 2 Acceptance Criteria

- `POST /api/knowledge-bases` creates a knowledge base.
- `GET /api/knowledge-bases` lists only knowledge bases allowed for the current user.
- `POST /api/knowledge-bases/{kbId}/documents` accepts multipart upload, stores the raw object through MinIO abstraction, creates `kb_document`, and creates `kb_index_task` in `PENDING`.
- `GET /api/documents/{documentId}` returns parse, chunk, embedding, and index status.
- `POST /api/documents/{documentId}/reindex` creates a new async index task without deleting old chunks.
- `POST /api/rag/query` returns `answer`, `sources`, and `traceId`.
- `GET /api/chat/sessions/{sessionId}/stream` emits `status`, `token`, and `done` SSE events.
- Retrieval service signatures require allowed KB IDs; no search method may execute without hard permission filtering.

## Phase 1 File Structure

Backend build and config:

- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/learningos/LearningOsApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `backend/src/main/resources/application-test.yml`
- Create: `backend/docker-compose.yml`
- Create: `backend/.env.example`

Common infrastructure:

- Create: `backend/src/main/java/com/learningos/common/api/ApiResponse.java`
- Create: `backend/src/main/java/com/learningos/common/api/ErrorCode.java`
- Create: `backend/src/main/java/com/learningos/common/exception/ApiException.java`
- Create: `backend/src/main/java/com/learningos/common/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/learningos/common/trace/TraceContext.java`
- Create: `backend/src/main/java/com/learningos/common/trace/TraceFilter.java`
- Create: `backend/src/main/java/com/learningos/common/auth/UserContext.java`
- Create: `backend/src/main/java/com/learningos/common/auth/UserContextHolder.java`
- Create: `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`

Configuration:

- Create: `backend/src/main/java/com/learningos/config/AppProperties.java`
- Create: `backend/src/main/java/com/learningos/config/StorageProperties.java`
- Create: `backend/src/main/java/com/learningos/config/RagProperties.java`
- Create: `backend/src/main/java/com/learningos/config/AiModelProperties.java`
- Create: `backend/src/main/java/com/learningos/config/AsyncConfig.java`

Health:

- Create: `backend/src/main/java/com/learningos/health/api/HealthController.java`
- Create: `backend/src/main/java/com/learningos/health/application/HealthService.java`
- Create: `backend/src/main/java/com/learningos/health/api/HealthDtos.java`

Placeholder modules:

- Create: `backend/src/main/java/com/learningos/user/package-info.java`
- Create: `backend/src/main/java/com/learningos/knowledge/package-info.java`
- Create: `backend/src/main/java/com/learningos/rag/package-info.java`
- Create: `backend/src/main/java/com/learningos/agent/package-info.java`
- Create: `backend/src/main/java/com/learningos/learning/package-info.java`
- Create: `backend/src/main/java/com/learningos/assessment/package-info.java`

Tests:

- Create: `backend/src/test/java/com/learningos/LearningOsApplicationTests.java`
- Create: `backend/src/test/java/com/learningos/common/trace/TraceFilterTest.java`
- Create: `backend/src/test/java/com/learningos/common/auth/DevAuthFilterTest.java`
- Create: `backend/src/test/java/com/learningos/common/exception/GlobalExceptionHandlerTest.java`
- Create: `backend/src/test/java/com/learningos/health/api/HealthControllerTest.java`

Documentation:

- Modify: `README.md`

## Phase 2 File Structure

RAG API:

- Create: `backend/src/main/java/com/learningos/rag/api/KnowledgeBaseController.java`
- Create: `backend/src/main/java/com/learningos/rag/api/DocumentController.java`
- Create: `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- Create: `backend/src/main/java/com/learningos/rag/api/dto/KnowledgeBaseDtos.java`
- Create: `backend/src/main/java/com/learningos/rag/api/dto/DocumentDtos.java`
- Create: `backend/src/main/java/com/learningos/rag/api/dto/RagQueryDtos.java`

RAG application services:

- Create: `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java`
- Create: `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- Create: `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- Create: `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
- Create: `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- Create: `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- Create: `backend/src/main/java/com/learningos/rag/application/RerankerService.java`
- Create: `backend/src/main/java/com/learningos/rag/application/PermissionService.java`

RAG domain:

- Create: `backend/src/main/java/com/learningos/rag/domain/KnowledgeBase.java`
- Create: `backend/src/main/java/com/learningos/rag/domain/KbPermission.java`
- Create: `backend/src/main/java/com/learningos/rag/domain/KbDocument.java`
- Create: `backend/src/main/java/com/learningos/rag/domain/KbDocChunk.java`
- Create: `backend/src/main/java/com/learningos/rag/domain/KbIndexTask.java`
- Create: `backend/src/main/java/com/learningos/rag/domain/KbChatSession.java`
- Create: `backend/src/main/java/com/learningos/rag/domain/KbChatMessage.java`
- Create: `backend/src/main/java/com/learningos/rag/domain/KbQueryLog.java`
- Create: `backend/src/main/java/com/learningos/rag/domain/enums/DocumentStatus.java`
- Create: `backend/src/main/java/com/learningos/rag/domain/enums/IndexTaskStatus.java`
- Create: `backend/src/main/java/com/learningos/rag/domain/enums/Visibility.java`

RAG repositories and storage:

- Create: `backend/src/main/java/com/learningos/rag/repository/KnowledgeBaseRepository.java`
- Create: `backend/src/main/java/com/learningos/rag/repository/KbPermissionRepository.java`
- Create: `backend/src/main/java/com/learningos/rag/repository/KbDocumentRepository.java`
- Create: `backend/src/main/java/com/learningos/rag/repository/KbDocChunkRepository.java`
- Create: `backend/src/main/java/com/learningos/rag/repository/KbIndexTaskRepository.java`
- Create: `backend/src/main/java/com/learningos/rag/repository/KbChatSessionRepository.java`
- Create: `backend/src/main/java/com/learningos/rag/repository/KbQueryLogRepository.java`
- Create: `backend/src/main/java/com/learningos/rag/storage/DocumentStorageService.java`
- Create: `backend/src/main/java/com/learningos/rag/storage/StoredObject.java`
- Create: `backend/src/main/java/com/learningos/rag/storage/MinioDocumentStorageService.java`
- Create: `backend/src/main/java/com/learningos/rag/storage/NoopDocumentStorageService.java`

Migration:

- Create: `backend/src/main/resources/db/migration/V1__rag_foundation.sql`

RAG tests:

- Create: `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java`
- Create: `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- Create: `backend/src/test/java/com/learningos/rag/api/ChatControllerTest.java`
- Create: `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java`
- Create: `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`

## Phase 2 Database Tables

Create `V1__rag_foundation.sql` with:

- `kb_knowledge_base`: `id`, `name`, `description`, `visibility`, `owner_user_id`, `created_by`, `created_at`, `updated_at`, `deleted_at`.
- `kb_permission`: `id`, `kb_id`, `subject_type`, `subject_id`, `permission`, `created_at`.
- `kb_document`: `id`, `kb_id`, `course_id`, `chapter_id`, `name`, `content_type`, `size_bytes`, `storage_bucket`, `storage_key`, `version`, `parse_status`, `index_status`, `error_message`, `created_by`, `created_at`, `updated_at`, `deleted_at`.
- `kb_doc_chunk`: `id`, `kb_id`, `document_id`, `document_version`, `chunk_index`, `content`, `content_tsv`, `embedding vector(1536)`, `page_num`, `section_title`, `metadata jsonb`, `created_at`.
- `kb_index_task`: `id`, `document_id`, `kb_id`, `status`, `retry_count`, `error_message`, `started_at`, `finished_at`, `created_at`, `updated_at`.
- `kb_chat_session`: `id`, `user_id`, `kb_scope jsonb`, `title`, `created_at`, `last_active_at`.
- `kb_chat_message`: `id`, `session_id`, `role`, `content`, `sources jsonb`, `token_count`, `latency_ms`, `created_at`.
- `kb_query_log`: `id`, `trace_id`, `user_id`, `kb_ids jsonb`, `question`, `rewritten_query`, `retrieval_count`, `reranker_status`, `sources jsonb`, `latency_ms`, `created_at`.

Required indexes:

- `idx_kb_permission_subject` on `(subject_type, subject_id)`.
- `idx_kb_document_kb_status` on `(kb_id, index_status)` where `deleted_at is null`.
- `idx_kb_doc_chunk_kb_doc` on `(kb_id, document_id)`.
- `idx_kb_doc_chunk_content_tsv` as a PostgreSQL GIN full-text index.
- `idx_kb_doc_chunk_embedding` as a PGVector index when extension is available.
- `idx_kb_index_task_status` on `(status, created_at)`.
- `idx_kb_query_log_trace_id` on `(trace_id)`.

## Phase 1 Tasks

### Task 1: Create Backend Build Skeleton

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/learningos/LearningOsApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `backend/src/main/resources/application-test.yml`
- Create: `backend/src/test/java/com/learningos/LearningOsApplicationTests.java`

- [x] Step 1: Create Maven project with Java 21, Spring Boot 3.x, Spring Web, Validation, Actuator, Data JPA, Flyway, PostgreSQL driver, Spring Data Redis, MinIO SDK, and Spring AI dependency management.
- [x] Step 2: Add `LearningOsApplication` under package `com.learningos`.
- [x] Step 3: Add `application.yml` with placeholders for datasource, Redis, MinIO, model provider, trace, and RAG limits.
- [x] Step 4: Add `application-test.yml` with external clients disabled or pointed at local test doubles.
- [x] Step 5: Add a context-load test.
- [ ] Step 6: Run:

```powershell
cd backend
.\mvnw.cmd test
```

Expected: build succeeds and the Spring application context loads.

### Task 2: Add Common API Envelope And Error Model

**Files:**
- Create: `backend/src/main/java/com/learningos/common/api/ApiResponse.java`
- Create: `backend/src/main/java/com/learningos/common/api/ErrorCode.java`
- Create: `backend/src/main/java/com/learningos/common/exception/ApiException.java`
- Create: `backend/src/main/java/com/learningos/common/exception/GlobalExceptionHandler.java`
- Create: `backend/src/test/java/com/learningos/common/exception/GlobalExceptionHandlerTest.java`

- [x] Step 1: Add `ApiResponse<T>` with `code`, `message`, and `data`.
- [x] Step 2: Add `ErrorCode` values: `OK`, `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `STORAGE_ERROR`, `DEPENDENCY_UNAVAILABLE`, `INTERNAL_ERROR`.
- [x] Step 3: Add `ApiException` carrying an `ErrorCode`.
- [x] Step 4: Add `GlobalExceptionHandler` for validation errors, `ApiException`, and unexpected exceptions.
- [x] Step 5: Write MockMvc test using a small test controller that throws `ApiException`.
- [ ] Step 6: Run:

```powershell
cd backend
.\mvnw.cmd "-Dtest=GlobalExceptionHandlerTest" test
```

Expected: error responses keep the common envelope and return the configured code.

### Task 3: Add TraceId Infrastructure

**Files:**
- Create: `backend/src/main/java/com/learningos/common/trace/TraceContext.java`
- Create: `backend/src/main/java/com/learningos/common/trace/TraceFilter.java`
- Create: `backend/src/test/java/com/learningos/common/trace/TraceFilterTest.java`

- [x] Step 1: Add `TraceContext` backed by `ThreadLocal<String>`.
- [x] Step 2: Add `TraceFilter` that reads `X-Trace-Id`, creates one when absent, sets response header `X-Trace-Id`, and clears context after request completion.
- [x] Step 3: Write tests for provided trace ID and generated trace ID.
- [ ] Step 4: Run:

```powershell
cd backend
.\mvnw.cmd "-Dtest=TraceFilterTest" test
```

Expected: response always contains `X-Trace-Id`.

### Task 4: Add Dev Auth And UserContext

**Files:**
- Create: `backend/src/main/java/com/learningos/common/auth/UserContext.java`
- Create: `backend/src/main/java/com/learningos/common/auth/UserContextHolder.java`
- Create: `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`
- Create: `backend/src/test/java/com/learningos/common/auth/DevAuthFilterTest.java`

- [x] Step 1: Add `UserContext` with `userId`, `displayName`, and `roles`.
- [x] Step 2: Add `UserContextHolder` using `ThreadLocal<UserContext>`.
- [x] Step 3: Add `DevAuthFilter` that reads `X-User-Id`, defaults to `dev_user`, and stores `UserContext`.
- [x] Step 4: Add a clear class-level comment that this filter is the phase-1 local substitute for Sa-Token/JWT.
- [x] Step 5: Write tests proving `X-User-Id` becomes current user and the context is cleared after request.
- [ ] Step 6: Run:

```powershell
cd backend
.\mvnw.cmd "-Dtest=DevAuthFilterTest" test
```

Expected: user context is available during request and cleared after completion.

### Task 5: Add Dependency Configuration And Docker Compose

**Files:**
- Create: `backend/docker-compose.yml`
- Create: `backend/.env.example`
- Create: `backend/src/main/java/com/learningos/config/AppProperties.java`
- Create: `backend/src/main/java/com/learningos/config/StorageProperties.java`
- Create: `backend/src/main/java/com/learningos/config/RagProperties.java`
- Create: `backend/src/main/java/com/learningos/config/AiModelProperties.java`
- Create: `backend/src/main/java/com/learningos/config/AsyncConfig.java`

- [x] Step 1: Add Docker Compose services `postgres`, `redis`, and `minio`.
- [x] Step 2: Use a PostgreSQL image that can support PGVector, or document the required PGVector extension image in Compose.
- [x] Step 3: Add Redis 7 service.
- [x] Step 4: Add MinIO service with console port and bucket-related environment placeholders.
- [x] Step 5: Bind configuration properties for application, storage, RAG, and AI model settings.
- [ ] Step 6: Run:

```powershell
cd backend
docker compose config
.\mvnw.cmd test
```

Expected: Compose file is syntactically valid and Spring binds configuration properties.

### Task 6: Add Health Endpoint

**Files:**
- Create: `backend/src/main/java/com/learningos/health/api/HealthController.java`
- Create: `backend/src/main/java/com/learningos/health/application/HealthService.java`
- Create: `backend/src/main/java/com/learningos/health/api/HealthDtos.java`
- Create: `backend/src/test/java/com/learningos/health/api/HealthControllerTest.java`

- [x] Step 1: Implement `GET /api/health`.
- [x] Step 2: Return `application`, `database`, `redis`, `minio`, and `model` status fields.
- [x] Step 3: Keep dependency checks shallow in Phase 1: report configured/unconfigured and avoid hard-failing startup when Redis/MinIO/model are not running.
- [x] Step 4: Write MockMvc test for response shape.
- [ ] Step 5: Run:

```powershell
cd backend
.\mvnw.cmd "-Dtest=HealthControllerTest" test
```

Expected: `/api/health` returns the documented health shape inside `ApiResponse`.

### Task 7: Add Placeholder Module Boundaries

**Files:**
- Create: `backend/src/main/java/com/learningos/user/package-info.java`
- Create: `backend/src/main/java/com/learningos/knowledge/package-info.java`
- Create: `backend/src/main/java/com/learningos/rag/package-info.java`
- Create: `backend/src/main/java/com/learningos/agent/package-info.java`
- Create: `backend/src/main/java/com/learningos/learning/package-info.java`
- Create: `backend/src/main/java/com/learningos/assessment/package-info.java`

- [x] Step 1: Add package documentation explaining each module boundary.
- [x] Step 2: State in `agent/package-info.java` that Agent tools call services and do not access repositories directly.
- [x] Step 3: State in `rag/package-info.java` that Phase 2 splits offline indexing from online query.
- [ ] Step 4: Run:

```powershell
cd backend
.\mvnw.cmd test
```

Expected: packages compile and no test regresses.

### Task 8: Add README Backend Instructions

**Files:**
- Modify: `README.md`

- [ ] Step 1: Add backend prerequisites: Java 21, Maven wrapper, Docker Desktop or Docker Compose.
- [x] Step 2: Add dependency startup command:

```powershell
cd backend
docker compose up -d
```

- [ ] Step 3: Add backend test command:

```powershell
cd backend
.\mvnw.cmd test
```

- [ ] Step 4: Add backend run command:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

- [x] Step 5: Add health verification command:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

- [ ] Step 6: Run frontend verification to ensure README-only change did not require frontend changes:

```powershell
cd frontend
npm test -- --run
npm run build
```

Expected: README gives full local startup path and frontend verification remains green.

### Task 9: Phase 1 Final Verification

**Files:**
- No new files.

- [ ] Step 1: Run:

```powershell
cd backend
.\mvnw.cmd test
```

Expected: all backend tests pass.

- [x] Step 2: Run:

```powershell
cd backend
docker compose config
```

Expected: Docker Compose config is valid.

- [ ] Step 3: Run:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Expected: backend starts on port `8080`.

- [ ] Step 4: In another terminal, run:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Expected: response contains `code = "OK"` and health fields for app, database, Redis, MinIO, and model config.

## Phase 2 Task Outline

Phase 2 should be split into its own implementation plan before coding. It must include:

- Knowledge base create/list permissions.
- Document upload through `DocumentStorageService`.
- MinIO implementation plus test/noop implementation.
- `kb_document`, `kb_index_task`, `kb_doc_chunk`, chat, and query log entities.
- Flyway RAG migration.
- `IndexService` async task status model.
- `ChunkService` with metadata containing `kbId`, `docId`, `pageNum`, `sectionTitle`, and `version`.
- `EmbeddingService` boundary with Redis cache key including model version.
- `RagQueryService` online path.
- Hybrid retrieval signatures that require `allowedKbIds`.
- RRF fusion.
- `RerankerService` timeout/failure fallback.
- Token budget trimming.
- Citations in both HTTP and SSE responses.
- Permission tests proving disallowed KBs are not retrieved.

## Implementation Rules

- Read relevant docs before each phase.
- Check current files with `rg --files`.
- Do not rewrite the Vue frontend unless the task is Phase 6 or an integration need makes it necessary.
- Controllers call application services only.
- Application services orchestrate workflows.
- Domain services hold core business rules.
- Agents can call tools only.
- Tools can call services only.
- Tools must not call repositories.
- Repositories handle data access only.
- Permissions must be enforced in backend services and repository/search methods, never by prompt.
- RAG answers must return citations.
- Learning paths, generated resources, assessment feedback, and Agent execution must leave trace records.
- Every phase must end with tests or concrete verification commands.

## Execution Handoff

Phase 0 appears complete from current repository evidence. Start implementation with Phase 1: Backend Infrastructure.

Recommended execution mode:

1. Subagent-Driven - dispatch a fresh worker per Phase 1 task and review after each task.
2. Inline Execution - execute Phase 1 tasks in this session with checkpoints.
