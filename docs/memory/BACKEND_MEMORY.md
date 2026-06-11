# BACKEND_MEMORY.md

## 2026-06-11 F1 Model Provider Registry Backend

- Implemented admin-side model provider registry backend slice.
- Added V21 `model_provider` table, `ModelProviderService`, and `/api/admin/model-providers` admin-only CRUD/set-default/test-connection APIs.
- API keys are AES-GCM encrypted via `ModelProviderSecretCodec`; list/detail responses only expose masked keys.
- `AiModelGateway` now resolves enabled default registry provider before env-based Spring AI bean routing.
- Supported provider codes: `deepseek`, `mimo`, `dashscope`, `openai`, `custom`.
- Verification passed: full backend `612 run, 0 failures, 0 errors, 1 skipped`.
- Follow-up: Admin frontend configuration page, embedding registry integration, external provider smoke.

## 2026-06-11 Backend Architecture TODO Plan Completion

- Closed `docs/planning/backend-architecture-todolist.md` for MVP / 最小可答辩闭环 scope.
- P0, P1, P2, P3-1, P3-2 minimum productionization, P3-3 minimum provider boundary, P3-4 MVP permission matrix, and P3-5 observability main items are all marked complete.
- P3-4 residual permission checklist items were closed at MVP boundary after formal OAuth2, roles-first RBAC, KB-course binding, production streaming, and high-value penetration matrix child tasks.
- Industrial parser/OCR, Qdrant real-service smoke, DashScope dedicated provider, and ongoing permission-matrix sampling were moved to a follow-up section and do not block plan completion.
- Verification passed: full backend `601 run, 0 failures, 0 errors, 1 skipped`.
- Evidence: `docs/evidence/EVIDENCE-20260611-backend-architecture-todolist-completion.md`

## 2026-06-11 P3-4 子任务 Formal Production Streaming Design

- Implemented and verified the P3-4 formal production streaming design slice.
- Added `POST /api/rag/query/stream` in `ChatController`, producing `text/event-stream`.
- The endpoint reuses `RagQueryService.query(...)` or `queryWithRequestId(...)` based on optional `requestId` and passes explicit admin/teacher facts from `UserContext.roles()`.
- Stream events are `status`, `token`, `done`, and safe `error`; `done` includes `answer`, `sources`, `retrieval`, `traceId`, and `latencyMs`.
- Extended production/staging security regression coverage for the POST stream endpoint: missing Bearer and invalid Bearer return `UNAUTHORIZED` before async/RAG work, valid Bearer ignores spoofed `X-User-Id`, and `USER sub=admin` does not gain admin semantics.
- No DB schema, dependency, auth framework, RAG service, Tutor controller, model gateway, VectorDB, parser, or OCR changes were added.
- Verification passed: adjacent backend `36 run, 0 failures`; full backend `601 run, 0 failures, 0 errors, 1 skipped`.
- Accepted limitations: transport streaming only; legacy GET Chat/Tutor streams still exist for dev/test/demo compatibility; standalone citation event and unified GET stream safe error contract remain possible follow-up hardening.
- P3-4 MVP permission scope is now closed at plan level; broader teacher-side data-scope expansion and ongoing business-matrix sampling remain follow-up maintenance.

## 2026-06-11 P3-4 子任务 Orchestrator Answer Submission Replay Scope Revalidation

- Implemented and verified the P3-4 Orchestrator `ANSWER_SUBMISSION` replay scope revalidation slice.
- RED proved a same-payload Orchestrator replay returned the old workflow with HTTP 200 after the learner's course enrollment changed from `ACTIVE` to `DROPPED`.
- `AssessmentService.replayAnswerIfPresent(...)` now calls `requireSubmitQuestionScope(request.learnerId(), request.questionId())` before request-hash comparison and answer snapshot replay.
- The replay path now matches direct `submitAnswerWithTraceId(...)` scope semantics: resolvable `questionId -> KnowledgePoint.courseId -> CourseAccessService.requireCourseRead(learnerId, false, false, courseId)`.
- Added `OrchestratorWorkflowControllerTest.answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow`.
- Forbidden replay returns safe `FORBIDDEN` without `data`, does not return the old workflow/task/trace metadata, and does not add answer/grading/mastery/wrong-question/learning-event rows.
- No REST API path/DTO, DB schema, dependency, frontend, or auth framework change was added.
- Verification passed: RED `1 run, 1 failure`; focused `1 run, 0 failures`; adjacent `93 run, 0 failures`; full backend `596 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 MVP permission scope is now closed at plan level; broader teacher-side data-scope expansion and ongoing business-matrix sampling remain follow-up maintenance.

## 2026-06-11 P3-4 子任务 Teacher Permission Residual Sampling Matrix

- Implemented and verified the P3-4 teacher permission residual sampling matrix slice through tests only.
- Added `KnowledgeBaseControllerTest` regression proving Bearer `TEACHER` with spoofed `X-User-Id: admin` sees only token-subject readable course-bound KBs; foreign course-bound KBs are redacted even when visibility is `PUBLIC`.
- Added `DocumentControllerTest` regression proving Bearer `TEACHER` cannot reindex a foreign course-bound document and the safe `FORBIDDEN` response does not leak document/index task/course metadata.
- The foreign document reindex denial path leaves index-task counts unchanged.
- Added `AnalyticsControllerTest` regression proving teacher class summary `pendingReviews` contains only requested-course reviews and does not leak foreign review/resource/task/course/title or `markdownContent`.
- No production code, REST API path/DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
- Verification passed: focused `71 run, 0 failures`; adjacent `172 run, 0 failures`; full backend `595 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open; Orchestrator `ANSWER_SUBMISSION` replay scope revalidation is tracked as a separate completed semantic child task.

## 2026-06-10 P3-2 子任务：Real VectorDB Adapter Minimum Integration

- Implemented and verified the P3-2 real VectorDB adapter minimum integration slice.
- Added a Qdrant-backed `VectorIndexAdapter` behind the project-owned adapter port and `learning-os.rag.vector.*` configuration.
- Default runtime remains disabled/noop and does not connect to Qdrant unless `enabled=true` and `provider=qdrant` are explicitly configured.
- `NoopVectorIndexAdapter` is now a plain implementation; `QdrantVectorConfiguration` provides the `@ConditionalOnMissingBean(VectorIndexAdapter.class)` fallback bean.
- Qdrant upsert payload contains only low-sensitivity chunk metadata; raw chunk content, question, prompt, storage key, user id, secret, and raw vector values are not exposed through payload/string output.
- Qdrant search requests only `chunkId` payload and disables vector return so the service can reload chunks and re-apply allowed-KB filtering.
- Verification passed: focused `6 run, 0 failures`; adjacent `28 run, 0 failures`; full backend `592 run, 0 failures, 0 errors, 1 skipped`; dependency tree recorded `spring-ai-qdrant-store:1.0.8 -> io.qdrant:client:1.13.0 -> grpc-netty-shaded:1.65.1`.
- P3-2 parent remains open for industrial PDF layout/table/TOC, native/cloud OCR, provider confidence pipeline, true rendered page numbers, plus Qdrant real service smoke, collection dimension validation, health/ops integration, and gRPC/Netty risk handling.

## 2026-06-10 P3-2 子任务：Vector Embedding Payload Contract

- Implemented and verified the P3-2 vector embedding payload contract slice.
- Added in-memory `EmbeddingVector` and `QueryEmbeddingResult`.
- `EmbeddingBatchResult` now carries chunk-id-aligned vectors when document embedding succeeds.
- `EmbeddingService.embedQuery(...)` creates query vectors before vector search; adapters no longer receive raw questions.
- `VectorChunkReference` can carry a vector payload and `VectorSearchRequest` carries a query vector; string representations do not expose raw float values.
- `IndexService` passes document embedding vectors into `VectorUpsertRequest`; `ChunkService` falls back to keyword/recency/RRF if query embedding fails.
- No dependency, VectorDB provider/config, REST API/DTO, DB migration, frontend, parser/OCR, or Agent/Orchestrator change was added.
- Verification passed: RED compile failure; focused `13 run, 0 failures`; adjacent `49 run, 0 failures`; full backend `586 run, 0 failures, 0 errors, 1 skipped`.
- This precondition was consumed by the later P3-2 real VectorDB adapter minimum integration.

## 2026-06-10 P3-2 子任务：DOCX Table/TOC Reading-Order Provider

- Implemented and verified the P3-2 DOCX table/TOC reading-order provider slice.
- `PoiDocxDocumentFormatParser` now walks `XWPFDocument#getBodyElements()` so paragraphs and tables keep DOCX body order.
- `DocumentParserService` lightweight DOCX fallback now parses `word/document.xml` body elements in `<w:p>` / `<w:tbl>` order.
- DOCX tables are emitted as independent parsed sections and indexed chunks with `contentKind=TABLE_TEXT`.
- TOC-like DOCX paragraphs are skipped by default to reduce RAG retrieval pollution.
- No dependency, API/DTO, DB migration, frontend, VectorDB, retrieval, or citation contract changes were added.
- Verification passed: RED `46 run, 3 failures`; focused `46 run, 0 failures`; adjacent `58 run, 0 failures`; full backend `582 run, 0 failures, 0 errors, 1 skipped`.
- P3-2 parent remains open for PDF layout/table/TOC provider, native/cloud OCR, provider confidence pipeline, true rendered page numbers, plus Qdrant real service smoke/ops validation.

## 2026-06-10 P3-4 子任务 Frontend SSE Sensitive URL Cleanup

- Implemented and verified the frontend SSE sensitive URL cleanup minimum slice.
- Production/staging student RAG chat now uses existing `POST /api/rag/query` instead of native `EventSource` GET URLs that carry `question` / `kbIds`.
- Production/staging REST failures surface directly and do not retry the same POST through the legacy SSE fallback path.
- Dev/test SSE streaming, SSE failure fallback, and malformed SSE event error behavior remain covered and passing.
- No backend code, REST API path/DTO, DB schema, dependency, auth protocol, query token, signed stream token, cookie/session, or LLM direct frontend call was added.
- Verification passed: RED `1 failed | 28 passed`; duplicate-POST RED `1 failed | 30 passed`; focused frontend `31 passed`; `npx vue-tsc -b --noEmit` passed.
- Accepted limitation at completion time: production/staging was a non-streaming POST fallback; this was later superseded by `docs/specs/SPEC-20260611-p3-4-formal-production-streaming-design.md`.

## 2026-06-10 P3-4 子任务 Assessment Submit Foreign QuestionId

- Implemented and verified the Assessment submit foreign-questionId security fix.
- RED proved Bearer `USER sub=alice` with spoofed headers could submit another course's `questionId` and trigger answer/grading/mastery/wrong-question/learning-event side effects.
- `AssessmentService.submitAnswerWithTraceId(...)` now calls `requireSubmitQuestionScope(...)` after requestId validation and before idempotency replay, content-safety, transaction start, or durable side effects.
- `requireSubmitQuestionScope(...)` enforces `questionId -> KnowledgePoint.courseId -> CourseAccessService.requireCourseRead(learnerId, false, false, courseId)` for resolvable course-bound questions.
- Legacy/template question ids such as `q_sql_join` remain compatible when no existing `KnowledgePoint` can be resolved.
- Verification passed: RED `49 run, 1 failure`; focused `60 run, 0 failures`; adjacent `92 run, 0 failures`; full backend `578 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for frontend production streaming client / sensitive SSE URL cleanup and broader follow-up.

## 2026-06-10 P3-4 子任务 Evaluation/Review Forged-ID Object-Oracle Matrix

- Implemented and verified the Evaluation/Review forged-id object-oracle matrix through tests only.
- Added `EvaluationRunControllerTest` regression proving Bearer `TEACHER` with spoofed `X-User-Id: admin` cannot create evaluation runs for foreign or missing evaluation sets.
- Foreign/missing evaluation set denial returns safe `FORBIDDEN` without `data`, forged id, prompt version, trace, run, or metric side effects.
- Added `ResourceReviewControllerTest` regression proving Bearer `TEACHER` with spoofed admin header cannot distinguish missing review ids from foreign review ids.
- Review denials return safe `FORBIDDEN` without review/task/resource/request summary leakage and without mutating review/resource/task state.
- No production code, REST API path/DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
- Verification passed: focused `26 run, 0 failures`; adjacent `41 run, 0 failures`; full backend `578 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for frontend production streaming client / sensitive SSE URL cleanup and broader follow-up.

## 2026-06-10 P3-4 子任务 Dev/Test Legacy Fallback Cleanup

- Implemented and verified dev/test legacy fallback cleanup.
- `CurrentUserService` now allows subject-name legacy inference only when there is no Spring Security JWT context.
- Dev/test no-Bearer `X-User-Id` header-only fallback remains compatible.
- Dev/test Spring Security JWT identities with `roles=["USER"]` no longer gain admin/teacher semantics from `sub=admin` or `sub=teacher_*`.
- Added `SecurityJwtAuthenticationTest` regressions for JWT subject-name role-confusion.
- Verification passed: RED `6 run, 1 failure`; focused `6 run, 0 failures`; adjacent auth `38 run, 0 failures`; full backend `578 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for frontend production streaming client / sensitive SSE URL cleanup and broader follow-up.

## 2026-06-10 P3-4 子任务 Course/Knowledge Forged Business-Object HTTP Matrix

- Implemented and verified the Course/Knowledge forged business-object HTTP matrix slice through tests only.
- Added `CourseKnowledgeControllerTest` regression proving Bearer `TEACHER` with spoofed `X-User-Id: admin` cannot combine an owned `courseId` with a foreign `chapterId` to create a knowledge point.
- Added `CourseKnowledgeControllerTest` regression proving Bearer `ADMIN` remains globally authorized but cannot bypass chapter-course consistency when creating knowledge points.
- Added `CourseKnowledgeControllerTest` regression proving Bearer `TEACHER` with spoofed `X-User-Id: admin` cannot create a cross-course knowledge dependency using a foreign-course prerequisite.
- Forbidden/invalid forged-object responses return safe `VALIDATION_ERROR` without `data`, forged ids, course/chapter/knowledge titles, or request titles.
- Rejected requests leave no `KnowledgePoint` or `KnowledgeDependency` side effects; dependency graph remains empty after the rejected cross-course edge.
- No production code, REST API path/DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
- Verification passed: focused `29 run, 0 failures`; adjacent `35 run, 0 failures`; full backend `574 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for frontend production streaming client / sensitive SSE URL cleanup and broader follow-up.

## 2026-06-10 P3-4 子任务 PromptVersion Forged-ID Object-Oracle Guards

- Implemented and verified the PromptVersion forged-id / object-oracle permission matrix slice through tests only.
- Added `PromptVersionControllerTest` regressions proving Bearer `ADMIN` missing detail ignores spoofed `X-User-Id` and returns authorized `NOT_FOUND` without `data`.
- Added `PromptVersionControllerTest` regressions proving Bearer `TEACHER` remains an authorized metadata reader for missing detail and returns `NOT_FOUND` without `promptText` or `data`.
- Added `PromptVersionControllerTest` regressions proving Bearer `STUDENT` missing detail returns `FORBIDDEN` without leaking forged code/version or not-found oracle text.
- Added `PromptVersionControllerTest` regressions proving Bearer `USER sub=admin` cannot read PromptVersion list/detail management data.
- No production code, REST API path/DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
- Verification passed: focused `13 run, 0 failures`; adjacent `20 run, 0 failures`; full backend `572 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for frontend production streaming client / sensitive SSE URL cleanup and broader follow-up.

## 2026-06-10 P3-4 子任务 Orchestrator Workflow Forged-ID Object-Oracle Guards

- Implemented and verified the Orchestrator workflow forged-id / object-oracle permission matrix slice through tests only.
- Added `OrchestratorWorkflowControllerTest` regressions proving Bearer non-owner cannot query another user's workflow status even when spoofing `X-User-Id` as the owner.
- Added `OrchestratorWorkflowControllerTest` regressions proving Bearer non-owner cannot retry another user's failed `RESOURCE_GENERATION` workflow even when spoofing `X-User-Id` as the owner.
- Forbidden status/retry responses return safe `NOT_FOUND` without leaking workflowId, agentTaskId, traceId, or requestId.
- Forbidden retry leaves no new AgentTask, AgentTrace, ResourceGenerationTask, LearningResource, ResourceReview, ModelCallLog, TokenUsageLog, or SourceCitation side effects.
- No production code, REST API path/DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
- Verification passed: focused `32 run, 0 failures`; adjacent `73 run, 0 failures`; full backend `572 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for frontend production streaming client / sensitive SSE URL cleanup and broader follow-up.

## 2026-06-10 P3-4 子任务 Course/Class/Resource Broader Business Permission Matrix

- Implemented and verified the broader business course/class/resource permission matrix slice through tests only.
- Added `CourseKnowledgeControllerTest` regressions proving Bearer `TEACHER` course list ignores spoofed `X-User-Id: admin` and returns only token-subject owned courses.
- Added `CourseKnowledgeControllerTest` regressions proving Bearer `STUDENT` course list ignores spoofed `X-User-Id: teacher_*`, returns only ACTIVE enrollment courses, and redacts DROPPED enrollment courses.
- Added `ResourceGenerationControllerTest` regressions proving Bearer owner cannot create course-bound resource generation tasks with DROPPED or missing enrollment, and forbidden requests leave no ResourceGenerationTask, LearningResource, ResourceReview, AgentTask, AgentTrace, ModelCallLog, TokenUsageLog, or SourceCitation side effects.
- Added `AnalyticsControllerTest` coverage proving Bearer teacher cannot read a DROPPED learner student summary in own course and the forbidden response does not leak course, knowledge point, path, wrong-cause, or resource-task signals.
- No production code, REST API path/DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
- Verification passed: focused `117 run, 0 failures`; adjacent `143 run, 0 failures`; full backend `566 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for dev/test legacy fallback cleanup, frontend production streaming client / sensitive SSE URL cleanup, and broader forged-id / business-object penetration matrix.

## 2026-06-10 P3-4 子任务 Resource/Course Permission Matrix Expansion

- Implemented and verified the resource/course permission matrix expansion slice through tests only.
- Added `ResourceGenerationControllerTest` regressions for learner-only released resource reads, Bearer owner precedence over spoofed `X-User-Id`, Bearer admin foreign learner denial, student foreign/missing anti-enumeration, and course-bound resource generation create owner/teacher behavior.
- Added `CourseKnowledgeControllerTest` coverage proving Bearer `USER sub=teacher_*` does not receive teacher semantics on dependency writes.
- Added `AnalyticsControllerTest` coverage proving Bearer student requests with spoofed admin headers remain owner-only and active-enrollment scoped.
- Added `ResourceReviewControllerTest` coverage proving Bearer teacher no-prefix review list redacts foreign-course reviews.
- No production code, REST API path/DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
- Verification passed: focused `106 run, 0 failures`; adjacent `139 run, 0 failures`; full backend `555 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for `agent task cancel` / course create residual matrix, broader business course/class/resource matrix, dev/test legacy fallback cleanup, and a later frontend production streaming client / sensitive SSE URL cleanup task.

## 2026-06-10 P3-4 子任务 Answer Record RBAC Penetration Matrix Expansion

- Implemented and verified the answer record RBAC penetration matrix expansion slice through tests only.
- Added `AssessmentControllerTest` regressions for Bearer admin/student/teacher role facts, spoofed `X-User-Id`, subject-name role-confusion denial, wrong-question list Bearer teacher no-prefix success, and teacher detail active-enrollment enforcement.
- Bearer `USER sub=admin` and `USER sub=teacher_*` are pinned as non-elevating for answer/wrong-question list/detail paths.
- Bearer student requests with spoofed admin headers are pinned to owner-only answer/wrong-question list/detail behavior.
- Teacher detail reads are pinned to require both own-course scope and active enrollment; dropped enrollment returns safe `FORBIDDEN` without `data`.
- No production code, REST API path/DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
- Verification passed: focused `48 run, 0 failures`; adjacent `65 run, 0 failures`; full backend `547 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for broader course/class/resource matrix, dev/test legacy fallback cleanup, and a later frontend production streaming client / sensitive SSE URL cleanup task.

## 2026-06-10 P3-4 子任务 Service Legacy Subject Auth Cleanup

- Implemented and verified the service legacy subject-name authorization cleanup slice.
- `KnowledgeCatalogService` no longer exposes legacy public overloads that infer admin/teacher from `currentUserId`; course create/read/list/chapter/knowledge-point/dependency/graph service authorization now requires explicit role facts.
- `KnowledgeCatalogService` no longer keeps target subject-name helper paths including `isAdmin(String)`, `isTeacherUser(String)`, `scopedCourseMissing(String)`, and one-argument role helper overloads.
- `AssessmentService` no longer exposes legacy answer/wrong-question list/detail overloads that infer admin/teacher from `currentUserId`.
- `AssessmentService` no longer keeps `isAdmin(String)` or `isTeacherUser(String)`.
- `GradingEvaluationService.evaluate(String, GradingEvaluationRequest)` was removed; pure algorithm entries `evaluate(GradingEvaluationRequest)` and `evaluate(List<Double>, List<Double>, double)` remain covered.
- Added reflection guards in `CourseAccessServiceTest`, `AssessmentServiceTest`, and `GradingEvaluationServiceTest`.
- No REST API path/DTO, DB schema, dependency, frontend, Spring Security, RAG runtime, Agent runtime, or Review Gate change was added.
- Verification passed: compile guard success; focused `22 run, 0 failures`; adjacent `197 run, 0 failures`; full backend `536 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for broader class/course matrix, answer-record expansion, dev/test legacy fallback cleanup, and a later frontend production streaming client / sensitive SSE URL cleanup task.

## 2026-06-10 P3-4 子任务 SSE Production Auth Strategy

- Implemented and verified the backend SSE production auth strategy slice through regression coverage; production code was already fail-closed and was not changed.
- `SseProductionAuthStrategyTest` now pins Chat/Tutor SSE behavior in `production` and `staging`.
- Chat/Tutor SSE no Bearer and invalid Bearer return `UNAUTHORIZED` before async work starts and before `RagQueryService` is called.
- staging Chat/Tutor SSE rejects header-only `X-User-Id` auth before async work starts.
- valid Bearer Chat/Tutor SSE uses JWT subject and `UserContext.roles()` facts while ignoring spoofed `X-User-Id`.
- Bearer `USER sub=admin` does not receive admin facts in Chat/Tutor SSE.
- No production code, API path/DTO, DB schema, dependency, frontend, query token, signed stream token, or cookie/session change was added.
- Verification passed: focused `10 run, 0 failures`; adjacent auth/SSE/RAG runtime `38 run, 0 failures`; full backend `530 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for broader class/course matrix, answer-record expansion, dev/test legacy fallback cleanup, and a later frontend production streaming client / sensitive SSE URL cleanup task.

## 2026-06-10 P3-4 子任务 KB-course Binding Governance

- Implemented and verified the RAG KB-course binding schema/lifecycle governance slice.
- `kb_knowledge_base` now has `course_id`, `binding_status`, `bound_by`, and `bound_at`; V20 backfills active KBs to `UNBOUND`, `BOUND`, or `CONFLICTED`.
- `KnowledgeBaseService` creates course-bound KBs only after `CourseAccessService.requireCourseRead(...)` and `requireCourseManage(...)`.
- `PermissionService` routes `BOUND` KB read/write through `CourseAccessService`; `PUBLIC`, owner, explicit KB permission, and admin early-return paths cannot bypass course access.
- `CONFLICTED` KB remains non-admin denied; admin keeps KB-level governance read/write semantics, but `DocumentService` rejects document upload for conflicted KBs.
- Empty `UNBOUND` KB first legal course-document upload auto-binds the KB as `BOUND`; the transition is protected by `EntityManager.refresh(..., PESSIMISTIC_WRITE)` and a lock-after replay check.
- Same `createdBy + requestId` with different document payload returns `409 CONFLICT` before KB-course mismatch validation; same-payload replay may omit `courseId` on already bound KBs.
- Added `PermissionServiceTest.adminCourseBoundKnowledgeBaseStillRequiresCourseAccessPath` to pin the strict BOUND-through-CourseAccess contract.
- Verification passed: PermissionService focused `9 run, 0 failures`; DocumentController focused `25 run, 0 failures`; adjacent KB-course matrix `76 run, 0 failures`; full backend `520 run, 0 failures, 0 errors, 1 skipped`.
- MySQL smoke was attempted but blocked by local environment: Docker daemon unavailable and local MySQL root credential rejected.
- P3-4 parent remains open for broader class/course matrix, answer-record expansion, dev/test legacy fallback cleanup, and a later frontend production streaming client / sensitive SSE URL cleanup task.

## 2026-06-10 P3-4 子任务 RAG Query Runtime Roles-First RBAC

- Implemented and verified the RAG query runtime roles-first RBAC slice.
- `/api/rag/query` POST/GET, Chat SSE, Tutor ask/stream, and Orchestrator `RAG_QA` now pass explicit admin/teacher facts derived from `UserContext.roles()` into `RagQueryService`.
- `RagQueryService` now exposes role-aware overloads for normal query, traceId query, requestId query, traceId+requestId query, and replay precheck.
- Legacy `RagQueryService` overloads remain for compatibility but default to `currentUserAdmin=false` and `currentUserTeacher=false`, so literal `userId = "admin"` does not grant admin KB read semantics.
- Orchestrator `RAG_QA` replay precheck and execution both use the same role facts; forbidden subject-name role-confusion leaves safe failed workflow evidence and no `kb_query_log` / `source_citation` artifacts.
- Added Orchestrator RAG_QA regressions for Bearer `ADMIN` with spoofed `X-User-Id` and Bearer `USER sub=admin` role-confusion denial.
- No REST API path, DTO, DB schema, dependency, frontend, or secret files were changed.
- Verification passed: Orchestrator focused `30 run, 0 failures`; runtime RBAC focused `60 run, 0 failures`; adjacent permission/API `161 run, 0 failures`; full backend `509 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for broader class/course matrix follow-up, answer-record expansion, dev/test legacy fallback cleanup, and a later frontend production streaming client / sensitive SSE URL cleanup task.

## 2026-06-10 P3-2 Parser/OCR TODO Status Reconciliation

- Reconciled `docs/planning/backend-architecture-todolist.md` with accepted P3-2-E/F/G/H/I evidence.
- Marked the complex PDF/DOCX, OCR fallback, real page number, and section hierarchy TODO as completed for the minimum productionized capability: PDFBox/POI parser providers, configurable/process OCR fallback, and best-effort `pageNum` / `headingPath` propagation into chunks.
- Added a separate open follow-up for industrial PDF/DOCX layout/table/TOC/reading-order, native/cloud OCR, OCR confidence, and true rendered page numbers.
- No backend code, schema, dependency, API, frontend, or secret files were changed by this reconciliation task.
- Remaining RAG production follow-up work: industrial document layout/OCR enhancements, Qdrant real service smoke, collection dimension validation, health/ops integration, and gRPC/Netty risk handling.

## 2026-06-10 P3-3 Model Provider TODO Status Reconciliation

- Reconciled `docs/planning/backend-architecture-todolist.md` with accepted P3-3-B and P3-3-C evidence.
- Marked the Spring AI Chat/Embedding model integration TODO as completed for the minimum Spring AI OpenAI-compatible adapter: `AiModelGateway` uses `ChatModel`, `EmbeddingService` uses `EmbeddingModel`, and default `AI_MODEL_PROVIDER=none` remains no-external-call.
- Marked model provider/model/promptVersion/latency/token/error logging as completed based on P3-3-A gateway evidence plus P3-3-B `model_call_log.provider` persistence.
- No backend code, schema, dependency, API, frontend, or secret files were changed by this reconciliation task.
- Remaining model/RAG follow-up work: DashScope / Spring AI Alibaba dedicated provider enhancement, controlled external provider smoke with secrets, and Qdrant real service smoke/ops validation.

## 2026-06-09 P3-4 子任务 Formal OAuth2/JWK/Spring Security

- Implemented and verified the formal OAuth2/JWK/Spring Security minimum authentication boundary slice.
- Added Spring Security Resource Server dependencies and a `SecurityFilterChain` under `common/auth`.
- `JwtDecoder` now prefers `learning-os.auth.jwk-set-uri`, otherwise uses a Spring Security HS256 compatibility path with a minimum 32-byte secret.
- Production-like environments (`prod` / `production` / `staging`) fail fast when neither JWK Set URI nor HS256 secret is configured; they no longer fall back to a source-code default secret or header identity.
- Production-like authorization now requires `JwtAuthenticationToken`, so anonymous authentication cannot satisfy the protected API boundary.
- `CurrentUserService` now prefers Spring Security `JwtAuthenticationToken` and maps JWT `sub`, `name`, and whitelisted `roles` into `UserContext`; subject-name role inference is not used for JWT identities.
- Production-like no-context fallback now returns `unauthenticated` instead of `dev_user`, avoiding misleading audit/log user IDs on 401 paths.
- `DevAuthFilter` no longer hand-verifies Bearer tokens and only keeps dev/test no-Bearer `X-User-Id` fallback.
- Added sanitized 401/403 JSON envelope handlers for Spring Security failures.
- RED evidence covered `AuthProperties` constructor binding failure, anonymous authentication production bypass, HS256 secret length enforcement, and MVC slice tests intercepted by default security auto-configuration.
- Verification passed: focused `27 run, 0 failures`; adjacent `106 run, 0 failures`; full backend `500 run, 0 failures, 0 errors, 1 skipped`.
- Remaining P3-4 follow-up work: broader class/course authorization matrix expansion, third-party IdP discovery compatibility, frontend production streaming client / sensitive SSE URL cleanup, and full cleanup of dev/test legacy subject fallback.

## 2026-06-09 P3-4 子任务 Broader Class/Course Permission Penetration Tests

- Implemented and verified the broader class/course permission penetration test slice for the current S Fast Lane scope.
- Added CourseKnowledge controller regressions proving Bearer `USER sub=teacher_1` cannot create a course for self and cannot create knowledge points in a course whose `teacherId` equals the token subject.
- Added Analytics controller regressions proving Bearer `USER sub=admin` / `USER sub=teacher_1` do not gain admin/teacher semantics from subject names.
- Added Bearer `TEACHER` class summary regression proving spoofed `X-User-Id` does not replace token subject and class analytics counts only active enrolled learners.
- Added dropped and never-enrolled learner legacy-signal regressions so learning paths, wrong questions, and resource tasks do not imply class membership.
- RED exposed an analytics HTTP authorization gap: `AnalyticsController` was still using `CurrentUserService.isAdmin()` / `isTeacherUser()` subject-name fallback. The controller now derives role facts from `UserContext.roles()`.
- No REST API path, request DTO, response DTO, schema, dependency, frontend, or formal OAuth2/JWK/Spring Security change was added.
- Verification passed: focused `59 run, 0 failures`; adjacent `82 run, 0 failures`; full backend `487 run, 0 failures, 0 errors, 1 skipped`.
- Remaining P3-4 follow-up work: broader class/course authorization matrix expansion and formal OAuth2/JWK/Spring Security.

## 2026-06-09 P3-4 子任务 Analytics TeacherClassSummary Legacy Subject-Name Cleanup

- Implemented and verified the Analytics teacher class summary legacy subject-name cleanup slice.
- Removed `AnalyticsService.teacherClassSummary(String courseId, String currentUserId)`.
- Removed `AnalyticsService.isLegacyTeacherUser(String)`, which previously inferred teacher semantics from `teacher` / `teacher_*` subjects.
- `AnalyticsService.requireTeacherClassAccess(...)` now depends only on explicit role facts: admin fact, or teacher fact plus `currentUserId == Course.teacherId`.
- `AnalyticsService.classLearnerIds(...)` now fails closed with `Set.of()` when `CourseAccessService` is absent; it no longer infers class membership from `LearningPath.goalId`.
- Added `AnalyticsServiceTest` reflection guards and behavior regressions covering legacy overload/helper removal, subject-name role-confusion denial, and fail-closed class learner fallback.
- No REST API path, request DTO, response DTO, schema, dependency, frontend, `AnalyticsController`, `CourseAccessService`, or formal OAuth2/JWK/Spring Security change was added.
- Verification passed: RED observed `4 run, 4 failures` plus membership RED `1 run, 1 failure`; focused `5/5`; compile guard success; adjacent `73/73`; full backend `482 run, 0 failures, 0 errors, 1 skipped`.
- Remaining P3 follow-up work: broader class/course authorization, formal OAuth2/JWK/Spring Security, and broader permission penetration tests.

## 2026-06-09 P3-4 子任务 LearningPath Create Legacy Overload Cleanup

- Implemented and verified the LearningPath create legacy overload cleanup slice.
- Removed `LearningWorkflowService.createPathForUser(String currentUserId, CreateLearningPathRequest request)`.
- Removed `LearningWorkflowService.isAdmin(String userId)`, which previously inferred admin semantics from `currentUserId = "admin"`.
- `LearningWorkflowService` create-path authorization surface now requires explicit roles-first facts through `createPathForUser(currentUserId, currentUserAdmin, currentUserTeacher, request)`.
- Added reflection guards so the legacy two-argument create overload and subject-name admin helper are not reintroduced.
- Added a service-level behavior regression test proving explicit admin facts still allow cross-learner path creation.
- No REST API path, request DTO, response DTO, schema, dependency, frontend, `LearningPathController`, `CourseAccessService`, ResourceGeneration, Orchestrator, Agent Trace, Review Gate, or formal OAuth2/JWK/Spring Security change was added.
- Verification passed: RED observed `2 run, 2 failures`; focused `3/3`; compile guard success; adjacent `25/25`; full backend `477 run, 0 failures, 0 errors, 1 skipped`.
- Remaining P3 follow-up work: broader class/course authorization, formal OAuth2/JWK/Spring Security, and broader permission penetration tests.

## 2026-06-09 P3-4-X LearningPath Detail Roles-First RBAC

- Implemented and verified the LearningPath detail roles-first RBAC slice for `GET /api/learning-paths/{pathId}`.
- `LearningPathController.get(...)` now reads `CurrentUserService.currentUser()` and derives explicit admin facts only from `UserContext.roles()`.
- `LearningWorkflowService` now exposes `getPathForUser(currentUserId, currentUserAdmin, pathId)` for roles-first detail reads.
- Legacy `getPathForUser(currentUserId, pathId)` remains as a compatibility entrypoint but delegates with `currentUserAdmin = false`, so subject-name `currentUserId = "admin"` no longer grants admin detail semantics through that signature.
- Bearer `ADMIN sub=ops_admin` can read existing foreign learning paths despite spoofed `X-User-Id`; admin missing path keeps `NOT_FOUND`.
- Bearer `USER sub=admin` no longer gains admin semantics; foreign/missing paths return safe `FORBIDDEN` without leaking target ids.
- Owner reads and non-owner missing/foreign anti-enumeration remain covered by HTTP regression tests.
- No dependency, schema, REST API path, request DTO, response DTO, frontend, formal OAuth2/JWK/Spring Security, Agent/RAG/model runtime, ResourceGeneration, Agent Trace, Review Gate, or create-path semantics change was added.
- Verification passed: RED observed `7 run, 4 failures`; focused `7/7`; controller `20/20`; adjacent `52/52`; full backend `474 run, 0 failures, 0 errors, 1 skipped`.
- Remaining P3 follow-up work: broader class/course authorization, formal OAuth2/JWK/Spring Security, broader permission penetration tests, and separate cleanup of `LearningWorkflowService.createPathForUser(String currentUserId, CreateLearningPathRequest request)` legacy subject-name helper.

## 2026-06-09 P3-4-W CourseAccessService Legacy Overload Cleanup

- Implemented and verified the `CourseAccessService` legacy overload cleanup slice.
- Removed legacy public overloads that inferred admin/teacher semantics from subject names:
  - `requireCourseRead(String currentUserId, String courseId)`
  - `requireCourseManage(String currentUserId, Course course)`
  - `requireLearnerEnrolledForExistingCourse(String currentUserId, String learnerId, String courseId)`
  - `listCoursesForUser(String currentUserId)`
- Removed legacy helper paths `scopedCourseMissing(String)`, `isAdmin(String)`, and `isTeacherUser(String)` from `CourseAccessService`.
- Public `CourseAccessService` course authorization entrypoints now require explicit roles-first facts for read/manage/enrollment/list behavior.
- Added `CourseAccessServiceTest` reflection guards and role-confusion behavior tests so `currentUserId = "admin"` or `"teacher_1"` does not grant permissions unless explicit role facts are true.
- No dependency, schema, REST API path, request DTO, response DTO, frontend, formal OAuth2/JWK/Spring Security, Agent/RAG/model runtime, or broader class/course change was added.
- Verification passed: RED observed `4 run, 2 failures`; focused `4/4`; compile guard success; adjacent `183/183`; full backend `467 run, 0 failures, 0 errors, 1 skipped`.
- Remaining P3 follow-up work: broader class/course authorization, formal OAuth2/JWK/Spring Security, broader permission penetration tests, and separate cleanup of other service-level subject-name helpers such as `LearningWorkflowService.getPathForUser(...)`.

## 2026-06-09 P3-4-V ResourceGeneration / Agent Trace Detail Roles-First RBAC

- Implemented and verified the ResourceGeneration / Agent Trace detail roles-first RBAC slice.
- `ResourceGenerationController` now reads `CurrentUserService.currentUser()` and derives explicit admin facts only from `UserContext.roles()` for `GET /api/resources/generation-tasks/{taskId}` and `GET /api/agent/resource-generation/tasks/{taskId}/learner-resources`.
- `ResourceGenerationService` now exposes `getTask(userId, currentUserAdmin, taskId)` and `getLearnerResources(userId, currentUserAdmin, taskId)` roles-first overloads.
- `learner-resources` remains owner-only; explicit admin only controls missing-object `NOT_FOUND` semantics and does not gain learner resource read access.
- `AgentTraceController` now derives explicit admin facts from `UserContext.roles()` for `GET /api/agent/tasks/{taskId}/trace`.
- `AgentTraceGovernanceController` now derives explicit admin facts from `UserContext.roles()` for `GET /api/agent/traces`.
- `AgentTraceGovernanceService` now exposes roles-first `search(...)` and `getTrace(...)` overloads.
- Bearer `ADMIN sub=ops_admin` can read ResourceGeneration task detail, Agent Trace detail, and Agent Trace search despite spoofed `X-User-Id`.
- Bearer `USER sub=admin` no longer gains admin detail/search semantics through subject-name role confusion.
- Non-admin missing/foreign ResourceGeneration task and Agent Trace detail paths continue to return safe `FORBIDDEN` without data; admin missing returns `NOT_FOUND`.
- No dependency, schema, API path, request DTO, response DTO, frontend, formal OAuth2/JWK/Spring Security, Agent/RAG/model runtime, Review Gate, CourseAccessService legacy cleanup, or cancel authorization change was added.
- Verification passed: RED observed `9 run, 9 failures`; focused `9/9`; adjacent `108/108`; full backend `463 run, 0 failures, 0 errors, 1 skipped`; integration review PASS.
- Remaining P3 follow-up work: CourseAccessService legacy overload cleanup, broader class/course authorization, formal OAuth2/JWK/Spring Security, and broader permission penetration tests.

## 2026-06-09 P3-4-U Review Gate ResourceReview Roles-First RBAC

- Implemented and verified the Review Gate ResourceReview roles-first RBAC slice.
- `ResourceReviewController` now reads `CurrentUserService.currentUser()` and derives admin/teacher facts only from `UserContext.roles()` for `GET /api/reviews/resources` and `POST /api/reviews/resources/{reviewId}/decision`.
- `ReviewGovernanceService` now exposes `listResourceReviews(reviewerUserId, reviewerAdmin, reviewerTeacher, status)` and `decide(reviewerUserId, reviewerAdmin, reviewerTeacher, reviewId, request)` roles-first overloads.
- Legacy `listResourceReviews(reviewerUserId, status)` and `decide(reviewerUserId, reviewId, request)` remain for compatibility.
- Bearer `ADMIN sub=ops_admin` can list/decide despite spoofed `X-User-Id`; Bearer `TEACHER sub=instructor_1` can review own-course resources without a `teacher_` subject prefix.
- Bearer `USER sub=admin` and Bearer `USER sub=teacher_1` no longer gain admin/teacher review permissions through subject-name role confusion.
- Teacher missing/foreign review decision paths continue to return safe `FORBIDDEN` without data.
- No dependency, schema, API path, request DTO, response DTO, frontend, formal OAuth2/JWK/Spring Security, ResourceGeneration, Agent Trace, or CourseAccessService legacy cleanup change was added.
- Verification passed: RED observed `16 run, 3 failures`; focused `16/16`; adjacent `56/56`; full backend `454 run, 0 failures, 0 errors, 1 skipped`; target code-path integration review PASS.
- Remaining P3 follow-up work: ResourceGeneration/Agent Trace detail roles-first RBAC, CourseAccessService legacy overload cleanup, broader class/course authorization, formal OAuth2/JWK/Spring Security, and broader permission penetration tests.

## 2026-06-09 P3-4-T Orchestrator RESOURCE_GENERATION Create Roles-First RBAC

- Implemented and verified the Orchestrator `RESOURCE_GENERATION` create/retry roles-first RBAC slice.
- `OrchestratorWorkflowController` now reads `CurrentUserService.currentUser()` and derives admin/teacher facts only from `UserContext.roles()` for `POST /api/orchestrator/workflows` and `POST /api/orchestrator/workflows/{workflowId}/retry`.
- `OrchestratorWorkflowService` now exposes `createWorkflow(ownerUserId, currentUserAdmin, currentUserTeacher, request)` and `retryWorkflow(ownerUserId, currentUserAdmin, currentUserTeacher, workflowId)`.
- The `RESOURCE_GENERATION` branch now calls the roles-first `ResourceGenerationService.createTaskInWorkflow(ownerUserId, currentUserAdmin, currentUserTeacher, request, context)` path instead of the legacy subject-name inference path.
- ResourceGeneration remains owner-only: Bearer admin/teacher do not gain代创建 ability through Orchestrator.
- Bearer `USER sub=admin` no longer bypasses course enrollment through Orchestrator `RESOURCE_GENERATION`; Bearer token roles override spoofed `X-User-Id`.
- Forbidden Orchestrator ResourceGeneration create leaves no ResourceGeneration/model/token/citation durable side effects. Course-enrollment denial may keep safe failed Orchestrator workflow evidence.
- No dependency, schema, API path, request DTO, response DTO, frontend, formal OAuth2/JWK/Spring Security, ResourceGeneration direct create, or ResourceGeneration detail/trace/cancel/review change was added.
- Verification passed: RED observed `28 run, 1 failure`; focused `28/28`; adjacent `94/94`; full backend `449 run, 0 failures, 0 errors, 1 skipped`; integration review PASS for P3-4-T scope.
- Remaining P3 follow-up work: broader class/course authorization, formal OAuth2/JWK/Spring Security, and broader permission penetration tests.

## 2026-06-09 P3-4-S LearningPath / ResourceGeneration Direct Create Roles-First RBAC

- Implemented and verified the direct HTTP create roles-first RBAC slice for LearningPath and ResourceGeneration.
- `LearningPathController` now reads `CurrentUserService.currentUser()` and derives admin/teacher facts only from `UserContext.roles()` for `POST /api/learning-paths`.
- `LearningWorkflowService` now exposes `createPathForUser(currentUserId, currentUserAdmin, currentUserTeacher, request)`; explicit `ADMIN` can create course-bound learning paths for other learners and bypass enrollment, while `USER sub=admin` cannot.
- `ResourceGenerationController` now reads `CurrentUserService.currentUser()` and derives admin/teacher facts only from `UserContext.roles()` for `POST /api/resources/generation-tasks`.
- `ResourceGenerationService` direct create now exposes a roles-first overload but keeps owner-only semantics; admin/teacher do not gain代创建 ability, and course-bound direct create does not use admin enrollment bypass.
- `CourseAccessService` now has a roles-first `requireLearnerEnrolledForExistingCourse(currentUserId, currentUserAdmin, learnerId, courseId)` overload; the legacy signature remains for compatibility.
- Forbidden ResourceGeneration direct create is denied before request replay, safety checks, Agent run, generation task/resource/review, model-call, or token-usage side effects.
- No dependency, schema, API path, request DTO, response DTO, frontend, formal OAuth2/JWK/Spring Security, or Orchestrator workflow changes were added in P3-4-S; Orchestrator `RESOURCE_GENERATION` create/retry was later completed by P3-4-T.
- Verification passed: RED observed `32 run, 3 failures`; focused `32/32`; adjacent `91/91`; full backend `446 run, 0 failures, 0 errors, 1 skipped`; integration review CONDITIONAL PASS for direct API scope.
- Remaining P3 follow-up work after P3-4-T: broader class/course authorization, formal OAuth2/JWK/Spring Security, and broader permission penetration tests.

## 2026-06-09 P3-4-R Assessment / GradingEvaluation Roles-First RBAC

- Implemented and verified the Assessment read paths and GradingEvaluation roles-first RBAC slice.
- `AssessmentController` now reads `CurrentUserService.currentUser()` and derives admin/teacher facts only from `UserContext.roles()` for answer/wrong-question list/detail and grading evaluation HTTP paths.
- `AssessmentService` now exposes roles-first overloads for `listAnswers`, `listWrongQuestions`, `answerDetail`, and `wrongQuestionDetail`; legacy overloads remain for compatibility.
- `GradingEvaluationService` now exposes `evaluate(currentUserId, currentUserAdmin, currentUserTeacher, request)` and routes HTTP authorization through role-aware `CourseAccessService.requireCourseRead(...)`.
- Bearer `ADMIN sub=ops_admin` works despite spoofed `X-User-Id`; Bearer `TEACHER sub=instructor_1` can read/evaluate own-course assessment data without a `teacher_` subject prefix.
- Bearer `USER sub=admin` and Bearer `USER sub=teacher_1` cannot use subject-name role confusion to gain admin/teacher Assessment or GradingEvaluation access.
- No dependency, schema, API path, request DTO, response DTO, frontend, `POST /api/assessment/answers`, or formal OAuth2/JWK/Spring Security changes were added.
- Verification passed: RED observed `37 run, 11 failures`; focused `37/37`; adjacent `123/123`; full backend `442 run, 0 failures, 0 errors, 1 skipped`; integration review PASS.
- Remaining P3 follow-up work: LearningPath/ResourceGeneration course-bound create role facts, broader class/course authorization, formal OAuth2/JWK/Spring Security, and broader permission penetration tests.

## 2026-06-09 P3-4-Q Analytics Student Summary Roles-First RBAC

- Implemented and verified the Analytics student summary roles-first RBAC补口 slice.
- `AnalyticsService.requireCourseReadForStudentSummary(...)` now calls the role-aware `CourseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId)` overload.
- Bearer `ADMIN sub=ops_admin` works despite spoofed `X-User-Id` for course-scoped student summary; admin missing course keeps `NOT_FOUND`.
- Bearer `TEACHER sub=instructor_1` can read own-course active-enrolled learner summaries without a `teacher_` subject prefix.
- Bearer `USER sub=teacher_1` cannot use subject-name role confusion to read course-scoped student summary as teacher.
- Teacher missing and foreign course-scoped student summary requests both return safe `FORBIDDEN` without `data`.
- No dependency, schema, API path, request DTO, response DTO, frontend, Assessment/GradingEvaluation, LearningPath/ResourceGeneration, or formal OAuth2/JWK/Spring Security changes were added.
- Verification passed: RED observed `34 run, 3 failures`; focused `34/34`; adjacent `68/68`; full backend `431 run, 0 failures, 0 errors, 1 skipped`.
- Remaining P3 follow-up work: Assessment/GradingEvaluation legacy role inference, LearningPath/ResourceGeneration course-bound create role facts, broader class/course authorization, and formal OAuth2/JWK/Spring Security.

## 2026-06-09 P3-4-P RAG KB Management Roles-First RBAC

- Implemented and verified the RAG Knowledge Base management roles-first RBAC slice.
- `KnowledgeBaseController` and `DocumentController` now read `CurrentUserService.currentUser()` and derive admin/teacher facts only from `UserContext.roles()`.
- `KnowledgeBaseService`, `DocumentService`, and `PermissionService` now expose role-aware overloads while keeping legacy signatures for compatibility with non-target paths.
- Bearer `ADMIN sub=ops_admin` works despite spoofed `X-User-Id` and can list/read/write active KBs.
- Bearer `TEACHER sub=instructor_1` can upload own-course document metadata without a `teacher_` subject prefix.
- Bearer `USER sub=admin` and Bearer `USER sub=teacher_1` role-confusion cases return safe `FORBIDDEN`.
- Non-admin missing document/reindex/index-task requests collapse to safe `FORBIDDEN`; admin missing retains `NOT_FOUND`.
- `DocumentService` course metadata scope now calls role-aware `CourseAccessService.requireCourseRead(...)` / `requireCourseManage(...)`.
- No dependency, schema, API path, request DTO, response DTO, frontend, parser/vector/index worker/storage/model runtime, `/api/rag/query` retrieval runtime, or formal OAuth2/JWK/Spring Security changes were added.
- Verification passed: RED observed `26 run, 6 failures`; focused controller `26/26`; focused service `4/4`; adjacent `30/30` and `34/34`; full backend `426 run, 0 failures, 0 errors, 1 skipped`.
- Broader class/course authorization and broader permission penetration tests remain open P3 follow-up work; `/api/rag/query` roles-first retrieval runtime migration and formal OAuth2/JWK/Spring Security were later completed by dedicated P3-4 slices.

## 2026-06-09 P3-4-O Evaluation Set / Run Roles-First RBAC

- Implemented and verified the Evaluation Set / Run management API roles-first RBAC slice.
- `EvaluationSetController` and `EvaluationRunController` now read `CurrentUserService.currentUser()` and derive admin/teacher facts only from `UserContext.roles()`.
- `EvaluationSetService` and `EvaluationRunService` management methods now require explicit `currentUserId/currentUserAdmin/currentUserTeacher` inputs.
- Service authorization helpers no longer infer admin/teacher from subject strings such as `admin` or `teacher_*`.
- Bearer `ADMIN sub=ops_admin` works despite spoofed `X-User-Id`; Bearer `TEACHER sub=instructor_1` can manage authorized evaluation sets without a `teacher_` prefix.
- Bearer `STUDENT/USER sub=admin` and Bearer `USER sub=teacher_1` role-confusion cases return safe `FORBIDDEN`.
- Non-admin missing and foreign evaluation set / run comparison requests collapse to safe `FORBIDDEN`; admin missing retains `NOT_FOUND`.
- No dependency, schema, API path, request DTO, frontend, RAG KB management, broader class/course, or formal OAuth2/JWK/Spring Security changes were added.
- Verification passed: RED observed `15 run, 9 failures`; focused controller `15/15`; focused service `19/19`; adjacent `48/48` and `73/73`; full backend `419 run, 0 failures, 0 errors, 1 skipped`.
- Broader class/course authorization and broader permission penetration tests remain open P3 follow-up work; RAG KB management full RBAC and formal OAuth2/JWK/Spring Security were later completed by dedicated P3-4 slices.

## 2026-06-09 P3-4-N PromptVersion Management API RBAC

- Implemented and verified the PromptVersion management API roles-first RBAC and `promptText` exposure hardening slice.
- `PromptVersionController` now injects `CurrentUserService`, reads `UserContext.roles()`, and passes explicit admin/teacher role facts into `PromptVersionService`.
- `POST /api/agent/prompt-versions` now requires explicit `ADMIN` role; `GET /api/agent/prompt-versions` and `GET /api/agent/prompt-versions/{code}/{version}` require `ADMIN` or `TEACHER`.
- `PromptVersionResponse` now supports role-based redaction; admin responses include `promptText`, while teacher metadata responses omit `promptText`.
- Bearer roles override spoofed `X-User-Id`; Bearer `STUDENT sub=admin`, Bearer `USER sub=admin`, and Bearer `USER sub=teacher_1` do not gain PromptVersion management access.
- `PromptVersionService` public management methods require explicit role facts; internal `findActiveByCode(...)` remains available for model-call linkage.
- No dependency, schema, API path, frontend, formal OAuth2/JWK/Spring Security, Evaluation, RAG KB, or full RBAC migration changes were added.
- Verification passed: RED observed, focused `14/14`, adjacent `48/48`, full backend `410 run, 0 failures, 0 errors, 1 skipped`.
- GradingEvaluation legacy caller migration, RAG KB management full RBAC, and broader class/course authorization remain open P3 follow-up work; formal OAuth2/JWK/Spring Security was later completed by the dedicated P3-4 formal auth slice.

## 2026-06-09 P3-4-M Course API / CourseAccessService Roles-First Overload

- Implemented and verified the Course API / Knowledge Catalog roles-first overload slice.
- `CourseAccessService` now has role-aware `requireCourseRead(...)`, `requireCourseManage(...)`, and `listCoursesForUser(...)` overloads; legacy signatures remain and delegate to legacy inference for compatibility.
- `KnowledgeCatalogService` now has role-aware create/read/list/chapter/knowledge point/dependency/graph overloads and routes Course API object authorization through role-aware course access.
- `CourseController` and `KnowledgePointController` pass explicit `UserContext.roles()` facts into `KnowledgeCatalogService` for Course API / knowledge graph main paths.
- Authorization semantics: Bearer `ADMIN sub=ops_admin` can list/read existing courses despite spoofed `X-User-Id`; Bearer admin missing course returns `NOT_FOUND`; Bearer `TEACHER sub=instructor_1` can read/manage own course without a `teacher_` prefix; Bearer student spoofing and `USER sub=admin` / `USER sub=teacher_1` role-confusion cases return safe `FORBIDDEN`.
- No dependency, schema, API path, frontend, RAG, model-provider, VectorDB, formal OAuth2/JWK/Spring Security, or full RBAC migration changes were added.
- Verification passed: RED observed, focused `20/20`, adjacent `63/63`, full backend `403 run, 0 failures, 0 errors, 1 skipped`.
- Other legacy `CourseAccessService` callers, broader class/course authorization, and PromptVersion/Evaluation/RAG KB management full RBAC remain open P3 follow-up work; formal OAuth2/JWK/Spring Security was later completed by the dedicated P3-4 formal auth slice.

## 2026-06-09 P3-4-L Class Analytics Roles-First Course Scope

- Implemented and verified the class analytics roles-first course-scope slice.
- `AnalyticsController.teacherClassSummary(...)` now passes `CurrentUserService.currentUserId()`, `isAdmin()`, and `isTeacherUser()` into `AnalyticsService`.
- `AnalyticsService.teacherClassSummary(...)` now has a role-aware overload; the legacy overload remains and delegates to the role-aware path.
- Authorization semantics: admin existing course allowed; admin missing course returns `NOT_FOUND`; teacher must have `currentUserId == Course.teacherId`; student/spoofed header denied; non-admin missing/foreign course returns safe `FORBIDDEN`.
- No dependency, schema, API path, frontend, RAG, model-provider, VectorDB, formal OAuth2/JWK/Spring Security, or broader class/course changes were added.
- Verification passed: RED observed, focused `29/29`, adjacent `56/56`, full backend `396 run, 0 failures, 0 errors, 1 skipped`.
- Broader class/course authorization, full `CourseAccessService` role-aware migration, and PromptVersion/Evaluation/RAG KB management full RBAC remain open P3 follow-up work; formal OAuth2/JWK/Spring Security was later completed by the dedicated P3-4 formal auth slice.

## 2026-06-09 P3-2-I Real OCR Provider

- Implemented and verified the process-based OCR fallback provider slice.
- Added `ProcessOcrFallbackProvider` behind the existing `OcrFallbackProvider` SPI.
- `RagParserOcrProperties` now includes `process.command`, `process.timeout`, and `process.max-output-chars`.
- Provider uses `ProcessBuilder(List<String>)`, sends parse input bytes through stdin, consumes stderr without exposing it, reads stdout as UTF-8 OCR text, and enforces timeout/output limits.
- Command missing returns `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`; command failure, timeout, exception, or oversized stdout returns `FAILED / OCR_PROVIDER_FAILED`.
- Spring Boot 3.5 binding/injection ambiguity was fixed with explicit constructor binding on `RagParserOcrProperties` and explicit `@Autowired` on the provider's Spring constructor.
- No OCR SDK/native/cloud dependency, schema, API, frontend, `IndexService`, retrieval, citation, embedding, VectorDB, or `pom.xml` changes were added.
- Verification passed: RED observed, focused `21/21`, adjacent `33/33`, no OCR/native/cloud dependencies in dependency tree, compile success, full backend `392 run, 0 failures, 0 errors, 1 skipped`.
- OCR SDK/native/cloud provider and industrial PDF/DOCX layout/table/TOC recognition remain open P3-2 follow-up work; Qdrant adapter minimum integration was completed later, while real service smoke/ops validation remains separate.

## 2026-06-09 P3-2-H Configurable OCR Fallback Provider

- Implemented and verified the configurable OCR fallback provider boundary slice.
- Added `RagParserOcrProperties` with `learning-os.rag.parser.ocr.enabled` and `learning-os.rag.parser.ocr.provider`.
- Added `OcrFallbackProvider` as the future OCR provider SPI and `ConfigurableOcrFallbackService` as the single Spring-facing `OcrFallbackService`.
- Default OCR remains disabled (`enabled=false`, `provider=none`) and returns `DISABLED / OCR_DISABLED`.
- Enabled missing provider returns `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`; provider exceptions return safe `FAILED / OCR_PROVIDER_FAILED`.
- `NoopOcrFallbackService` remains directly instantiable but is no longer a competing Spring service bean.
- No OCR SDK/native/cloud dependency, schema, API, frontend, `IndexService`, retrieval, citation, embedding, VectorDB, or parser SDK changes were added.
- Verification passed: focused `15/15`, adjacent `33/33`, no OCR/native/cloud OCR dependencies in dependency tree, compile success, full backend `385 run, 0 failures, 0 errors, 1 skipped`.
- Real OCR implementation and industrial PDF/DOCX layout/table/TOC recognition remain open P3-2 follow-up work; Qdrant adapter minimum integration was completed later, while real service smoke/ops validation remains separate.

## 2026-06-09 P3-2-G Real PDF/DOCX Parser SDK Provider

- Implemented and verified the real PDF/DOCX parser SDK provider slice.
- Added `PdfBoxDocumentFormatParser` using Apache PDFBox `3.0.7` for page-by-page PDF text extraction.
- Added `PoiDocxDocumentFormatParser` using Apache POI `poi-ooxml:5.5.1` for DOCX paragraph text, Heading1-6 metadata, page break, tab, and line-break handling.
- Added `ParserResourceLimits` and `ParserTextSanitizer` for conservative input/output limits and normalized text output.
- `DocumentParserService` keeps the P3-2-F provider registry boundary; Spring bean providers override default lightweight PDF/DOCX providers, while no-arg construction remains lightweight-compatible for existing parser tests.
- `IndexService`, API, DB migration, frontend, retrieval/citation, VectorDB, and OCR implementation were not changed.
- PDFBox `commons-logging` transitive dependency is excluded to avoid Spring JCL conflict.
- Verification passed: RED observed, focused `26/26`, adjacent `15/15`, dependency tree verified, full backend `378 run, 0 failures, 0 errors, 1 skipped`.
- Real OCR fallback and industrial-grade PDF/DOCX layout/table/TOC recognition remain open P3-2 follow-up work; Qdrant adapter minimum integration was completed later, while real service smoke/ops validation remains separate.

## 2026-06-09 P3-2-F Parser Provider Boundary + OCR Fallback Contract

- Implemented and verified the parser provider boundary / OCR fallback contract slice.
- Added `ParseInput`, `DocumentFormatParser`, `OcrFallbackService`, `OcrFallbackResult`, and `NoopOcrFallbackService`.
- `DocumentParserService` now dispatches through a provider registry while preserving the public `parse(KbDocument, byte[])` method and the `ParsedDocument/ParsedSection` output boundary consumed by `IndexService`.
- Default lightweight Markdown/TXT/PDF/DOCX providers remain internal to `DocumentParserService` for a small diff; external `DocumentFormatParser` beans can override or extend provider behavior later.
- Noop OCR returns `DISABLED / OCR_DISABLED / ""`; image-only PDFs still produce no sections and never fallback to raw PDF bytes.
- Provider failures continue to map to the safe `DOCUMENT_PARSE_FAILED` code.
- No dependency, schema, API, frontend, VectorDB, retrieval, or citation contract changes.
- Verification passed: RED observed, focused `19/19`, adjacent `15/15`, extended adjacent `49/49`, full backend `371 run, 0 failures, 0 errors, 1 skipped`.

## 2026-06-09 P3-4-K Permission Penetration Matrix

- Implemented and verified the current transitional P3-4 permission penetration matrix slice.
- Added staging header-only auth denial coverage in `DevAuthFilterTest`.
- Added Bearer roles controller integration coverage for analytics overview and token-budget governance; spoofed `X-User-Id` no longer drives admin-only access.
- Fixed analytics admin-only gates to use roles-first `CurrentUserService.isAdmin()` / `currentUserAdmin` rather than the literal `"admin"` user id.
- Added course/RAG negative coverage: active enrolled students still cannot write course graph data; dropped enrollment courses are hidden from student course lists; students cannot spoof RAG document course metadata even when they own a public KB and are enrolled.
- No dependency, schema, frontend, formal OAuth2/JWK/Spring Security, or broader class/course changes.
- Verification passed: focused `65/65`, adjacent regression `119/119`, full backend `367 run, 0 failures, 0 errors, 1 skipped`.

## 2026-06-08 P3-3-C Real Model Provider Adapter

- Implemented and verified the minimal Spring AI OpenAI-compatible real model provider adapter slice.
- `backend/pom.xml` now uses Spring AI `1.0.8` and includes `spring-ai-starter-model-openai`.
- `AiModelGateway` calls Spring AI `ChatModel` when `AI_MODEL_PROVIDER != none` and chat model runtime configuration is complete.
- `EmbeddingService` calls Spring AI `EmbeddingModel` when embedding provider runtime configuration is complete.
- Default `AI_MODEL_PROVIDER=none` remains deterministic/noop and does not make external calls.
- Missing provider beans fail closed with safe error codes; provider non-JSON/schema-invalid output and raw provider errors are sanitized.
- Runtime chat model can be set with `AI_CHAT_MODEL=gpt-5.5`; secrets and base URLs must stay in runtime environment variables only.
- Verification passed: focused `18/18`, adjacent regression `53/53`, dependency tree, compile, full backend `357 run, 0 failures, 0 errors, 1 skipped`.

## 2026-06-08 P3-4-J Analytics Student Summary Course Scope

- Implemented and verified course-scoped authorization for `GET /api/analytics/students/{learnerId}/summary`.
- Endpoint now accepts optional `courseId`; teacher calls require `courseId`, own course, and active enrolled learner.
- Student course-scoped reads require `learnerId == currentUserId` plus active course enrollment.
- Admin can read any existing learner summary globally or scoped to an existing course.
- Course-scoped aggregation filters learning paths, path nodes, mastery records, and wrong questions to the requested course.
- No dependency/schema/frontend/RAG/model-provider changes.
- Verification passed: TDD RED observed, focused `22/22`, adjacent regression `60/60`, full backend `350 run, 0 failures, 0 errors, 1 skipped`.

## 2026-06-08 P3-4-I Real Auth Context / RBAC Compatibility

- Implemented and verified Bearer HS256 JWT auth-context bootstrap in `DevAuthFilter` and `CurrentUserService`.
- Valid Bearer tokens establish `UserContext` from `sub`, optional display name, and roles; invalid Bearer tokens return `UNAUTHORIZED` and never fall back to `X-User-Id`.
- `prod` / `production` / `staging` no longer trust `X-User-Id`; `dev` / `test` keep `X-User-Id` / `dev_user` compatibility only when Bearer is absent.
- `CurrentUserService.currentUser()` was added; `isAdmin()` / `isTeacherUser()` now prefer roles and only use legacy userId inference in dev/test.
- `AuthProperties` was added and bound through `learning-os.auth.jwt-secret` / `learning-os.auth.issuer`; no dependency, schema, frontend, or business-service changes.
- Verification passed: TDD RED observed, focused `13/13`, adjacent regression `74/74`, full backend `345 run, 0 failures, 0 errors, 1 skipped`.

## 2026-06-08 P3-4-H RAG Document Course/Chapter Metadata Scope

- Implemented and verified course/chapter metadata authorization for `POST /api/knowledge-bases/{kbId}/documents`.
- `DocumentService.upload(...)` now validates normalized `courseId/chapterId` before requestId hashing/replay, object storage, `kb_document` persistence, and `kb_index_task` creation.
- Non-empty `courseId` requires centralized course read + manage authorization; teachers are limited to own courses and students cannot spoof course metadata even when they can write to a KB.
- Admin missing course returns `NOT_FOUND`; teacher/student missing or foreign course returns safe `FORBIDDEN` without `data`.
- Non-empty `chapterId` requires `courseId`; missing/foreign chapter returns generic `VALIDATION_ERROR` without echoing object identifiers.
- No dependency/schema/frontend/DTO/parser/vector/model-provider changes.
- Verification passed: TDD RED observed, focused `18/18`, adjacent regression `58/58`, full backend `337 run, 0 failures, 0 errors, 1 skipped`.

## 2026-06-08 P3-4-G Grading Evaluation Course Scope

- Implemented and verified course-scoped `POST /api/assessment/grading-evaluations`.
- HTTP requests now require `courseId` for teacher/admin calls; legacy score-array metric calculation remains compatible only when bound to a course.
- Student requests return `FORBIDDEN` before course or sample validation.
- Teacher requests are limited to own courses; teacher missing/foreign course returns safe `FORBIDDEN` without `data`.
- Admin requests may evaluate any existing course; admin missing course returns `NOT_FOUND`.
- Non-blank sample `knowledgePointId` values must belong to request `courseId`; outside/missing knowledge points return generic `VALIDATION_ERROR`.
- Verification passed: TDD RED observed, focused `29/29`, adjacent regression `58/58`, full backend `329 run, 0 failures, 0 errors, 1 skipped`.

## 2026-06-08 P3-4-F Assessment Record List RBAC / Pagination

- Implemented and verified `GET /api/assessment/answers` and `GET /api/assessment/wrong-questions`.
- Student list reads are owner-only; explicit foreign `learnerId` returns safe `FORBIDDEN`.
- Teacher list reads require `courseId`, own course, and active learner enrollment; own-course but unenrolled learner filter returns empty page.
- Admin list reads are global and support `learnerId` / `courseId` filters; missing course returns `NOT_FOUND`.
- Pagination is bounded with `page >= 0` and `size 1..50`.
- List summary DTOs omit answer text, `requestId`, `requestHash`, `responseJson`, `payloadJson`, `gradingResultId`, `causeAnalysis`, and `replanRecordId`.
- Verification passed: TDD RED observed, focused `AssessmentControllerTest` `19/19`, adjacent regression `59/59`, full backend `322 run, 0 failures, 0 errors, 1 skipped`.

## 2026-06-08 P3-4-E Assessment Record RBAC Matrix

- Implemented and verified `GET /api/assessment/answers/{answerId}` and `GET /api/assessment/wrong-questions/{wrongQuestionId}`.
- Student reads are owner-only; teacher reads require own course plus active learner enrollment; admin reads are global.
- Non-admin missing/foreign answer and wrong-question reads return safe `FORBIDDEN` without `data`; admin missing returns `NOT_FOUND`.
- Detail DTOs do not expose `requestId`, `requestHash`, `responseJson`, or `payloadJson`.
- Verification passed: focused `AssessmentControllerTest` `13/13`, adjacent regression `53/53`, full backend `316 run, 0 failures, 0 errors, 1 skipped`.

## 2026-06-08 P3-4-D Course Enrollment Scope

- Implemented and verified: V19 `course_enrollment`, `CourseEnrollment`, `CourseEnrollmentRepository`, and centralized `CourseAccessService`.
- Student course list/detail/knowledge-graph reads now use active enrollment for student scope while preserving admin/global and teacher-own-course behavior.
- Course-bound learning path and resource generation creation now check learner active enrollment only when `goalId` is an existing course; non-course template goals stay compatible.
- Teacher class summary learner set now comes from active `course_enrollment` rather than learning-path inference.
- Verification passed: focused tests `74/74`, `ResourceReviewControllerTest` `11/11`, adjacent tests `87/87`, full backend `312 run, 0 failures, 0 errors, 1 skipped`.

## Tech Stack

- Java 21
- Spring Boot 3.x
- Spring AI / Spring AI Alibaba as the planned model integration layer
- MySQL 8.x as primary database
- Flyway migrations
- JPA repositories in the current backend implementation

## Package Structure

```text
backend/src/main/java/com/learningos/
  common/         # Auth, trace, exception handling
  user/           # User management
  knowledge/      # Course, chapter, knowledge graph
  learning/       # Learner profile, learning workflow, path planning
  assessment/     # Answer submission, grading, feedback diagnosis
  rag/            # Knowledge base, document, chunk, retrieval, citation
  agent/          # Resource generation, review, agent trace, model/token logs
  evaluation/     # Evaluation set and sample management
  orchestrator/   # Workflow orchestration
  tutor/          # AI tutoring API
  analytics/      # Operations and learning analytics
  health/         # Dependency health checks
```

## Layering

```text
Controller
-> Application Service
-> Domain Service / Agent Orchestrator
-> Tool adapter
-> Service
-> Repository
-> Database
```

Core rule: Agents and tools must go through services. They must not access repositories directly.

## Core Backend Rules

- Backend owns all AI and model-provider calls.
- Frontend never calls LLM APIs and never stores model API keys.
- Permission checks happen in backend code, not in prompts.
- RAG retrieval must filter allowed KB ids before chunk lookup.
- RAG answers and course-grounded resources need source citations.
- Agent workflows must write `agent_task` and ordered `agent_trace` records.
- Model calls must write `model_call_log` and `token_usage_log`.
- Generated learning resources must pass Critic/teacher review before learner release.
- Long-running AI tasks need status, retry, failure reason, and trace evidence.

## Completed Backend Capabilities

| Capability | Status | Notes |
|---|---|---|
| Spring Boot backend foundation | Done | Java 21, controllers, services, repositories, common API envelope |
| Trace and current user context | Done | `CurrentUserService`, `TraceFilter`, `X-Trace-Id`, and Bearer JWT / dev-test fallback auth-context bootstrap; HTTP request completion now logs whitelisted `traceId/userId/route/status/latencyMs/errorCode`, and unsafe client trace ids are replaced before response/logging |
| RAG foundation | Done | KB, permissions, document metadata, chunks, query log, citation DTOs; online query now uses allowed-KB keyword + recency + RRF with safe reranker fallback metadata; embedding/vector adapter boundary exists with default noop and allowed-KB vector hit filtering; Spring AI OpenAI-compatible embedding provider adapter is available behind `EmbeddingService`; Qdrant real VectorDB adapter minimum integration is available behind `learning-os.rag.vector.*`; real PDFBox/POI parser providers, configurable OCR fallback selection, and process OCR provider are available behind parser boundaries |
| Learning loop foundation | Done | learner profile, learning goal, path, path nodes, mastery records; profile dimensions now include seven explicit fields plus `lastEvidenceId`; learning paths persist `profileSnapshot`; Knowledge DAG path planning now distinguishes `PREREQUISITE` from non-locking `RELATED` / `ADVANCED` edges, prioritizes low-mastery prerequisite remediation below `0.6`, and path nodes expose recommendation metadata |
| Resource generation governance | Partial | task/resource/review/trace/model/token logs exist; resource generation tasks persist `profileSnapshot` and pass it into ResourceAgent model requests; ResourceAgent now carries `agent-resource-v1`, gateway validates resource structured output, success model evidence uses gateway response provider/model/latency/token/cost, `model_call_log.provider` is persisted as a low-cardinality safe value, and Spring AI OpenAI-compatible chat provider adapter is available behind `AiModelGateway`; unreviewed create/detail/replay responses hide `markdownContent`; Review Gate state model, structured audit fields, admin-global/teacher-own-course review authorization, task-level `source_citation`, Critic `citationCheck`, `NO_SOURCE` approval/release blocking, and task-level recovery metadata are implemented; real RAG retrieval grounding, resource-level citation schema, DashScope/VectorDB enhancements, and full RBAC still needed |
| Agent state machine governance | Done | `AgentRunRecorder` validates task/trace statuses and transitions, supports cancellation, writes structured failures, records sanitized tool calls, and preserves failed resource generation task evidence |
| Assessment feedback loop | Partial | answer, grading, wrong-cause feedback, BKT-lite mastery update from latest learner history, replan record, answer detail/list, and wrong-question detail/list exist; assessment detail and list RBAC matrices are implemented; offline grading quality evaluation covers MAE, grade agreement, wrong-cause agreement, grouped analysis, and course-scoped teacher/admin authorization; rubric/model grading and scheduled/evaluation-set runner remain pending |
| Idempotent resource generation | Done | `requestId` support and retry wrapper for structured model generation |
| Idempotent answer submission | Done | `requestId` required on `POST /api/assessment/answers`; same payload replays first response, payload conflict returns 409, DB unique-key race is handled by replay |
| Idempotent RAG query | Done | `RAG_QA` requires `requestId`; `kb_query_log` stores response snapshots; same payload replays first workflow/response, conflicting payload returns 409 |
| Idempotent document upload | Done | Optional multipart `requestId`; same payload replays the first upload response, conflicting payload returns 409, and `kb_document` has request snapshot fields plus unique key; course/chapter metadata is now authorized before requestId/replay/storage/save/index side effects |
| RAG index active dedup | Done | Repeated document reindex reuses latest `PENDING/RUNNING` task; `FAILED/SUCCEEDED` creates a new `PENDING` task |
| RAG index timeout recovery | Done | Service and scheduler can recover timed-out `RUNNING` index tasks to `FAILED`, increment retry count, record error evidence, and allow reindex retry |
| RAG index worker progress | Done | Due `PENDING` index tasks are auto-claimed by a scheduled worker; tasks persist independently committed progress, phase, heartbeat, lease, next retry, and recoverable state; worker batches isolate single-task failures; task detail API enforces KB read permission |
| RAG chunk production metadata | Done | `IndexService` now uses token-window chunking with 40-token overlap, stable SHA-256 `chunkHash`, Markdown heading hierarchy, and structured short chunk metadata |
| RAG parser adapter minimal | Done | `rag/parser` now owns Markdown/TXT/PDF/DOCX section parsing; `IndexService` consumes `DocumentParserService` output while preserving chunk/hash/metadata behavior and safe parser failure codes |
| RAG parser layout / page hierarchy | Done | P3-2-E adds no-dependency best-effort simple PDF page segmentation, DOCX same-paragraph page-break section splitting, and `w:tab` / non-page `w:br` separators; chunk page metadata propagation is verified; OCR and VectorDB were handled as separate follow-up tracks |
| RAG real parser SDK provider | Done | P3-2-G adds Apache PDFBox/POI providers behind `rag/parser`; PDF text is extracted per page, DOCX headings/page breaks/separators are extracted through POI, and resource limits plus safe parser failures are enforced; OCR and industrial layout remain open |
| RAG configurable OCR fallback provider | Done | P3-2-H adds disabled-by-default OCR selection and future `OcrFallbackProvider` SPI behind `OcrFallbackService`; missing providers and provider exceptions return safe OCR statuses; real OCR and industrial layout remain open |
| RAG process OCR fallback provider | Done | P3-2-I adds process-based OCR provider behind `OcrFallbackProvider`; explicit runtime command stdout can feed image-only PDF fallback, with timeout/output limits and safe OCR statuses; OCR SDK/native/cloud provider and industrial layout remain open |
| RAG parser metadata contract foundation | Done | P3-2 metadata foundation adds `pageNumSource`, stable 1-based `readingOrderIndex`, `contentKind`, optional `layoutConfidence` / `ocrConfidence`, OCR fallback metadata propagation, and safe chunk `metadataJson` fields without API/DB/dependency/frontend changes; real layout/table/TOC providers, native/cloud OCR, provider confidence pipeline, and rendered page labels remain open |
| RAG real VectorDB adapter minimum integration | Done | Qdrant-backed `VectorIndexAdapter` is available behind `learning-os.rag.vector.*`; default runtime remains disabled/noop, payload excludes raw content/question/prompt/user/storage/secret data, and search returns only `chunkId` payload with vectors disabled. Real service smoke, collection dimension validation, health/ops integration, and gRPC/Netty risk handling remain open |
| Orchestrator workflow query | Done | `POST /api/orchestrator/workflows` returns status context; `GET /api/orchestrator/workflows/{workflowId}` returns task/trace summary and next actions |
| Orchestrator workflow context convergence | Partial | `RESOURCE_GENERATION`, `RAG_QA`, and `ANSWER_SUBMISSION` workflows now execute downstream services with the same `agentTaskId/traceId`; Orchestrator `RESOURCE_GENERATION` create/retry now pass roles-first auth facts into ResourceGeneration workflow create; generic runtime failure evidence is durable; `FAILED RESOURCE_GENERATION` has owner retry; node contract policy is explicit in workflow step responses; background recovery and retry limits remain open |
| Evaluation endpoints | Partial | RAG benchmark metrics, grading quality metrics, `evaluation_set` / `evaluation_sample`, persisted evaluation runs, prompt-version comparison, and a repeatable RAG archive report script are implemented; automatic scheduled evaluation runner remains pending |
| Prompt Version management | Done | `PromptVersion` entity/repository/service/API/tests added; HTTP management APIs now require roles-first admin/teacher access, with admin-only upsert and teacher metadata-only reads that omit `promptText`; model call logs persist provider, prompt code/version, temperature, and structured output schema; P3-3-A/B/C record success provider/model/latency/token/cost from gateway response and keep provider/schema errors sanitized; prompt-version quality comparison is implemented through evaluation runs |
| Analytics overview | Done | overview and token/model aggregation exist; student summary with optional course scope, teacher class summary with roles-first class/course access, admin agent/token/RAG summary fields, and Token / Cost budget governance endpoint are implemented |
| Permission/security minimum hardening | Partial | Profile/path owner checks, object detail anti-enumeration for path/resource task/agent trace/RAG document/reindex/index task, RAG document course/chapter metadata scope, course list/detail/knowledge-graph read scope, active enrollment course scope, analytics student summary course scope, analytics class summary roles-first course scope, assessment answer/wrong-question detail and list RBAC, answer/wrong-question Bearer/header/role-confusion penetration matrix, grading evaluation course scope, PromptVersion management API roles-first RBAC and `promptText` redaction, analytics overview/token-budget role gates, health redaction, strict RAG `kbIds`, `GET /api/rag/query` handler coverage, LearningPath/ResourceGeneration direct create roles-first facts, Orchestrator `RESOURCE_GENERATION` create/retry roles-first facts, Bearer JWT auth-context compatibility, current transitional penetration matrix, formal OAuth2/JWK/Spring Security minimum authentication boundary, backend SSE production auth strategy, target service legacy subject-name authorization cleanup, Assessment submit foreign-questionId guard, Evaluation/Review forged-id matrix, dev/test JWT fallback cleanup, frontend SSE sensitive URL cleanup fallback, and formal production streaming design are implemented; broader class/course/resource RBAC sampling and third-party IdP discovery compatibility remain P3 work |
| Runtime observability | Partial | Structured request logs, Micrometer running metrics, deep health checks, and query-time ops alerting are implemented. `LearningOsMetrics` records HTTP request latency/failures, RAG query latency/count/retrieval/citation/failures, model gateway latency/failures, and token/cost summaries with low-cardinality non-sensitive tags. `/api/health` probes `DataSource`, Redis `ping()`, MinIO endpoint/client construction, and model provider configuration while keeping responses sanitized. `GET /api/analytics/ops/alerts` returns admin-only alerts for slow RAG queries, slow model calls, RAG no-source rate/count, and review backlog through whitelisted DTOs. Dashboards, exporters, external push, persisted alert rules, and real RBAC remain open production work |
| MySQL migration smoke | Partial | Real MySQL 8 smoke is opt-in and Docker-backed; it has verified Flyway V1-V17 historically. V18 provider migration has H2/text convergence coverage and smoke test assertions, but fresh real MySQL V18 smoke is pending because the local 3306 MySQL `root` credential failed with `Access denied`. Normal `mvn test` remains independent of MySQL |

## Current Backend TODO Source

Primary planning file:

- `docs/planning/backend-architecture-todolist.md`

Execution priority:

1. P0: Orchestrator workflow, Agent state machine, idempotency/retry/recovery, review gate hardening.
2. P1: learner profile strategy, Knowledge DAG planning, Educational RAG Router, citation governance, learning analytics APIs.
3. P2: prompt-version quality comparison, RAG/grading metrics, token budget governance, trace dashboard.
4. P3: production RAG indexing, real model integration, permissions, observability. Real MySQL migration smoke is complete.

## Open Issues

| Issue | Priority | Notes |
|---|---|---|
| Workflow recovery strategy is incomplete | P0/P3 | Resource-generation model failure, RAG_QA runtime `ApiException`, and generic `RuntimeException` now leave queryable evidence; resource-generation tasks persist `retryCount/nextRetryAt/lastError/recoverable`; `FAILED RESOURCE_GENERATION` has owner retry; node contracts and RAG/answer resubmit semantics are explicit; background retry workers, retry limits, and retry backoff still need work |
| Transitional permission model is still only partially role-aware | P3/P0 security | Bearer JWT auth-context compatibility, the formal OAuth2/JWK/Spring Security minimum authentication boundary, backend SSE production auth strategy, target service legacy subject-name authorization cleanup, dev/test JWT fallback cleanup, Assessment submit foreign-questionId guard, Evaluation/Review forged-id matrix, frontend SSE sensitive URL cleanup fallback, and formal production streaming design are implemented, but some business-object authorization paths still rely on transitional admin/teacher/student/enrollment scope; review list/decision deny students, allow admin globally, and limit teacher to own-course reviews through `ResourceGenerationTask.goalId -> Course.teacherId`; course read/list/knowledge graph now use admin/teacher/student/enrollment scope; RAG document upload validates course/chapter metadata through course manage scope before side effects; assessment detail/list reads use student owner, teacher own-course active-enrollment, and admin global semantics; answer/wrong-question Bearer/header/role-confusion matrix is pinned by MockMvc tests; grading evaluation requires `courseId`; analytics student and class summaries have course-scoped role-aware gates; LearningPath/ResourceGeneration direct create and Orchestrator `RESOURCE_GENERATION` create/retry now use roles-first facts; `KnowledgeCatalogService`, `AssessmentService`, and `GradingEvaluationService` no longer expose target subject-name authorization overloads. Broader class/course/resource sampling and IdP discovery compatibility remain future work |
| Citation governance is partial | P3/P1 | RAG answers cite sources; generated resources now persist task-level `source_citation` and Critic `citationCheck`; resource-level citation schema and real Course RAG retrieval grounding remain open |
| Prompt/evaluation management needs metric depth | P2 | Prompt Version management API, model-call prompt metadata, evaluation set/sample management, evaluation runs, and prompt-version quality comparison are implemented; dedicated RAG/grading benchmark metrics still need expansion |
| Permission leakage tests need expansion | P3 | Minimum forged `kbIds`, cross-learner profile/path tests, object detail missing-vs-foreign tests, course read/enrollment tests, RAG document course/chapter metadata upload tests, assessment answer/wrong-question detail/list matrix tests, answer/wrong-question Bearer/header/role-confusion tests, grading evaluation course-scope tests, analytics Bearer role admin-only tests, analytics class summary roles-first tests, LearningPath/ResourceGeneration direct create role-confusion tests, Orchestrator ResourceGeneration role-confusion tests, staging header-only denial, Bearer JWT / `X-User-Id` fallback auth-context tests, formal OAuth2/JWK/Spring Security focused tests, and Chat/Tutor SSE production/staging auth tests exist; broader course/class/resource matrix still needs expansion |

## Recent Backend Updates

| Date | Update | Tests |
|---|---|---|
| 2026-06-10 | P3-2 real VectorDB adapter minimum integration completed: Qdrant-backed `VectorIndexAdapter` is connected behind the existing port, default runtime remains disabled/noop, `NoopVectorIndexAdapter` is now a plain implementation with fallback bean registration in `QdrantVectorConfiguration`, upsert payload is low-sensitivity chunk metadata only, and search requests only `chunkId` payload with vector return disabled. Dependency tree records `spring-ai-qdrant-store:1.0.8 -> io.qdrant:client:1.13.0 -> grpc-netty-shaded:1.65.1`; no blind dependency override was added. | `mvn --% -Dtest=QdrantVectorIndexAdapterTest,RagVectorConfigurationTest test`; `mvn --% -Dtest=NoopVectorIndexAdapterTest,IndexServiceVectorPayloadTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest,RagQueryServiceTest test`; `mvn test`; `mvn --% dependency:tree -Dincludes=org.springframework.ai:spring-ai-qdrant-store,io.qdrant:client,io.grpc:grpc-netty-shaded,io.grpc:grpc-api` |
| 2026-06-10 | P3-2 industrial parser metadata foundation completed: `ParsedSection`, `ParsedDocument`, `OcrFallbackResult`, OCR fallback paths, and `IndexService` chunk metadata now carry safe parser metadata for page source, reading order, content kind, and optional layout/OCR confidence. No dependency/schema/API/frontend/retrieval/citation/VectorDB changes. First full backend run hit unrelated MockMvc SSE async race; isolated nested SSE test passed and full backend rerun passed. | RED contract compile failure observed; `mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,IndexServiceTest test`; `mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest test`; `mvn --% -Dtest=com.learningos.common.auth.SseProductionAuthStrategyTest$Production#tutorStreamInProductionDoesNotInferAdminFromBearerSubjectName test`; `mvn test` |
| 2026-06-10 | P3-4 agent task cancel / course create role-confusion residual matrix completed through tests only: `ResourceGenerationControllerTest` now pins Bearer owner-only cancel, `USER sub=admin` / `USER sub=teacher_*` denial, non-owner spoofed owner header denial, and no task/trace mutation on forbidden cancel; `CourseKnowledgeControllerTest` now pins Bearer `USER sub=admin` course-create denial without persistence and Bearer `TEACHER` spoofed admin header non-elevation. No production code/API/DTO/schema/dependency/frontend/Agent/RAG/Spring Security change was added. | `mvn --% -Dtest=ResourceGenerationControllerTest,CourseKnowledgeControllerTest test`; `mvn --% -Dtest=ResourceGenerationControllerTest,AgentTraceControllerTest,AgentRunRecorderTest,CourseKnowledgeControllerTest,CourseAccessServiceTest test`; `mvn test` |
| 2026-06-10 | P3-4 answer record RBAC penetration matrix expansion completed through tests only: `AssessmentControllerTest` now pins Bearer admin/student/teacher role facts, spoofed `X-User-Id`, `USER sub=admin` / `USER sub=teacher_*` role-confusion denial, wrong-question list teacher no-prefix success, and teacher detail active-enrollment denial. No production code/API/DTO/schema/dependency/frontend/Agent/RAG/Spring Security change was added. | `mvn --% -Dtest=AssessmentControllerTest test`; `mvn --% -Dtest=AssessmentControllerTest,AssessmentServiceTest,CourseAccessServiceTest test`; `mvn test` |
| 2026-06-10 | P3-4 service legacy subject-name authorization cleanup completed: `KnowledgeCatalogService`, `AssessmentService`, and `GradingEvaluationService` no longer expose target legacy overloads or helpers that infer admin/teacher from `currentUserId = "admin"` / `teacher_*`; roles-first service entrypoints remain; `GradingEvaluationService` pure metric entries remain intact. No API/DTO/schema/dependency/frontend/Spring Security/RAG/Agent runtime change was added. | `mvn --% -q -DskipTests compile`; `mvn --% -Dtest=CourseAccessServiceTest,AssessmentServiceTest,GradingEvaluationServiceTest test`; `mvn --% -Dtest=CourseKnowledgeControllerTest,CourseAccessServiceTest,AssessmentControllerTest,AssessmentServiceTest,GradingEvaluationServiceTest,AnalyticsControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,RagQueryServiceTest,LearningWorkflowControllerTest test`; `mvn test` |
| 2026-06-10 | P3-4 SSE production auth strategy completed: Chat/Tutor SSE production/staging backend auth is pinned to Bearer/JWT fail-closed; no Bearer, invalid Bearer, and header-only spoofing return `UNAUTHORIZED` before async work or `RagQueryService`; valid Bearer uses JWT subject/roles and ignores spoofed `X-User-Id`; Bearer `USER sub=admin` has no admin facts. No production code/API/DTO/schema/dependency/frontend/query token/signed stream token/cookie/session change was added. | `mvn --% -Dtest=SseProductionAuthStrategyTest test`; `mvn --% -Dtest=SseProductionAuthStrategyTest,ChatControllerTest,TutorControllerTest,SecurityFilterChainTest,DevAuthFilterTest,SecurityJwtAuthenticationTest test`; `mvn test` |
| 2026-06-09 | P3-4-U Review Gate ResourceReview roles-first RBAC completed: `GET /api/reviews/resources` and `POST /api/reviews/resources/{reviewId}/decision` now derive admin/teacher facts only from `UserContext.roles()` and pass explicit facts into `ReviewGovernanceService`; Bearer admin spoofed header works, Bearer teacher no-prefix own-course review works, and `USER sub=admin/teacher_1` role-confusion is denied safely. No dependency/schema/API/frontend/formal OAuth2/JWK/Spring Security/ResourceGeneration/Agent Trace changes. | TDD RED observed; `mvn --% -Dtest=ResourceReviewControllerTest test`; `mvn --% -Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,ResourceGenerationControllerTest,DevAuthFilterTest,CurrentUserServiceTest test`; `mvn test`; integration review PASS for target code path |
| 2026-06-09 | P3-4-T Orchestrator `RESOURCE_GENERATION` create roles-first RBAC completed: `POST /api/orchestrator/workflows` and retry now derive admin/teacher facts only from `UserContext.roles()` and pass them into ResourceGeneration workflow create; `USER sub=admin` role-confusion is denied, Bearer admin cannot create for another learner, and student owner with active enrollment works despite spoofed `X-User-Id`. Forbidden create leaves no ResourceGeneration/model/token/citation side effects. No dependency/schema/API/frontend/formal OAuth2/JWK/Spring Security changes. | TDD RED observed; `mvn --% -Dtest=OrchestratorWorkflowControllerTest test`; `mvn --% -Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,LearningWorkflowControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test`; `mvn test`; integration review PASS |
| 2026-06-09 | P3-4-S LearningPath / ResourceGeneration direct create roles-first RBAC completed: `POST /api/learning-paths` and `POST /api/resources/generation-tasks` now derive admin/teacher facts only from `UserContext.roles()`; LearningPath explicit admin代创建 works despite spoofed `X-User-Id`; ResourceGeneration remains owner-only; `USER sub=admin` role-confusion is denied; forbidden ResourceGeneration direct create leaves no task/resource/review/trace/model/token side effects. No dependency/schema/API/frontend/formal OAuth2/JWK/Spring Security/Orchestrator workflow changes were added in P3-4-S; Orchestrator workflow create/retry was later completed by P3-4-T. | TDD RED observed; `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test`; `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test`; `mvn test`; integration review CONDITIONAL PASS for direct API scope |
| 2026-06-09 | P3-4-P RAG KB management roles-first RBAC completed: KnowledgeBase/Document controllers now derive admin/teacher facts from `UserContext.roles()`; `KnowledgeBaseService`, `DocumentService`, and `PermissionService` expose role-aware overloads while legacy signatures remain non-elevating; Bearer admin spoofed header and teacher no-prefix success paths work; `USER sub=admin`, `USER sub=teacher_1`, and non-admin missing document/index oracle cases are denied safely. No dependency/schema/API/frontend/retrieval runtime/formal OAuth2/JWK/Spring Security changes. | TDD RED observed; `mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest test`; `mvn --% -Dtest=PermissionServiceTest test`; `mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,ChatControllerTest,RagEvaluationControllerTest test`; `mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test`; `mvn test` |
| 2026-06-09 | P3-4-O Evaluation Set / Run roles-first RBAC completed: Evaluation Set / Run controllers now derive admin/teacher facts from `UserContext.roles()`; services no longer infer roles from `admin` or `teacher_*` subjects; Bearer admin spoofed header and teacher no-prefix success paths work; `STUDENT/USER sub=admin`, `USER sub=teacher_1`, and non-admin missing/foreign oracle cases are denied safely. No dependency/schema/frontend/formal OAuth2/JWK/Spring Security changes. | TDD RED observed; `mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test`; `mvn --% -Dtest=EvaluationSetServiceTest,EvaluationRunServiceTest test`; `mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,DevAuthFilterTest,CurrentUserServiceTest test`; `mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,PromptVersionControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test`; `mvn test` |
| 2026-06-09 | P3-4-L class analytics roles-first course scope completed: `GET /api/analytics/classes/{courseId}/summary` now uses role-derived admin/teacher flags; Bearer admin can read existing class summary despite spoofed `X-User-Id`; Bearer teacher must own the course; Bearer student is denied; non-admin missing/foreign course returns safe `FORBIDDEN`; admin missing course remains `NOT_FOUND`. No dependency/schema/frontend/formal OAuth2/JWK/Spring Security changes. | TDD RED observed; `mvn --% -Dtest=AnalyticsControllerTest test`; `mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test`; `mvn test` |
| 2026-06-09 | P3-2-I process OCR fallback provider completed: `ProcessOcrFallbackProvider` uses explicit runtime command config, `ProcessBuilder(List<String>)`, stdin bytes, stdout OCR text, stderr drain without exposure, timeout/output limits, and safe OCR statuses. No OCR SDK/native/cloud dependency, schema/API/frontend/IndexService/retrieval/citation/VectorDB changes. | `mvn --% -Dtest=ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,RealParserProviderTest test`; `mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest test`; `mvn --% dependency:tree -Dincludes=net.sourceforge.tess4j:tess4j,net.java.dev.jna:jna,org.bytedeco:*,com.aliyun:*,com.google.cloud:google-cloud-vision,software.amazon.awssdk:textract`; `mvn --% -DskipTests compile`; `mvn test` |
| 2026-06-09 | P3-2-H configurable OCR fallback provider completed: OCR selection is now controlled by `learning-os.rag.parser.ocr.enabled/provider`, defaults to disabled/noop, uses `OcrFallbackProvider` for future real providers, and keeps a single Spring-facing `OcrFallbackService`. No OCR SDK/native/cloud dependency, schema/API/frontend/IndexService/retrieval/citation/VectorDB changes. | `mvn --% -Dtest=ConfigurableOcrFallbackServiceTest,NoopOcrFallbackServiceTest,RealParserProviderTest test`; `mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest test`; `mvn --% dependency:tree -Dincludes=net.sourceforge.tess4j:tess4j,net.java.dev.jna:jna,org.bytedeco:*,com.aliyun:*,com.google.cloud:google-cloud-vision,software.amazon.awssdk:textract`; `mvn --% -DskipTests compile`; `mvn test` |
| 2026-06-09 | P3-2-G real PDF/DOCX parser SDK provider completed: Apache PDFBox `3.0.7` and Apache POI `poi-ooxml:5.5.1` are connected behind the existing `rag/parser` provider boundary; PDF text is extracted per page, DOCX heading/page-break/separator metadata is extracted through POI, and safe parser resource limits are enforced. No `IndexService`, schema/API/frontend/retrieval/citation/VectorDB/OCR changes. | `mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,NoopOcrFallbackServiceTest test`; `mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test`; `mvn --% dependency:tree -Dincludes=org.apache.pdfbox:pdfbox,org.apache.poi:poi-ooxml`; `mvn --% dependency:tree -Dincludes=commons-logging:commons-logging`; `mvn test` |
| 2026-06-09 | P3-2-E RAG parser layout/page hierarchy completed: `DocumentParserService` now splits simple PDF `/Type /Page` text into page-aware sections, splits DOCX text around same-paragraph page breaks, and treats `w:tab` / non-page `w:br` as separators. No dependency/schema/API/frontend changes. | `mvn --% -Dtest=DocumentParserServiceTest test`; `mvn --% -Dtest=IndexServiceTest#processSimpleMultiPagePdfPreservesPageNumbersInChunks test`; `mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test`; `mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test`; `mvn test` |
| 2026-06-08 | P3-3-C real model provider adapter completed: Spring AI BOM upgraded to `1.0.8`, `spring-ai-starter-model-openai` added, `AiModelGateway` calls OpenAI-compatible `ChatModel`, and `EmbeddingService` calls OpenAI-compatible `EmbeddingModel` when runtime provider configuration is complete. Default `AI_MODEL_PROVIDER=none` remains no-external-call; missing beans fail closed; provider output/errors are sanitized; unused OpenAI image/audio/moderation auto-config is disabled. | `mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest test`; `mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest,ResourceGenerationControllerTest,RagQueryServiceTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest test`; `mvn dependency:tree`; `mvn compile`; `mvn test` |
| 2026-06-08 | P3-4-J analytics student summary course scope completed: `GET /api/analytics/students/{learnerId}/summary` accepts optional `courseId`; students require own learner plus active enrollment for scoped reads; teachers require `courseId`, own course, and active enrolled learner; admins can read global or scoped summaries; scoped aggregation filters path/mastery/wrong-question signals to the course. No dependency/schema/frontend changes. | TDD RED observed; `mvn --% -Dtest=AnalyticsControllerTest test`; `mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,AssessmentControllerTest test`; `mvn test` |
| 2026-06-08 | P3-4-I real auth context / RBAC compatibility completed: `DevAuthFilter` now verifies Bearer HS256 JWTs and builds `UserContext` from `sub`, optional `name`, and roles; invalid Bearer tokens return safe `UNAUTHORIZED` without `X-User-Id` fallback; `prod` / `production` / `staging` no longer trust `X-User-Id`; `dev` / `test` keep `X-User-Id` / `dev_user` fallback only when Bearer is absent; `CurrentUserService.currentUser()` was added and role checks now prefer token roles. No dependency/schema/frontend/business-service changes. | TDD RED observed; `mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest test`; `mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,StructuredRequestLoggingFilterTest,CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest test`; `mvn test` |
| 2026-06-08 | P3-4-H RAG document course/chapter metadata scope completed: `DocumentService.upload(...)` now validates non-empty `courseId` through centralized course read/manage scope before requestId/hash/replay/storage/save/index, requires `chapterId` to belong to request `courseId`, rejects student course metadata spoofing, collapses teacher missing/foreign course to safe `FORBIDDEN`, returns `NOT_FOUND` for admin missing course, and keeps requestId metadata conflict semantics. No dependency/schema/frontend/DTO/parser/vector/model-provider changes. | TDD RED observed; `mvn --% -Dtest=DocumentControllerTest test`; `mvn --% -Dtest=DocumentControllerTest,CourseKnowledgeControllerTest,RagQueryServiceTest,IndexServiceTest test`; `mvn test` |
| 2026-06-08 | P3-4-G grading evaluation course scope completed: `POST /api/assessment/grading-evaluations` now requires `courseId`; student calls are denied first; teachers can run evaluations only for own courses; teacher missing/foreign course returns safe `FORBIDDEN`; admin missing course returns `NOT_FOUND`; sample knowledge points must belong to the request course. No dependency/schema/frontend/metric formula changes. | TDD RED observed; `mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest test`; `mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test`; `mvn test` |
| 2026-06-08 | P3-4-F assessment record list RBAC / pagination completed: `AssessmentController` now exposes answer and wrong-question list endpoints; `AssessmentService` enforces student owner-only, teacher required-course own-course active-enrollment learner, and admin global/filter list semantics; pagination is bounded; summary DTOs omit answer text, idempotency snapshots, internal payloads, `gradingResultId`, `causeAnalysis`, and `replanRecordId`. No dependency/schema/frontend changes. | TDD RED observed; `mvn --% -Dtest=AssessmentControllerTest test`; `mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test`; `mvn test` |
| 2026-06-08 | P3-4-E assessment record RBAC matrix completed: `AssessmentController` now exposes answer and wrong-question detail endpoints; `AssessmentService` enforces student owner-only, teacher own-course active-enrollment learner, and admin global read semantics; non-admin missing/foreign records return safe `FORBIDDEN` without `data`; DTOs omit request/response snapshot internals. No dependency/schema/frontend changes. | `mvn --% -Dtest=AssessmentControllerTest test`; `mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test`; `mvn test` |
| 2026-06-08 | P3-3-B model call provider observability completed: V18 adds `model_call_log.provider`, `ModelCallLog` defaults null/blank provider to `none`, `AgentRunRecorder` persists provider on success/failure model-call logs while preserving old failure signature compatibility, and `AiModelGateway` normalizes provider values to `none/openai/dashscope/anthropic/gemini/mock/other`. Sensitive/high-cardinality provider strings such as URL/`apiKey`/`sk-` collapse to `other`; `errorMessage` remains safe-code-only. No dependency/frontend/API/real provider changes. | `mvn --% -Dtest=SchemaConvergenceMigrationTest,AgentRunRecorderTest,AiModelGatewayTest,ResourceGenerationControllerTest test`; `mvn test`; MySQL smoke attempted with `mvn --% -Dtest=MysqlMigrationSmokeTest -Dlearningos.mysql.smoke=true test` but blocked by local MySQL `root` credential mismatch |
| 2026-06-08 | P3-4-C permission matrix security precondition completed: `KnowledgeCatalogService` now scopes course list/detail/knowledge graph reads by transitional admin/teacher/student role, non-admin missing/foreign course detail and graph reads return safe `FORBIDDEN` without `data`, and `GradingEvaluationService` limits grading evaluation to teacher/admin. No dependency/schema/frontend changes. | `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test`; `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test`; `mvn test` |
| 2026-06-08 | P3-2-A embedding/vector adapter boundary completed: `EmbeddingService` batch contract + disabled/enabled semantics, `VectorIndexAdapter` noop implementation, `IndexService` EMBEDDING/VECTOR_UPSERT stages before saving new chunks, chunk metadata status fields, vector hit id-only results, allowed-KB vector filtering, and `ChunkService` vector RRF hook; no dependency/schema/API changes. | `mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest test`; `mvn test` |
| 2026-06-08 | P3-2 RAG hybrid retrieval/RRF/reranker fallback completed: `ChunkService` now returns `HYBRID_RRF` retrieval results from allowed-KB keyword + recency branches, `RrfRanker` fuses and deduplicates candidates, `RerankerService` returns stable statuses and falls back on timeout/error, and `RagQueryService` writes whitelisted retrieval/hybrid/reranker metadata without raw provider errors or full chunks. No dependency/schema/frontend/API changes. | `mvn --% -Dtest=RrfRankerTest,RagQueryServiceTest test`; `mvn --% -Dtest=RagQueryServiceTest,IndexServiceTest,OrchestratorWorkflowControllerTest test`; `mvn test` |
| 2026-06-08 | P3-3-A model gateway boundary hardening completed: `AiModelGateway` validates `agent-resource-v1` structured output (`resources[]`, required fields, `safetyStatus` whitelist), maps schema failure to `STRUCTURED_OUTPUT_INVALID`, maps provider failure to `MODEL_PROVIDER_ERROR`, removes raw provider cause propagation, and `AgentRunRecorder` records success evidence from gateway response model/latency/token/cost. No dependency/schema/frontend/provider SDK changes. | `mvn --% -Dtest=AiModelGatewayTest test`; `mvn --% -Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest test`; `mvn --% -Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,AgentTraceControllerTest,AnalyticsControllerTest test`; `mvn test` |
| 2026-06-07 | P3-5 operations alerting completed: `AnalyticsController` exposes `GET /api/analytics/ops/alerts` as a temporary admin-only query endpoint; `AnalyticsService.opsAlerts(...)` aggregates `KbQueryLog`, `ModelCallLog`, and `ResourceReview` into `SLOW_RAG_QUERY`, `SLOW_MODEL_CALL`, `RAG_NO_SOURCE`, and `REVIEW_BACKLOG` alerts with default thresholds and validation. The response uses `OpsAlertSummary` / `OpsAlertThresholds` / `OpsAlertItem` whitelists and avoids prompt/question/raw response/errorMessage/markdownContent/review-private leakage. No API dependency/schema/frontend changes beyond the documented endpoint. | `mvn --% -Dtest=AnalyticsControllerTest test`; `mvn --% -Dtest=AnalyticsControllerTest,StructuredRequestLoggingFilterTest,HealthServiceTest,HealthControllerTest,RagQueryServiceTest,ResourceReviewControllerTest test`; `mvn test` |
| 2026-06-07 | P3-4-B object detail anti-enumeration completed: `LearningWorkflowService`, `ResourceGenerationService`, `AgentTraceGovernanceService`, and `DocumentService` now collapse non-admin missing/foreign object detail responses to safe `FORBIDDEN` without `data` for learning paths, resource generation tasks, agent traces, RAG documents, reindex requests, and index tasks. No API/schema/dependency changes; admin retains true `NOT_FOUND` for missing objects where needed. | `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test`; `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,DocumentControllerTest,ChatControllerTest,RagQueryServiceTest,CourseKnowledgeControllerTest test`; `mvn test` |
| 2026-06-07 | P3-4-A Course/Knowledge Catalog permission hardening completed: `KnowledgeCatalogService` enforces transitional RBAC for course/chapter/knowledge-point/dependency writes (`resolveCourseTeacherId` + `requireCourseTeacherOrAdmin`), `CurrentUserService` exposes `isAdmin()`/`isTeacherUser()`, controllers only pass `currentUserId`, students are `FORBIDDEN`, teachers are own-course only, admins are global; no API/schema/dependency change. First slice of staged `backend-architecture-completion`. | `mvn --% -Dtest=CourseKnowledgeControllerTest test`; `mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest test` |
| 2026-06-07 | P3-5 deep health checks completed: `/api/health` now reports database `DataSource` probe results, Redis `ping()` state, MinIO configuration/client construction state, and model provider disabled/configured/unconfigured state with fixed error codes and sanitized metadata. Redis host defaults to empty so optional Redis is `UNCONFIGURED` until explicitly configured. | `mvn --% -Dtest=HealthServiceTest,HealthControllerTest test`; `mvn --% -Dtest=HealthServiceTest,HealthControllerTest,StructuredRequestLoggingFilterTest test`; `mvn test` |
| 2026-06-07 | P3-5 Micrometer running metrics completed: added `LearningOsMetrics`, emitted HTTP/RAG/model/token/cost meters, exposed `/actuator/metrics`, kept tags low-cardinality and non-sensitive, and made `StructuredRequestLoggingFilter` compatible with MVC slice tests through optional collaborators. | `mvn --% -Dtest=StructuredRequestLoggingFilterTest,RagQueryServiceTest,AiModelGatewayTest,AgentRunRecorderTest test`; `mvn --% -Dtest=GlobalExceptionHandlerTest,HealthControllerTest,StructuredRequestLoggingFilterTest test`; `mvn test` |
| 2026-06-07 | P3-5 structured request logging completed: added `StructuredRequestLoggingFilter`, propagated `ErrorCode` from `GlobalExceptionHandler` through an internal request attribute, restricted `X-Trace-Id` to a safe character/length policy, and verified logs omit body/query/sensitive headers/raw exception messages. | `mvn --% -Dtest=StructuredRequestLoggingFilterTest,TraceFilterTest,GlobalExceptionHandlerTest test`; `mvn test` |
| 2026-06-07 | Review Gate course scope hardening completed for P3-4: `ReviewGovernanceService` now gates resource review list/decision by course ownership, `admin` remains global, non-admin missing/foreign review decisions return safe `FORBIDDEN`, and resource generation review tests seed matching courses for the new permission path. | `mvn --% -Dtest=ResourceReviewControllerTest#teacherCannotDistinguishMissingReviewFromForbiddenReview test`; `mvn --% -Dtest=ReviewGovernanceServiceTest,ResourceReviewControllerTest,ResourceGenerationControllerTest test`; `mvn test` |
| 2026-06-07 | RAG parser adapter minimal completed for P3-2: added `DocumentParserService`, `DocumentParser`, `ParsedDocument`, `ParsedSection`, and `DocumentParseException`; `IndexService` now consumes the parser boundary and maps parser failures to `DOCUMENT_PARSE_FAILED`; no dependency, API, or schema changes. | `mvn --% -Dtest=DocumentParserServiceTest test`; `mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest test`; `mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test`; `mvn test` |
| 2026-06-06 | RAG index worker progress completed and review-hardened for P3-2: V16 adds progress/phase/heartbeat/lease/retry fields, `IndexTaskWorkerScheduler` auto-consumes due `PENDING` tasks with per-task failure isolation, `IndexService` commits stage progress independently, `IndexTaskRecoveryScheduler` uses `leaseUntil < now` plus worker retry/backoff for expired leases, and `GET /api/index-tasks/{taskId}` exposes permission-checked safe task status. | `mvn --% -Dtest=IndexTaskRecoverySchedulerTest test`; `mvn --% -Dtest=IndexTaskRecoverySchedulerTest,IndexTaskWorkerSchedulerTest,IndexServiceTest test`; `mvn --% -Dtest=SchemaConvergenceMigrationTest,IndexServiceTest,IndexTaskWorkerSchedulerTest,IndexTaskRecoverySchedulerTest,DocumentControllerTest test`; `mvn --% -Dtest=MysqlMigrationSmokeTest test`; `mvn test`; `powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?...'` |
| 2026-06-06 | RAG chunk production metadata completed for P3-2: `IndexService` now uses token-window chunking with 40-token overlap, stable SHA-256 `chunkHash`, Markdown heading hierarchy, and structured short metadata, and V17 adds the `kb_doc_chunk.chunk_hash` schema. | `mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test`; `mvn test`; `powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?...'` |
| 2026-06-06 | P3-1 MySQL migration smoke completed and later extended through V17: `MysqlMigrationSmokeTest` runs current Flyway migrations when explicitly enabled, `scripts/mysql-migration-smoke.ps1` invokes the latest method, V1 `kb_query_log` large payload columns are `text` to avoid MySQL row-size overflow, and H2/Flyway-disabled test profile differences are documented. | `mvn --% -Dtest=MysqlMigrationSmokeTest test`; `mvn --% -Dtest=SchemaConvergenceMigrationTest test`; `powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?...'`; `mvn test` |
| 2026-06-06 | Prompt Version quality comparison completed for TODO P2-1: `evaluation_run` / `evaluation_run_metric` persist prompt-version quality runs, comparison uses only `SUCCEEDED` runs, aggregates by metric sample count, rejects duplicate metric names, and V14 enforces sample-count constraints. | `cd backend; mvn "-Dtest=EvaluationRunServiceTest,EvaluationRunControllerTest,SchemaConvergenceMigrationTest" test`; `cd backend; mvn "-Dtest=EvaluationRunServiceTest,EvaluationRunControllerTest,EvaluationSetServiceTest,EvaluationSetControllerTest,PromptVersionServiceTest,PromptVersionControllerTest,RagEvaluationServiceTest,RagEvaluationControllerTest,GradingEvaluationServiceTest,AssessmentControllerTest#exposesAutomatedGradingQualityEvaluationReport,SchemaConvergenceMigrationTest" test` |
| 2026-06-06 | RAG quality evaluation completed for TODO P2-2: `RagEvaluationRequest` supports benchmark samples and `RagEvaluationResult` returns Recall@K, Citation Accuracy, Groundedness, No-source Refusal Rate, benchmark summary, sample results, and report text. | `cd backend; mvn "-Dtest=RagEvaluationServiceTest,RagEvaluationControllerTest" test` |
| 2026-06-06 | RAG evaluation archive reporting verified for TODO P2-2: the archive script generated a Markdown report under `backend/target/rag-evaluation-archive/latest/`. | `powershell -ExecutionPolicy Bypass -File .\scripts\run-rag-evaluation-archive.ps1` |
| 2026-06-06 | Token / Cost budget governance completed for TODO P2-4 analytics scope: `/api/analytics/token-budget/governance` returns cost stats by user/course/Agent type/time window, budget decisions, high-cost task warnings, and abnormal model-call detection. | `cd backend; mvn "-Dtest=AnalyticsControllerTest" test` |
| 2026-06-06 | Automated grading quality evaluation completed for TODO P2-3: `GradingEvaluationService` supports structured samples, MAE, grade agreement, wrong-cause agreement, and grouped analysis by question type, knowledge point, and rubric version. | `cd backend; mvn "-Dtest=GradingEvaluationServiceTest,AssessmentControllerTest#exposesAutomatedGradingQualityEvaluationReport" test` |
| 2026-06-06 | Agent Trace governance backend completed for TODO P2-5: `/api/agent/traces` filters trace records, `AgentRunRecorder.recordToolCall(...)` persists sanitized tool calls, detail returns `toolCalls` and retention policy, and V15 adds tool-call governance fields. | `cd backend; mvn "-Dtest=AgentTraceControllerTest,AgentRunRecorderTest" test` |
| 2026-06-06 | Evaluation Set management completed for TODO P2-1: `evaluation_set` and `evaluation_sample` now persist RAG question sets, grading sample sets, and resource generation sample sets; management APIs enforce teacher/admin access and course ownership where applicable. | `cd backend; mvn "-Dtest=EvaluationSetServiceTest,EvaluationSetControllerTest,SchemaConvergenceMigrationTest" test`; `cd backend; mvn "-Dtest=EvaluationSetServiceTest,EvaluationSetControllerTest,PromptVersionServiceTest,PromptVersionControllerTest,RagEvaluationServiceTest,RagEvaluationControllerTest,GradingEvaluationServiceTest,AssessmentControllerTest#exposesAutomatedGradingQualityEvaluationReport,SchemaConvergenceMigrationTest" test` |
| 2026-06-06 | Model call prompt metadata completed for P2-1: `model_call_log` now stores `promptCode`, `promptVersion`, `temperature`, and a whitelist structured output schema; model provider errors are persisted and exposed through trace/task evidence as `MODEL_PROVIDER_ERROR` instead of raw provider messages. | `cd backend; mvn "-Dtest=AgentRunRecorderTest,SchemaConvergenceMigrationTest" test`; `cd backend; mvn "-Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest,SchemaConvergenceMigrationTest,PromptVersionServiceTest,PromptVersionControllerTest,AiModelGatewayTest" test` |
| 2026-06-06 | Resource generation recovery state completed for P0-3: `resource_generation_task` now persists `retryCount`, `nextRetryAt`, `lastError`, and `recoverable`; recoverable model failures write safe `MODEL_CALL_FAILED` and expose recovery metadata through resource generation responses. | `cd backend; mvn "-Dtest=ResourceGenerationControllerTest#persistsRetryMetadataWhenResourceGenerationFailsRecoverably,SchemaConvergenceMigrationTest#v11MigrationAddsRecoveryStateColumnsToResourceGenerationTask" test`; `cd backend; mvn "-Dtest=ResourceGenerationControllerTest,SchemaConvergenceMigrationTest" test` |
| 2026-06-06 | Resource citation governance completed for P1-4: resource generation now writes task-level `source_citation` records by `traceId`, generated resources distinguish `COURSE_RAG` and `NO_SOURCE`, Critic reviews save `citationCheck`, and `NO_SOURCE` resources cannot be directly approved or released to learners. | `cd backend; mvn "-Dtest=ResourceGenerationControllerTest#resourceGenerationPersistsSourceCitationsAndCriticCitationCheck+noSourceGeneratedResourcesRequireReviewAndRejectApproval" test`; `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest,RagQueryServiceTest,SchemaConvergenceMigrationTest" test` |
| 2026-06-06 | Orchestrator node contract policy completed for P0-1: workflow steps and recent failed steps now expose `inputDto`, `outputDto`, `failurePolicy`, `retryPolicy`, and `retryable`; failed RAG/answer workflows return `RESUBMIT_ORIGINAL_REQUEST`, while failed resource generation keeps endpoint retry. | `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest" test`; `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest,RagQueryServiceTest,SchemaConvergenceMigrationTest" test` |
| 2026-06-06 | Learner profile snapshot context added for P1-1: profile extraction returns seven explicit dimensions with `lastEvidenceId`, path/resource generation persist `profileSnapshot`, ResourceAgent payload includes the snapshot, V10 adds snapshot columns, and historical profile aliases are preserved. | `cd backend; mvn "-Dtest=LearningWorkflowControllerTest,LearningWorkflowServiceTest,ResourceGenerationControllerTest,SchemaConvergenceMigrationTest,OrchestratorWorkflowControllerTest" test` |
| 2026-06-05 | Analytics module added `GET /api/analytics/students/{learnerId}/summary`, overview `agentSummary`, and token aggregations by user and agent name. | `cd backend; mvn "-Dtest=AnalyticsControllerTest" test` |
| 2026-06-05 | Assessment loop now reads latest `mastery_record` before grading feedback and applies BKT-lite dynamic mastery updates instead of fixed demo values. | `cd backend; mvn "-Dtest=AssessmentFeedbackServiceTest,AssessmentControllerTest" test` |
| 2026-06-06 | Answer submission idempotency added with required `requestId`, `answer_record` response snapshots, `(learner_id, request_id)` unique index, replay/409 handling, and concurrent unique-key replay coverage. | `cd backend; mvn test` |
| 2026-06-06 | Agent state machine convergence added: task/trace status validation, guarded transitions, cooperative cancellation, `WAITING_REVIEW` after resource draft generation, `DONE` after full review approval, and persisted `FAILED` evidence for model-generation failure. | `cd backend; mvn test` |
| 2026-06-06 | Review Gate response hardening added: unreviewed resource generation create/detail/replay responses omit `markdownContent`; approved resources return content after all reviews pass. | `cd backend; mvn "-Dtest=ResourceGenerationControllerTest" test`; `cd backend; mvn test` |
| 2026-06-06 | Orchestrator `RESOURCE_GENERATION` context unified: resource generation can run inside an existing workflow `AgentExecutionContext`, trace steps append after `workflow_start`, and model failure keeps queryable workflow evidence. | `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest,AgentRunRecorderTest,ResourceGenerationControllerTest" test`; `cd backend; mvn test` |
| 2026-06-06 | RAG index active dedup added: `IndexService.createPendingTask(...)` reuses latest `PENDING/RUNNING` index task and creates a new `PENDING` task only after `FAILED/SUCCEEDED`. | `cd backend; mvn "-Dtest=DocumentControllerTest" test`; `cd backend; mvn test` |
| 2026-06-06 | RAG index timeout recovery added: `IndexService.recoverTimedOutRunningTasks(...)` marks timed-out `RUNNING` tasks as `FAILED`, increments `retryCount`, records `errorMessage/finishedAt`, and recovered documents can be reindexed with a new task. | `cd backend; mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test`; `cd backend; mvn test` |
| 2026-06-06 | Orchestrator `RAG_QA` context unified: RAG query now runs inside the workflow `AgentExecutionContext`, reuses Orchestrator traceId for `kb_query_log` and `source_citation`, appends RAG trace steps, and keeps invalid payload from creating `agent_task`. | `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest" test`; `cd backend; mvn test` |
| 2026-06-06 | Orchestrator `ANSWER_SUBMISSION` context unified and review-hardened: answer submission now runs inside the workflow `AgentExecutionContext`, reuses Orchestrator traceId across assessment rows, appends assessment trace steps, validates `requestId` in service layer, exact-matches replay workflow envelopes, cleans transient loser task/trace on trace drift, and rejects invalid/conflicting payloads before creating durable workflow state. | `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest,AssessmentServiceTest" test`; `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest,AssessmentControllerTest,AssessmentServiceTest" test`; `cd backend; mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test`; `cd backend; mvn test` |
| 2026-06-06 | Review Gate state model hardened: review decisions now support `REJECTED`, `resource_review` stores structured audit fields, all-approved tasks/resources move to `PUBLISHED`, and learner release requires `PUBLISHED` plus all reviews approved. | `cd backend; mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,SchemaConvergenceMigrationTest" test`; `cd backend; mvn "-Dtest=ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest" test`; `cd backend; mvn test` |
| 2026-06-06 | Orchestrator runtime failure evidence added: task-created `RAG_QA` runtime `ApiException` persists `FAILED` agent task and `step_runtime_failure`, keeps safe error-code-only evidence, preserves original HTTP error code, and avoids query/citation success artifacts. | `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest#persistsRagQaRuntimeFailureEvidenceWithoutWritingQueryArtifacts" test`; `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest" test`; `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest,AssessmentControllerTest,AssessmentServiceTest" test`; `cd backend; mvn test` |
| 2026-06-06 | RAG query replay snapshot added: `kb_query_log` now stores `requestId/requestHash/responseJson`, `RAG_QA` same-payload replay returns the first workflow without duplicate task/query/citation rows, conflicting payloads return 409, and RAG workflow input snapshots are sanitized. | `cd backend; mvn "-Dtest=SchemaConvergenceMigrationTest,RagQueryServiceTest,OrchestratorWorkflowControllerTest" test`; `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest,AssessmentControllerTest,AssessmentServiceTest" test`; `cd backend; mvn test` |
| 2026-06-06 | RAG document upload idempotency added: optional multipart `requestId`, `kb_document` request snapshots, unique key `(created_by, request_id)`, replay for matching uploads, 409 for payload conflicts, and duplicate document/index-task prevention. | `cd backend; mvn "-Dtest=SchemaConvergenceMigrationTest,DocumentControllerTest" test`; `cd backend; mvn "-Dtest=SchemaConvergenceMigrationTest,DocumentControllerTest,IndexServiceTest,IndexTaskRecoverySchedulerTest" test` |
| 2026-06-06 | RAG index recovery scheduler and lock added: `createPendingTask(...)` locks `kb_document` before active-task reuse/create, and startup/fixed-delay recovery calls `recoverTimedOutRunningTasks(...)` with configurable timeout. | `cd backend; mvn "-Dtest=IndexServiceTest,IndexTaskRecoverySchedulerTest" test`; `cd backend; mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test` |
| 2026-06-06 | Orchestrator failure retry policy added: generic runtime exceptions persist safe `FAILED` workflow evidence, `POST /api/orchestrator/workflows/{workflowId}/retry` supports owner retry for `FAILED RESOURCE_GENERATION`, and retry lineage is exposed as `retryOfWorkflowId`. | `cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest" test` |
| 2026-06-06 | Review Gate authorization hardening added: review list/decision APIs deny students before loading details and allow temporary `teacher/admin` reviewers. | `cd backend; mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest" test` |
| 2026-06-06 | Teacher class analytics summary added: `GET /api/analytics/classes/{courseId}/summary` returns course weak knowledge points, wrong-cause distribution, resource completion, and pending review metadata with teacher/admin access checks. | `cd backend; mvn "-Dtest=AnalyticsControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest" test` |
| 2026-06-06 | Knowledge DAG dependency types added for P1-2: dependency creation validates `PREREQUISITE` / `RELATED` / `ADVANCED`, and learning path planning only uses `PREREQUISITE` as prerequisite lock/order edges. | `cd backend; mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest" test` |
| 2026-06-06 | Knowledge DAG mastery remediation priority added for P1-2: ready prerequisite nodes below mastery `0.6` and with downstream dependents are prioritized before ordinary ready nodes while keeping existing `DONE/ACTIVE/LOCKED` statuses. | `cd backend; mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest" test` |
| 2026-06-06 | Learning path node recommendation metadata added for P1-2: path nodes now persist and return recommendation reason, estimated duration minutes, resource type, and assessment binding relation. | `cd backend; mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowServiceTest,LearningWorkflowControllerTest,SchemaConvergenceMigrationTest" test` |
| 2026-06-06 | P3-4 minimum permission/security hardening added: profile/path owner checks, analytics overview admin-only, health redaction, strict RAG `kbIds`, and `GET /api/rag/query` handler coverage. Full production RBAC and resource/answer permission matrix remain open. | `cd backend; mvn "-Dtest=LearningWorkflowControllerTest,LearningWorkflowServiceTest,AnalyticsControllerTest,HealthControllerTest,ChatControllerTest,RagQueryServiceTest" test`; `cd backend; mvn test` |
