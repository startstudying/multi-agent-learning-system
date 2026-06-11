# Changelog

All notable changes to this project are documented here.

## [Unreleased]

- Epic 后续增强（F1–F7）全部落地：
  - Admin 前端 `/admin/model-providers` 供应商配置页；Embedding registry 路由；Token 预算调用前门禁；Ops 告警持久化 + webhook opt-in + Admin 真实告警区；Qdrant health/dimension probe；PDF parser layout 元数据增强；权限矩阵回归测试；external-smoke opt-in 测试。
  - 迁移 V22 `ops_alert_record`；配置项 `learning-os.token-budget` / `learning-os.ops-alert` / `rag.vector.qdrant.expected-dimension`。
  - 验证：backend `mvn test` 通过；frontend `31 passed`。
  - 证据：`docs/evidence/EVIDENCE-20260611-backend-followup-enhancements-epic.md`

- F1 子任务 model provider registry backend:
  - Added Flyway V21 `model_provider` table and admin-only `/api/admin/model-providers` CRUD/set-default/test-connection APIs.
  - API keys are AES-GCM encrypted at rest; responses expose only `apiKeyMasked`, never full secrets.
  - `AiModelGateway` now prefers enabled default registry provider (`deepseek` / `mimo` / `dashscope` / `openai` / `custom`) over env-only OpenAI-compatible config.
  - Verification: focused provider tests passed; full backend `612 run, 0 failures, 0 errors, 1 skipped`.
  - Follow-up: Admin frontend page, embedding path registry integration, external provider smoke.

- 后端架构 TODO 计划收口（MVP / 最小可答辩闭环）：
  - 将 `docs/planning/backend-architecture-todolist.md` 标记为 Completed（2026-06-11）。
  - P0、P1、P2、P3-1、P3-2（最小生产化）、P3-3（最小 provider 边界）、P3-4（MVP 权限矩阵）、P3-5 主计划项全部 [x]。
  - P3-4 三个残余 unchecked 项在 MVP 边界内标记完成；工业级 parser/OCR、Qdrant 真实 smoke、DashScope 专用 provider、持续权限矩阵抽样移入“后续增强”区。
  - 验证：`backend` full Maven test `601 run, 0 failures, 0 errors, 1 skipped`。
  - 证据：`docs/evidence/EVIDENCE-20260611-backend-architecture-todolist-completion.md`

- P3-4 子任务 formal production streaming design:
  - Added `POST /api/rag/query/stream` as the production/staging student RAG streaming endpoint.
  - The endpoint returns `text/event-stream` and emits `status` / `token` / `done` / safe `error` events while reusing `RagQueryService`.
  - Request body carries `question`, `kbIds`, `topK`, and optional `requestId`; no query token or signed stream URL was added.
  - Production/staging frontend now uses `fetch` / `ReadableStream` through `streamRequest(...)` and `streamRagQuery(...)`; native `EventSource` remains only for dev/test legacy demo flow.
  - Added frontend coverage proving production/staging calls `/api/rag/query/stream`, creates no `EventSource`, does not put `question` / `kbIds` in the stream URL, and does not retry through legacy SSE on stream failure.
  - Extended `SseProductionAuthStrategyTest` for the new POST stream path: missing/invalid Bearer fails before async work, spoofed `X-User-Id` is ignored under valid Bearer, and `USER sub=admin` does not infer admin.
  - Verification passed: adjacent backend `36 run, 0 failures`; full backend `601 run, 0 failures, 0 errors, 1 skipped`; frontend focused `31 passed`; `npx vue-tsc -b --noEmit` passed; `npm run build` passed.
  - Accepted limitations: this is transport streaming, not true model token streaming; frontend Bearer token source is a wrapper hook pending login integration; legacy GET SSE remains dev/test/demo compatible.
  - P3-4 parent remains open for broader business-matrix sampling and other teacher-side data-scope expansion.

- P3-4 子任务 orchestrator answer submission replay scope revalidation:
  - Fixed stale authorization replay for Orchestrator `ANSWER_SUBMISSION` workflows.
  - Added RED/GREEN MockMvc regression `OrchestratorWorkflowControllerTest.answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow`.
  - `AssessmentService.replayAnswerIfPresent(...)` now revalidates `questionId -> KnowledgePoint.courseId -> ACTIVE enrollment` before returning an existing answer snapshot.
  - A learner whose enrollment changed from `ACTIVE` to `DROPPED` now receives safe `FORBIDDEN` on same-payload Orchestrator replay instead of the old workflow envelope.
  - Forbidden replay does not add answer/grading/mastery/wrong-question/learning-event rows and does not return old workflow/task/trace metadata.
  - No REST API contract, DTO, DB schema, dependency, frontend, or auth framework change was added.
  - Verification passed: RED observed `1 failure`; focused `1 run, 0 failures`; adjacent `93 run, 0 failures`; full backend `596 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for broader business-matrix sampling and teacher-side data-scope expansion.

- P3-4 子任务 teacher permission residual sampling matrix:
  - Added MockMvc residual permission-matrix tests for Bearer teacher KB list, foreign course-bound document reindex denial, and teacher class summary pending review redaction.
  - Pinned Bearer `TEACHER` KB list behavior: spoofed `X-User-Id: admin` is ignored and foreign course-bound KBs are redacted even when visibility is `PUBLIC`.
  - Pinned Bearer `TEACHER` document reindex behavior: foreign course-bound documents return safe `FORBIDDEN`, do not leak document/index task/course metadata, and do not create new index tasks.
  - Pinned teacher class summary behavior: `pendingReviews` contains only the requested course reviews and does not leak foreign review/resource/task/course/title or `markdownContent`.
  - No production code, REST API contract, DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
  - Verification passed: focused `71 run, 0 failures`; adjacent `172 run, 0 failures`; full backend `595 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open; Orchestrator `ANSWER_SUBMISSION` replay scope revalidation is tracked as a separate completed semantic child task above.

- P3-2 子任务 real VectorDB adapter minimum integration:
  - Added a real Qdrant-backed `VectorIndexAdapter` minimum implementation behind the project-owned adapter port.
  - Added `RagVectorProperties`, Qdrant operations/command/point/hit types, and conditional Qdrant configuration under `learning-os.rag.vector.*`.
  - Default runtime remains Noop and does not connect to Qdrant unless `learning-os.rag.vector.enabled=true` and `provider=qdrant` are explicitly configured.
  - Fixed Noop fallback bean registration by keeping `NoopVectorIndexAdapter` as a plain implementation and letting `QdrantVectorConfiguration` own the `@ConditionalOnMissingBean(VectorIndexAdapter.class)` fallback bean.
  - Qdrant upsert payload is limited to low-sensitivity chunk metadata; raw chunk content, questions, prompts, storage keys, user ids, secrets, and raw vector values are not exposed through payload/string output.
  - Qdrant native upsert uses `setVectors(...)` plus `putAllPayload(...)`; search requests only `chunkId` payload via `setWithPayload(...)` and disables vector return via `setWithVectors(...enable=false)` so retrieval can safely re-load chunks and re-apply allowed-KB filtering.
  - Verification passed: focused `6 run, 0 failures`; adjacent `28 run, 0 failures`; full backend `592 run, 0 failures, 0 errors, 1 skipped`; dependency tree recorded `spring-ai-qdrant-store:1.0.8 -> io.qdrant:client:1.13.0 -> grpc-netty-shaded:1.65.1` and `grpc-api:1.75.0`.
  - Accepted limitation: real Qdrant service smoke, collection dimension validation, health/ops integration, dependency-check/CVE scanning, and gRPC/Netty dependency upgrade/risk acceptance remain follow-up work.

- P3-2 子任务 Vector embedding payload contract:
  - Added internal `EmbeddingVector` and `QueryEmbeddingResult` contracts.
  - `EmbeddingBatchResult` now carries chunk-id-aligned vectors on successful document embedding.
  - `EmbeddingService` now exposes query embedding for vector search without making adapters embed raw questions.
  - `VectorChunkReference` can carry in-memory vectors while `toString()` hides raw float values.
  - `VectorSearchRequest` now carries a query vector and no longer carries the raw question.
  - `IndexService` passes embedding vectors into `VectorUpsertRequest`; `ChunkService` falls back to keyword/recency/RRF when query embedding fails.
  - No Maven dependency, VectorDB provider/config, API/DTO, DB migration, frontend, parser/OCR, or Agent/Orchestrator change was added.
  - Verification passed: RED compile failure observed; focused `13 run, 0 failures`; adjacent `49 run, 0 failures`; full backend `586 run, 0 failures, 0 errors, 1 skipped`.
  - Accepted limitation: this entry only covered the internal payload contract; it was later consumed by the P3-2 real VectorDB adapter minimum integration.

- P3-2 子任务 DOCX table/TOC reading-order provider:
  - DOCX POI provider now walks `XWPFDocument#getBodyElements()` so paragraphs and tables keep document body order.
  - Lightweight DOCX fallback now reads `word/document.xml` body order for `<w:p>` and `<w:tbl>` instead of flattening table cells into ordinary text.
  - DOCX tables are emitted as independent parsed sections and indexed chunks with `contentKind=TABLE_TEXT`.
  - TOC-like DOCX paragraphs are skipped by default to avoid table-of-contents pollution in RAG retrieval.
  - No Maven dependency, API/DTO, DB migration, frontend, VectorDB, retrieval, or citation contract changes were added.
  - Verification passed: RED `46 run, 3 failures`; focused `46 run, 0 failures`; adjacent `58 run, 0 failures`; full backend `582 run, 0 failures, 0 errors, 1 skipped`.
  - Accepted limitation: P3-2 parent remains open for PDF layout/table/TOC provider, native/cloud OCR, provider confidence pipeline, true rendered page numbers, and real VectorDB.

- P3-2 子任务 industrial parser reading-order/confidence/page-number metadata:
  - Added parser metadata contract fields: `pageNumSource`, `readingOrderIndex`, `layoutConfidence`, `ocrConfidence`, and `contentKind`.
  - `ParsedSection` and `OcrFallbackResult` keep backward-compatible constructors while supporting the new metadata.
  - `ParsedDocument` now fills stable 1-based `readingOrderIndex` for sections that do not provide one.
  - OCR fallback success now produces `pageNumSource=OCR_FALLBACK`, `contentKind=OCR_TEXT`, and optional `ocrConfidence`.
  - `IndexService` writes safe short parser metadata into chunk `metadataJson` and does not write raw OCR text or provider responses.
  - No Maven dependency, API/DTO, DB migration, frontend, VectorDB, retrieval, or citation contract changes were added.
  - Verification passed: RED `testCompile` contract failure observed; focused `43 run, 0 failures`; adjacent `55 run, 0 failures`; full backend rerun `579 run, 0 failures, 0 errors, 1 skipped`.
  - Accepted limitation: this is the metadata contract foundation only; industrial PDF/DOCX layout/table/TOC, native/cloud OCR providers, provider confidence pipeline, and true rendered page labels remain future P3-2 slices.

- P3-4 子任务 frontend SSE sensitive URL cleanup:
  - Production/staging student RAG chat now uses the existing `POST /api/rag/query` path instead of native `EventSource` GET URLs carrying `question` / `kbIds`.
  - Production/staging REST failures now surface directly without retrying the same POST through the old SSE fallback path.
  - Dev/test SSE streaming behavior remains intact for the existing demo and regression tests.
  - Added production/staging Vitest coverage proving no `EventSource` is created and POST `/api/rag/query` carries `kbIds`, `question`, and `topK` in the JSON body.
  - Verification passed: RED `1 failed | 28 passed`; duplicate-POST RED `1 failed | 30 passed`; focused frontend `31 passed`; `npx vue-tsc -b --noEmit` passed.
  - Accepted limitation: this is the S fallback cleanup; true production streaming with Bearer and no query URL remains a future M slice.

- P3-4 子任务 Assessment submit foreign-questionId:
  - Fixed `POST /api/assessment/answers` foreign course `questionId` authorization.
  - Added a RED/GREEN regression proving Bearer `USER sub=alice` with spoofed headers cannot submit a `questionId` from another learner's course.
  - `AssessmentService.submitAnswerWithTraceId(...)` now checks `questionId -> KnowledgePoint.courseId -> CourseAccessService.requireCourseRead(learnerId, false, false, courseId)` before idempotency replay, content-safety, transaction start, or durable side effects.
  - Preserved legacy/template question id compatibility when no existing `KnowledgePoint` can be resolved.
  - Verification passed: RED `49 run, 1 failure`; focused `60 run, 0 failures`; adjacent `92 run, 0 failures`; full backend `578 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for formal production streaming client design and industrial parser follow-up.

- P3-4 子任务 Evaluation/Review forged-id object-oracle matrix:
  - Added MockMvc tests for Evaluation Run forged `evaluationSetId` and Resource Review missing-vs-foreign object oracle behavior.
  - Pinned Bearer `TEACHER` with spoofed `X-User-Id: admin` denial for foreign/missing evaluation set run creation without run/metric side effects.
  - Pinned Bearer `TEACHER` review decision behavior: missing and foreign reviews both return safe `FORBIDDEN` without review/task/resource/request leakage and without mutating review/resource/task state.
  - No production code, API contract, DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
  - Verification passed: focused `26 run, 0 failures`; adjacent `41 run, 0 failures`; full backend `578 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for formal production streaming client design and industrial parser follow-up.

- P3-4 子任务 dev/test legacy fallback cleanup:
  - Tightened `CurrentUserService` legacy subject-name inference so Spring Security JWT identities in dev/test no longer gain admin/teacher semantics from `sub=admin` or `sub=teacher_*`.
  - Preserved dev/test no-Bearer `X-User-Id` header-only fallback compatibility.
  - Added `SecurityJwtAuthenticationTest` regressions for JWT `roles=["USER"]` subject-name role-confusion.
  - Verification passed: RED `6 run, 1 failure`; focused `6 run, 0 failures`; adjacent auth `38 run, 0 failures`; full backend `578 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for formal production streaming client design and industrial parser follow-up.

- P3-4 子任务 Course/Knowledge forged business-object HTTP matrix:
  - Added MockMvc permission-matrix tests for Course/Knowledge forged business-object write paths.
  - Pinned Bearer `TEACHER` behavior: spoofed `X-User-Id: admin` cannot combine own `courseId` with a foreign `chapterId` to create a knowledge point.
  - Pinned Bearer `ADMIN` behavior: admin can manage courses globally but still cannot bypass chapter-course consistency.
  - Pinned cross-course dependency behavior: spoofed-header Bearer teacher cannot combine a target `knowledgePointId` with a foreign-course `prerequisiteId`.
  - Pinned safe rejection behavior: `VALIDATION_ERROR` responses contain no `data` and do not leak forged ids, course/chapter/knowledge titles, or request titles.
  - Pinned no-side-effect behavior: rejected requests do not create `KnowledgePoint` or `KnowledgeDependency` rows.
  - No production code, REST API contract, DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
  - Verification passed: focused `29 run, 0 failures`; adjacent `35 run, 0 failures`; full backend `574 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for frontend production streaming client / sensitive SSE URL cleanup and industrial parser follow-up.

- P3-4 子任务 PromptVersion forged-id object-oracle guards:
  - Added MockMvc permission-matrix tests for PromptVersion missing detail, Bearer spoofed-header behavior, and subject-name role-confusion.
  - Pinned Bearer `ADMIN` missing detail behavior: spoofed `X-User-Id` is ignored and authorized missing reads return `NOT_FOUND` without `data`.
  - Pinned Bearer `TEACHER` metadata-reader missing detail behavior: returns `NOT_FOUND` without `promptText` or `data`.
  - Pinned Bearer `STUDENT` missing detail denial: returns `FORBIDDEN` without leaking forged code/version or not-found oracle text.
  - Pinned Bearer `USER sub=admin` list/detail denial so subject-name role-confusion cannot read PromptVersion management data.
  - No production code, REST API contract, DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
  - Verification passed: focused `13 run, 0 failures`; adjacent `20 run, 0 failures`; full backend `572 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for frontend production streaming client / sensitive SSE URL cleanup and industrial parser follow-up.

- P3-4 子任务 Orchestrator workflow forged-id object-oracle guards:
  - Added MockMvc permission-matrix tests for forged `workflowId` status and retry paths.
  - Pinned non-owner Bearer workflow status behavior: spoofed `X-User-Id` cannot query another user's workflow and the `NOT_FOUND` response does not leak workflow/task/trace/request metadata.
  - Pinned non-owner Bearer workflow retry behavior: spoofed `X-User-Id` cannot retry another user's failed `RESOURCE_GENERATION` workflow.
  - Pinned forbidden retry side-effect boundary: no new AgentTask, AgentTrace, ResourceGenerationTask, LearningResource, ResourceReview, ModelCallLog, TokenUsageLog, or SourceCitation rows.
  - No production code, REST API contract, DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
  - Verification passed: focused `32 run, 0 failures`; adjacent `73 run, 0 failures`; full backend `572 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for frontend production streaming client / sensitive SSE URL cleanup and industrial parser follow-up.

- P3-4 子任务 Course/class/resource broader business permission matrix:
  - Added MockMvc permission-matrix tests for Bearer teacher/student course lists, course-bound resource generation owner enrollment state, and teacher student-summary dropped-enrollment denial.
  - Pinned Bearer `TEACHER` course list behavior: spoofed `X-User-Id: admin` is ignored and only token-subject owned courses are returned.
  - Pinned Bearer `STUDENT` course list behavior: spoofed `X-User-Id: teacher_*` is ignored and only ACTIVE enrollment courses are returned; DROPPED enrollment courses are redacted.
  - Pinned course-bound resource generation create behavior: Bearer owner with DROPPED or no enrollment is denied without ResourceGenerationTask / LearningResource / Review / AgentTask / AgentTrace / ModelCall / TokenUsage / SourceCitation side effects.
  - Pinned analytics student-summary behavior: Bearer teacher cannot read a DROPPED learner in own course and the forbidden response does not leak course, knowledge point, path, wrong-cause, or resource-task signals.
  - No production code, REST API contract, DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
  - Verification passed: focused `117 run, 0 failures`; adjacent `143 run, 0 failures`; full backend `566 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for dev/test legacy fallback cleanup, frontend production streaming client / sensitive SSE URL cleanup, and broader forged-id / business-object penetration matrix.

- P3-4 子任务 Agent task cancel / course create role-confusion residual matrix:
  - Added MockMvc residual permission-matrix tests for `POST /api/agent/tasks/{taskId}/cancel` and `POST /api/courses`.
  - Pinned cancel owner-only semantics under Bearer auth: owner succeeds despite spoofed `X-User-Id`; Bearer `USER sub=admin`, Bearer `USER sub=teacher_*`, and non-owner spoofed owner header cannot cancel foreign tasks and leave task/trace state unchanged.
  - Pinned course create roles-first semantics: Bearer `USER sub=admin` cannot create a course or persist side effects; Bearer `TEACHER` with spoofed admin header creates only for the token subject and cannot use the spoofed header for elevation.
  - No production code, REST API contract, DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
  - Verification passed: focused `57 run, 0 failures`; adjacent `80 run, 0 failures`; full backend `561 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for dev/test legacy fallback cleanup, frontend production streaming client / sensitive SSE URL cleanup, and broader `course/class/resource` business permission matrix.

- P3-4 子任务 Resource/course permission matrix expansion:
  - Added MockMvc permission-matrix tests for `learner-resources`, course-bound resource generation create, course knowledge dependency write, analytics student summary, and resource review list.
  - Pinned Bearer owner priority over spoofed `X-User-Id`, learner-only released resource reads, Bearer admin denial on foreign learner resources, and student missing/foreign anti-enumeration.
  - Pinned Bearer teacher own-course denial for student resource generation create without side effects, Bearer `USER sub=teacher_*` dependency-write role-confusion denial, and teacher no-prefix review list redaction to own-course reviews.
  - No production code, REST API contract, DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
  - Verification passed: focused `106 run, 0 failures`; adjacent `139 run, 0 failures`; full backend `555 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for dev/test legacy fallback cleanup, frontend production streaming client / sensitive SSE URL cleanup, `agent task cancel` / course create residual matrix, and broader business permission matrix.

- P3-4 子任务 Answer record RBAC penetration matrix expansion:
  - Added MockMvc penetration tests for answer/wrong-question Bearer admin/student/teacher role handling, spoofed `X-User-Id`, and subject-name role-confusion denial.
  - Pinned wrong-question list Bearer teacher no-prefix success and `USER sub=admin` / `USER sub=teacher_*` denial.
  - Pinned student Bearer owner-only answer/wrong-question list/detail behavior even when spoofed admin headers are present.
  - Pinned teacher detail denial when the target learner is no longer actively enrolled in the teacher-owned course.
  - No production code, REST API contract, DTO, DB schema, dependency, frontend, Agent/RAG runtime, or Spring Security change was added.
  - Verification passed: focused `48 run, 0 failures`; adjacent `65 run, 0 failures`; full backend `547 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for broader course/class/resource matrix, dev/test legacy fallback cleanup, and frontend production streaming client / sensitive SSE URL cleanup.

- P3-4 子任务 Service legacy subject-name authorization cleanup:
  - Removed legacy subject-name authorization overloads from `KnowledgeCatalogService`, `AssessmentService`, and `GradingEvaluationService`.
  - Removed target `isAdmin(String)` / `isTeacherUser(String)` helper paths and related `admin` / `teacher_*` role inference helpers from those services.
  - `KnowledgeCatalogService` course create/read/list/chapter/knowledge-point/dependency/graph service authorization now requires explicit admin/teacher facts.
  - `AssessmentService` answer/wrong-question list/detail service authorization now requires explicit admin/teacher facts.
  - `GradingEvaluationService.evaluate(String, GradingEvaluationRequest)` was removed while pure metric evaluation entries remain intact.
  - Added reflection guards in focused service tests so the legacy overloads/helpers cannot be reintroduced.
  - No REST API contract, DTO, DB schema, dependency, frontend, Spring Security, RAG runtime, Agent runtime, or Review Gate change was added.
  - Verification passed: compile guard success; focused `22 run, 0 failures`; adjacent `197 run, 0 failures`; full backend `536 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for broader class/course matrix, answer-record expansion, dev/test legacy fallback cleanup, and a later frontend production streaming client / sensitive SSE URL cleanup task.

- P3-4 子任务 SSE production auth strategy:
  - Added `SseProductionAuthStrategyTest` to pin the backend Chat/Tutor SSE production/staging Bearer/JWT fail-closed strategy.
  - production Chat/Tutor SSE no Bearer or invalid Bearer now returns `UNAUTHORIZED` before async work starts and before `RagQueryService` is called.
  - staging Chat/Tutor SSE header-only `X-User-Id` auth is rejected before async work starts.
  - valid Bearer Chat/Tutor SSE uses JWT subject and role facts while ignoring spoofed `X-User-Id`; Bearer `USER sub=admin` does not gain admin facts.
  - No production code, REST API contract, DTO, schema, dependency, frontend, query token, signed stream token, or cookie/session change was added.
  - Verification passed: focused `10 run, 0 failures`; adjacent auth/SSE/RAG runtime `38 run, 0 failures`; full backend `530 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for broader class/course matrix follow-up, answer-record expansion, dev/test legacy fallback cleanup, and a later frontend production streaming client / sensitive SSE URL cleanup task.

- P3-4 子任务 KB-course binding governance:
  - Added V20 KB-course binding governance schema: `kb_knowledge_base.course_id`, `binding_status`, `bound_by`, and `bound_at`, with binding status constraints and backfill to `UNBOUND` / `BOUND` / `CONFLICTED`.
  - `KnowledgeBaseService` now supports course-bound KB creation with `CourseAccessService.requireCourseRead(...)` + `requireCourseManage(...)`.
  - `PermissionService` now routes `BOUND` KB read/write through `CourseAccessService`; `PUBLIC`, owner, explicit permission, and admin early-return paths cannot bypass course access.
  - `DocumentService` now supports empty `UNBOUND` KB first course-document upload auto-binding under a pessimistic KB row lock, rejects mixed-course uploads, and blocks `CONFLICTED` document upload.
  - Preserved document upload idempotency semantics: same `createdBy + requestId` with different payload returns `409 CONFLICT` before KB-course mismatch validation.
  - Added regressions for admin CourseAccess path, requestId course/chapter conflict priority, course-bound replay omitting `courseId`, `CONFLICTED` denial, and RAG forbidden no-artifact behavior.
  - Verification passed: focused PermissionService `9 run, 0 failures`; focused DocumentController `25 run, 0 failures`; adjacent KB-course matrix `76 run, 0 failures`; full backend `520 run, 0 failures, 0 errors, 1 skipped`.
  - MySQL smoke was attempted but blocked by local environment: Docker daemon unavailable and local MySQL root credential rejected.
  - P3-4 parent remains open for broader class/course matrix follow-up, answer-record expansion, dev/test legacy fallback cleanup, and a later frontend production streaming client / sensitive SSE URL cleanup task.

- P3-4 子任务 RAG query runtime roles-first RBAC:
  - `/api/rag/query` POST / GET、Chat SSE、Tutor ask / stream now derive explicit admin/teacher facts from `UserContext.roles()` and pass them into `RagQueryService`.
  - `RagQueryService` now exposes role-aware query, traceId, requestId, traceId+requestId, and replay overloads; legacy overloads remain but default to `admin=false` / `teacher=false`.
  - Orchestrator `RAG_QA` replay precheck and query execution now pass the same runtime role facts into RAG query replay/execution.
  - Added regressions for Bearer `ADMIN` with spoofed `X-User-Id`, Bearer `USER sub=admin` role-confusion denial, role-aware requestId replay, legacy literal `admin` denial, and Orchestrator `RAG_QA` side-effect-free forbidden handling.
  - No REST API contract, DTO, DB schema, dependency, frontend, or secret changes were added.
  - Verification passed: Orchestrator focused `30 run, 0 failures`; runtime RBAC focused `60 run, 0 failures`; adjacent permission/API `161 run, 0 failures`; full backend `509 run, 0 failures, 0 errors, 1 skipped`.
  - P3-4 parent remains open for broader class/course matrix follow-up, answer-record expansion, dev/test legacy fallback cleanup, and a later frontend production streaming client / sensitive SSE URL cleanup task.

- P3-2 parser/OCR TODO status reconciliation:
  - Reconciled `docs/planning/backend-architecture-todolist.md` with accepted P3-2-E/F/G/H/I evidence.
  - Marked the parser/OCR/page hierarchy TODO as completed for the minimum productionized capability: PDFBox/POI provider, configurable/process OCR fallback, and best-effort `pageNum` / `headingPath` propagation.
  - Added a separate follow-up TODO for industrial PDF/DOCX layout/table/TOC/reading-order, native/cloud OCR, OCR confidence, and true rendered page numbers.
  - No backend code, schema, dependency, API, frontend, or secret files were changed by this reconciliation task.

- P3-3 model provider TODO status reconciliation:
  - Reconciled `docs/planning/backend-architecture-todolist.md` with already accepted P3-3-B / P3-3-C evidence.
  - Marked the Spring AI OpenAI-compatible Chat/Embedding provider adapter TODO as completed while keeping DashScope / Spring AI Alibaba dedicated provider enhancement and external provider smoke as follow-up work.
  - Marked provider/model/promptVersion/latency/token/error logging as completed based on P3-3-A gateway evidence plus P3-3-B `model_call_log.provider` persistence.
  - No backend code, schema, dependency, API, frontend, or secret files were changed by this reconciliation task.

- P3-4 子任务 formal OAuth2/JWK/Spring Security:
  - Added Spring Security Resource Server authentication boundary with `SecurityFilterChain`, project JSON 401/403 handlers, JWK Set URI decoder path, HS256 compatibility decoder path, issuer validation, and optional audience validation.
  - `CurrentUserService` now prefers Spring Security `JwtAuthenticationToken` and maps JWT `sub` / `name` / whitelisted `roles` into `UserContext`; roles are not inferred from subject names.
  - `DevAuthFilter` no longer hand-verifies Bearer tokens and only keeps dev/test no-Bearer `X-User-Id` fallback.
  - Production-like environments require a real `JwtAuthenticationToken`; anonymous authentication and spoofed `X-User-Id` no longer satisfy production auth. Missing JWK/HS256 secret fails fast, and HS256 secrets must be at least 32 bytes.
  - Added SecurityFilterChain tests for no token, invalid token, wrong issuer, wrong audience, expired token, valid Bearer ignoring spoofed headers, and `sub=admin roles=USER` role-confusion denial.
  - Added focused coverage for production-like missing JWK/HS256 auth material fail-fast and production no-context fallback using `unauthenticated` instead of `dev_user`.
  - Updated existing JWT test fixtures to use 32-byte fake HS256 secrets and isolated non-security `@WebMvcTest` slices from default Spring Security auto-configuration.
  - Verification passed: focused `27 run, 0 failures`; adjacent `106 run, 0 failures`; full backend `500 run, 0 failures, 0 errors, 1 skipped`.

- P3-4 子任务 broader class/course permission penetration tests:
  - Added CourseKnowledge and Analytics MockMvc penetration tests for `USER sub=teacher_1` subject-name role-confusion, spoofed `X-User-Id`, and active-enrollment-only class analytics.
  - Fixed a RED authorization gap in `AnalyticsController`: analytics HTTP role checks now derive admin/teacher facts from `UserContext.roles()` instead of `CurrentUserService.isAdmin()` / `isTeacherUser()` subject-name fallback.
  - Added expert subagent reports and integration review for architect, security, and test coverage.
  - No REST API contract, DTO, schema, dependency, frontend, or formal OAuth2/JWK/Spring Security change was added.
  - Verification passed: RED compile/import and RED authorization gap observed; focused `59 run, 0 failures`; adjacent `82 run, 0 failures`; full backend `487 run, 0 failures, 0 errors, 1 skipped`.

- P3-4 子任务 Analytics teacherClassSummary legacy subject-name cleanup:
  - Removed `AnalyticsService.teacherClassSummary(String, String)` legacy public overload.
  - Removed `AnalyticsService.isLegacyTeacherUser(String)` subject-name teacher helper.
  - `AnalyticsService.requireTeacherClassAccess(...)` no longer grants admin/teacher access from subject name when explicit role facts are false.
  - `AnalyticsService.classLearnerIds(...)` now fails closed with `Set.of()` when `CourseAccessService` is absent instead of inferring class membership from `LearningPath.goalId`.
  - Added `AnalyticsServiceTest` reflection guards and behavior regressions for service-level role-confusion cleanup.
  - No REST API contract, request DTO, response DTO, schema, dependency, frontend, `AnalyticsController`, `CourseAccessService`, or formal OAuth2/JWK/Spring Security change was added.
  - Verification passed: RED `4 run, 4 failures` plus membership RED `1 run, 1 failure`; focused `5/5`; compile guard success; adjacent `73/73`; full backend `482 run, 0 failures, 0 errors, 1 skipped`.

- P3-4 子任务 LearningPath create legacy overload cleanup:
  - Removed `LearningWorkflowService.createPathForUser(String, CreateLearningPathRequest)` legacy public overload.
  - Removed `LearningWorkflowService.isAdmin(String)` subject-name admin helper.
  - `LearningWorkflowService` create path public API surface now requires explicit roles-first admin/teacher facts for non-template authorization behavior.
  - Added reflection guards and a roles-first admin behavior regression test in `LearningWorkflowServiceTest`.
  - No REST API contract, request DTO, response DTO, schema, dependency, frontend, `LearningPathController`, `CourseAccessService`, ResourceGeneration, Orchestrator, Agent Trace, Review Gate, or formal OAuth2/JWK/Spring Security change was added.
  - Verification passed: RED `2 run, 2 failures`, focused `3/3`, compile guard success, adjacent `25/25`, full backend `477 run, 0 failures, 0 errors, 1 skipped`.

- Workflow Size Classification:
  - `AGENTS.md` now routes raw requirements through S/M/L size classification before document creation.
  - Small slices can use one mini TASK with embedded Context Pack and combined Evidence/Acceptance instead of full PRD/REQ/SPEC/PLAN/TASK/CONTEXT/subagent/retro.
  - Medium and large tasks keep stronger design, context, subagent, evidence, and retrospective requirements according to risk.
  - Long-running epics such as `P3-4` should now use parent ID + semantic child task names instead of artificial `X/Y/Z` slice names.
  - `.agents/skills/feature-development-workflow/SKILL.md` was updated to match the new size-aware workflow.

- P3-4-X LearningPath Detail Roles-First RBAC:
  - `LearningPathController` now derives explicit admin facts from `UserContext.roles()` for `GET /api/learning-paths/{pathId}`.
  - `LearningWorkflowService` now exposes a roles-first `getPathForUser(currentUserId, currentUserAdmin, pathId)` overload for detail reads.
  - Bearer `ADMIN sub=ops_admin` can read foreign learning path detail despite spoofed `X-User-Id`, and missing path keeps `NOT_FOUND`.
  - Bearer `USER sub=admin` no longer gains admin detail or missing-object semantics through subject-name role confusion.
  - Owner reads and non-owner missing/foreign anti-enumeration continue to behave safely.
  - No REST API contract, request DTO, response DTO, schema, dependency, frontend, formal OAuth2/JWK/Spring Security, Agent/RAG/model runtime, or create-path semantics change was added.
  - Verification passed: RED `7 run, 4 failures`, focused `7/7`, controller `20/20`, adjacent `52/52`, full backend `474 run, 0 failures, 0 errors, 1 skipped`.

- P3-4-W CourseAccessService Legacy Overload Cleanup:
  - `CourseAccessService` public course authorization API surface now only exposes roles-first overloads that receive explicit admin/teacher facts.
  - Removed legacy subject-name inference overloads for course read, course manage, learner enrollment, and course list.
  - Removed `scopedCourseMissing(String)`, `isAdmin(String)`, and `isTeacherUser(String)` helper paths from `CourseAccessService`.
  - Added reflection and behavior tests to prevent reintroducing subject-name role inference through `currentUserId = "admin"` or `currentUserId = "teacher_1"`.
  - No REST API contract, request DTO, response DTO, schema, dependency, frontend, formal OAuth2/JWK/Spring Security, Agent/RAG/model runtime, or broader class/course change was added.
  - Verification passed: RED `4 run, 2 failures`, focused `4/4`, compile guard success, adjacent `183/183`, full backend `467 run, 0 failures, 0 errors, 1 skipped`.

- P3-4-V ResourceGeneration / Agent Trace Detail Roles-First RBAC:
  - `ResourceGenerationController` now derives explicit admin facts from `UserContext.roles()` for `GET /api/resources/generation-tasks/{taskId}` and `GET /api/agent/resource-generation/tasks/{taskId}/learner-resources`.
  - `AgentTraceController` and `AgentTraceGovernanceController` now pass explicit admin facts from `UserContext.roles()` into Agent Trace detail/search services.
  - `ResourceGenerationService` and `AgentTraceGovernanceService` now expose roles-first overloads while preserving legacy overloads for compatibility.
  - Bearer `ADMIN sub=ops_admin` can read ResourceGeneration detail and Agent Trace detail/search despite spoofed `X-User-Id`.
  - Bearer `USER sub=admin` no longer gains admin read/search semantics; learner-resources remains owner-only and non-admin missing/foreign paths remain safe `FORBIDDEN`.
  - No dependency, schema, API path, request DTO, response DTO, frontend, formal OAuth2/JWK/Spring Security, Agent/RAG/model runtime, Review Gate, or cancel authorization change was added.
  - Verification passed: RED `9 run, 9 failures`, focused `9/9`, adjacent `108/108`, full backend `463 run, 0 failures, 0 errors, 1 skipped`.

- P3-4-U Review Gate ResourceReview Roles-First RBAC:
  - `ResourceReviewController` now derives admin/teacher facts only from `UserContext.roles()` for `GET /api/reviews/resources` and `POST /api/reviews/resources/{reviewId}/decision`.
  - `ReviewGovernanceService` now exposes roles-first list/decision overloads while preserving legacy overloads for compatibility.
  - Bearer `ADMIN sub=ops_admin` can list/decide resource reviews despite spoofed `X-User-Id`.
  - Bearer `TEACHER sub=instructor_1` can review own-course resources without a `teacher_` subject prefix.
  - Bearer `USER sub=admin` and `USER sub=teacher_1` role-confusion cases are denied with safe `FORBIDDEN`.
  - No dependency, schema, API path, request DTO, response DTO, frontend, formal OAuth2/JWK/Spring Security, ResourceGeneration, or Agent Trace change was added.
  - Verification passed: RED `16 run, 3 failures`, focused `16/16`, adjacent `56/56`, full backend `454 run, 0 failures, 0 errors, 1 skipped`, and integration review PASS for target code path.

- P3-4-T Orchestrator `RESOURCE_GENERATION` Create Roles-First RBAC:
  - `OrchestratorWorkflowController` now derives admin/teacher facts only from `UserContext.roles()` for `POST /api/orchestrator/workflows` and retry.
  - `OrchestratorWorkflowService` now exposes roles-first create/retry overloads and routes `RESOURCE_GENERATION` workflow create through the roles-first ResourceGeneration workflow API.
  - Bearer `USER sub=admin` can no longer bypass course enrollment through Orchestrator `RESOURCE_GENERATION`.
  - Bearer admin/teacher still do not gain ResourceGeneration代创建 ability; ResourceGeneration remains owner-only.
  - Forbidden Orchestrator ResourceGeneration requests leave no ResourceGeneration/model/token/citation durable side effects, while course-enrollment denial may keep safe failed workflow evidence.
  - No dependency, schema, API path, request DTO, response DTO, frontend, or formal OAuth2/JWK/Spring Security change was added.
  - Verification passed: RED `28 run, 1 failure`, focused `28/28`, adjacent `94/94`, full backend `449 run, 0 failures, 0 errors, 1 skipped`, and integration review PASS for P3-4-T scope.

- P3-4-S LearningPath / ResourceGeneration Direct Create Roles-First RBAC:
  - `LearningPathController` now derives admin/teacher facts only from `UserContext.roles()` for `POST /api/learning-paths`.
  - `LearningWorkflowService` now exposes a roles-first `createPathForUser(...)` overload; explicit `ADMIN` can create course-bound learning paths for other learners and bypass enrollment, while `USER sub=admin` cannot.
  - `ResourceGenerationController` now derives role facts only from `UserContext.roles()` for `POST /api/resources/generation-tasks`.
  - `ResourceGenerationService` direct create now uses a roles-first overload while preserving owner-only semantics; admin/teacher do not gain代创建 ability.
  - `CourseAccessService` now exposes a roles-first course-bound learner enrollment helper while preserving the legacy signature for compatibility.
  - Forbidden resource generation direct create is rejected before task/resource/review/trace/model/token durable side effects.
  - No dependency, schema, API path, request DTO, response DTO, frontend, formal OAuth2/JWK/Spring Security, or Orchestrator workflow change was added in P3-4-S; Orchestrator `RESOURCE_GENERATION` create/retry was later closed by P3-4-T.
  - Verification passed: RED `32 run, 3 failures`, focused `32/32`, adjacent `91/91`, full backend `446 run, 0 failures, 0 errors, 1 skipped`, and integration review CONDITIONAL PASS for direct API scope.

- P3-4-R Assessment / GradingEvaluation Roles-First RBAC:
  - `AssessmentController` now derives admin/teacher facts only from `UserContext.roles()` for assessment answer/wrong-question read paths and grading evaluation.
  - `AssessmentService` now exposes roles-first overloads for answer/wrong-question list and detail paths while keeping legacy overloads for compatibility.
  - `GradingEvaluationService` now exposes `evaluate(currentUserId, admin, teacher, request)` and uses role-aware `CourseAccessService.requireCourseRead(...)` on the HTTP path.
  - Bearer `ADMIN` works despite spoofed `X-User-Id`; Bearer `TEACHER` no longer needs a `teacher_` subject prefix for own-course assessment/grading reads.
  - Bearer `USER sub=admin` and `USER sub=teacher_1` role-confusion cases are denied with safe `FORBIDDEN`.
  - No dependency, schema, API path, request DTO, response DTO, frontend, `POST /api/assessment/answers`, or formal OAuth2/JWK/Spring Security changes were added.
  - Verification passed: RED `37 run, 11 failures`, focused `37/37`, adjacent `123/123`, full backend `442 run, 0 failures, 0 errors, 1 skipped`, and integration review PASS.

- P3-4-Q Analytics Student Summary Roles-First RBAC:
  - `AnalyticsService.requireCourseReadForStudentSummary(...)` now calls the role-aware `CourseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId)` overload instead of the legacy subject-name inference signature.
  - Bearer `ADMIN sub=ops_admin` can read course-scoped student summaries despite spoofed `X-User-Id`, and admin missing course keeps `NOT_FOUND`.
  - Bearer `TEACHER sub=instructor_1` can read own-course active-enrolled learner summaries without a `teacher_` subject prefix.
  - Bearer `USER sub=teacher_1` role-confusion and teacher missing/foreign course oracle cases are denied with safe `FORBIDDEN`.
  - No dependency, schema, API path, request DTO, response DTO, frontend, Assessment/GradingEvaluation, LearningPath/ResourceGeneration, or formal OAuth2/JWK/Spring Security changes were added.
  - Verification passed: RED `34 run, 3 failures`, focused `34/34`, adjacent `68/68`, and full backend `431 run, 0 failures, 0 errors, 1 skipped`.

- P3-4-P RAG KB Management Roles-First RBAC:
  - `KnowledgeBaseController` and `DocumentController` now derive admin/teacher facts only from `UserContext.roles()` and pass them explicitly to RAG KB/document application services.
  - `KnowledgeBaseService`, `DocumentService`, and `PermissionService` now expose roles-first overloads while keeping legacy signatures for compatibility with non-target paths.
  - Bearer `ADMIN` can list/read/write active KBs despite spoofed `X-User-Id`; Bearer `TEACHER` no longer needs a `teacher_` subject prefix for own-course document metadata upload.
  - Bearer `USER sub=admin` and `USER sub=teacher_1` role-confusion cases are denied; non-admin missing document/reindex/index-task requests collapse to safe `FORBIDDEN`, while admin missing keeps `NOT_FOUND`.
  - No dependency, schema, API path, request DTO, response DTO, frontend, parser/vector/index worker/storage/model runtime, `/api/rag/query` retrieval runtime, or formal OAuth2/JWK/Spring Security changes were added.
  - Verification passed: RED `26 run, 6 failures`, focused controller `26/26`, focused service `4/4`, adjacent `30/30` and `34/34`, and full backend `426 run, 0 failures, 0 errors, 1 skipped`.

- P3-4-O Evaluation Set / Run Roles-First RBAC:
  - `EvaluationSetController` and `EvaluationRunController` now derive admin/teacher facts only from `UserContext.roles()` and pass them explicitly to application services.
  - `EvaluationSetService` and `EvaluationRunService` management paths now require explicit role facts and no longer infer admin/teacher from legacy subject strings such as `admin` or `teacher_*`.
  - Bearer `ADMIN` works despite spoofed `X-User-Id`; Bearer `TEACHER` no longer needs a `teacher_` subject prefix for authorized evaluation management.
  - Bearer `STUDENT/USER sub=admin` and `USER sub=teacher_1` role-confusion cases are denied.
  - Non-admin missing and foreign evaluation set / run comparison requests collapse to safe `FORBIDDEN`; admin missing keeps `NOT_FOUND`.
  - No dependency, schema, API path, request DTO, frontend, RAG KB management, broader class/course, or formal OAuth2/JWK/Spring Security changes were added.
  - Verification passed: RED `15 run, 9 failures`, focused `15/15` controller and `19/19` service, adjacent `48/48` and `73/73`, and full backend `419 run, 0 failures, 0 errors, 1 skipped`.

- P3-4-N PromptVersion Management API RBAC:
  - `POST /api/agent/prompt-versions` now requires an explicit `ADMIN` role derived from `UserContext.roles()`.
  - PromptVersion list/detail now require `ADMIN` or `TEACHER`; teachers receive metadata-only responses with `promptText` omitted.
  - Bearer roles override spoofed `X-User-Id`; Bearer `STUDENT/USER sub=admin` and `USER sub=teacher_1` no longer gain PromptVersion management access.
  - `PromptVersionService` management methods require explicit role facts; internal `findActiveByCode(...)` remains available for model-call linkage.
  - No dependency, schema, API path, frontend, formal OAuth2/JWK/Spring Security, Evaluation, RAG KB, or full RBAC migration changes were added.
  - Verification passed: focused `14 run, 0 failures, 0 errors`, adjacent `48 run, 0 failures, 0 errors`, and full backend `410 run, 0 failures, 0 errors, 1 skipped`.

- P3-4-M Course API / CourseAccessService Roles-First Overload:
  - `CourseAccessService` now has roles-first read/manage/list overloads while keeping legacy signatures for compatibility.
  - `CourseController` and `KnowledgePointController` now pass explicit `UserContext.roles()` facts into `KnowledgeCatalogService` for Course API / knowledge graph main paths.
  - `KnowledgeCatalogService` now has roles-first create/read/list/chapter/knowledge point/dependency/graph overloads and routes Course API authorization through role-aware course access.
  - Bearer `ADMIN sub=ops_admin` can list/read existing courses despite spoofed `X-User-Id`; Bearer `TEACHER sub=instructor_1` can read/manage own course without a `teacher_` prefix; Bearer student spoofing and `USER sub=admin` / `USER sub=teacher_1` role-confusion cases are denied.
  - No dependency, schema, API path, frontend, RAG, model provider, VectorDB, formal OAuth2/JWK/Spring Security, or full RBAC migration changes were added.
  - Verification passed: focused `20 run, 0 failures, 0 errors`, adjacent `63 run, 0 failures, 0 errors`, and full backend `403 run, 0 failures, 0 errors, 1 skipped`.

- P3-4-L Class Analytics Roles-First Course Scope:
  - `GET /api/analytics/classes/{courseId}/summary` now uses role-derived `CurrentUserService.isAdmin()` / `isTeacherUser()` inputs instead of relying only on legacy `currentUserId` string checks.
  - Bearer `ADMIN` can read existing class summary even with spoofed `X-User-Id`; Bearer `TEACHER` must still own the course; Bearer `STUDENT` remains denied even with spoofed admin header.
  - Non-admin missing and foreign class summary requests now collapse to safe `FORBIDDEN`; admin missing course keeps `NOT_FOUND`.
  - No dependency, schema, API path, frontend, RAG, model provider, VectorDB, formal OAuth2/JWK/Spring Security, or broader class/course model changes were added.
  - Verification passed: focused `29 run, 0 failures, 0 errors`, adjacent `56 run, 0 failures, 0 errors`, and full backend `396 run, 0 failures, 0 errors, 1 skipped`.

- P3-2-I Real OCR Provider:
  - Added `ProcessOcrFallbackProvider` behind the existing `OcrFallbackProvider` SPI. `provider=process` now launches an external command with `ProcessBuilder(List<String>)`, feeds PDF bytes through stdin, reads stdout as OCR text, and maps command missing / non-zero exit / timeout / exception to safe OCR status codes.
  - `RagParserOcrProperties` now includes `process.command`, `process.timeout`, and `process.max-output-chars`; Spring record binding and provider injection were made explicit so Spring Boot 3.5 context creation stays deterministic.
  - Default OCR remains disabled and no OCR Maven dependency was added. `IndexService`, API, DB, frontend, retrieval/citation, and VectorDB were not changed.
  - Verification passed: focused `21 run, 0 failures, 0 errors`, adjacent `33 run, 0 failures, 0 errors`, no OCR dependency tree hits, compile success, and full backend `392 run, 0 failures, 0 errors, 1 skipped`.

- P3-2-H Configurable OCR Fallback Provider:
  - Added `learning-os.rag.parser.ocr.enabled` and `learning-os.rag.parser.ocr.provider` configuration with default disabled/noop behavior.
  - Added `RagParserOcrProperties`, `OcrFallbackProvider` SPI, and `ConfigurableOcrFallbackService` so future OCR providers can be selected without multiple `OcrFallbackService` bean ambiguity.
  - Provider unavailable/failure states now return fixed safe reason codes, and fake OCR success can drive PDF image-only fallback tests.
  - No OCR/native/cloud dependency, `pom.xml`, `IndexService`, API, DB, frontend, retrieval/citation, or VectorDB changes were added.
  - TDD RED/GREEN, focused, adjacent, dependency tree, compile, and full backend Maven verification passed: `385 run, 0 failures, 0 errors, 1 skipped`.

- Documentation note:
  - 当前 `CHANGELOG.md` 为近期条目的压缩恢复版；未在工作区找到原完整历史备份，后续若发现历史来源应恢复完整历史后再保留本次 P3-4-H 条目。

- P3-2-G Real PDF/DOCX Parser SDK Provider:
  - Added real Apache PDFBox 3.0.7 and Apache POI poi-ooxml 5.5.1 parser providers behind the existing `rag/parser` boundary.
  - `PdfBoxDocumentFormatParser` now extracts real PDF text per page with `pageNum`; `PoiDocxDocumentFormatParser` now extracts real DOCX heading/page metadata and separators.
  - Input/output limits and safe `DOCUMENT_PARSE_FAILED` mapping remain in place; `IndexService`, API, DB, frontend, retrieval/citation, and VectorDB contracts were not changed.
  - Focused, adjacent, dependency tree, and full backend Maven verification passed: `378 run, 0 failures, 0 errors, 1 skipped`.

- P3-2-F Parser Provider Boundary + OCR Fallback Contract:
  - Added `ParseInput`, `DocumentFormatParser`, `OcrFallbackService`, `OcrFallbackResult`, and `NoopOcrFallbackService`.
  - `DocumentParserService` now dispatches through a provider registry while preserving the existing `ParsedDocument/ParsedSection` boundary consumed by `IndexService`.
  - Noop OCR returns `DISABLED / OCR_DISABLED / ""`; image-only PDFs still produce no sections and never fallback to raw PDF bytes.
  - Provider failures continue to map to the safe `DOCUMENT_PARSE_FAILED` code.
  - No dependency, schema, API, frontend, VectorDB, retrieval, or citation contract changes were added.
  - TDD RED/GREEN, adjacent regression, and full backend Maven verification passed: `371 run, 0 failures, 0 errors, 1 skipped`.
- P3-4-K Permission Penetration Matrix:
  - Added current transitional permission-matrix coverage for staging header-only auth denial, Bearer roles overriding spoofed `X-User-Id`, student course-graph write denial, dropped enrollment course-list redaction, and RAG document course-metadata spoofing denial.
  - Analytics admin-only entries now use roles-first `CurrentUserService.isAdmin()` / `currentUserAdmin` gates instead of relying on the literal `"admin"` user id.
  - Token-budget governance rejects header-only or non-admin access while allowing Bearer `ADMIN` role access.
  - No dependency, schema, API path, frontend, formal OAuth2/JWK/Spring Security, or broader class/course model changes were added.
  - TDD RED/GREEN、相邻回归和全量后端验证已通过。

- P3-2-E RAG Parser Layout / Page Hierarchy:
  - PDF parser now performs no-dependency best-effort page segmentation for simple `/Type /Page` markers while continuing to extract only `Tj` / `TJ` text operators.
  - DOCX parser now splits text around same-paragraph page breaks and treats `w:tab` / non-page `w:br` as text separators.
  - Multi-page PDF page numbers now propagate into `KbDocChunk.pageNum` through the existing parser/chunk boundary.
  - No raw PDF fallback was restored; no dependency, schema, API, or frontend changes were added.
  - TDD RED/GREEN、相邻回归和全量后端验证已通过。

- P3-3 Real Model Provider Adapter:
  - Spring AI BOM upgraded to `1.0.8`.
  - Added `org.springframework.ai:spring-ai-starter-model-openai`.
  - `AiModelGateway` now supports OpenAI-compatible Spring AI `ChatModel` when `AI_MODEL_PROVIDER != none` and chat model configuration is complete.
  - `EmbeddingService` now supports OpenAI-compatible Spring AI `EmbeddingModel` when embedding provider configuration is complete.
  - Default `AI_MODEL_PROVIDER=none` keeps deterministic/noop local behavior without external calls.
  - Missing provider beans fail closed with safe error codes instead of returning placeholder success.
  - Provider non-JSON/schema-invalid output maps to `STRUCTURED_OUTPUT_INVALID`; raw provider errors remain sanitized.
  - Unused OpenAI image/audio/moderation auto-config is explicitly disabled.
  - Focused, adjacent, dependency tree, compile, and full backend Maven verification passed.

- P3-4-J Analytics Student Summary Course Scope:
  - `GET /api/analytics/students/{learnerId}/summary` now accepts optional `courseId`.
  - Student course-scoped summary reads require own learner plus active enrollment.
  - Teacher summary reads require `courseId`, own course, and active enrolled learner.
  - Admin can read global and course-scoped learner summaries.
  - Course-scoped summary aggregation now filters path/mastery/wrong-question signals to the requested course.
  - TDD RED/GREEN、相邻回归和全量后端验证已通过。

- P3-4-I Real Auth Context / RBAC Compatibility:
  - Added `learning-os.auth.jwt-secret` / `learning-os.auth.issuer` configuration with `AUTH_JWT_SECRET` / `AUTH_JWT_ISSUER` env binding.
  - Bearer HS256 JWT now establishes `UserContext` with `sub`, optional `name`, and roles.
  - Invalid Bearer token returns fixed `UNAUTHORIZED` and never falls back to `X-User-Id`.
  - `prod` / `production` / `staging` no longer trust `X-User-Id`; missing token returns `UNAUTHORIZED`.
  - `dev` / `test` keep `X-User-Id` / `dev_user` fallback only when Bearer token is absent.
  - `CurrentUserService.isAdmin()` / `isTeacherUser()` now prefer roles and keep legacy userId inference only in dev/test.
  - TDD RED/GREEN, adjacent regression, and full backend Maven verification passed.
- P3-4-H RAG Document Course/Chapter Metadata Scope:
  - 收口 `POST /api/knowledge-bases/{kbId}/documents` 的 course/chapter 元数据写入权限。
  - `DocumentService.upload(...)` 现在会在 `storageService.store(...)` 之前执行 course manage + chapter belongs-to-course 校验。
  - teacher 只能上传自己课程的文档元数据；student 即使 enrolled 且拥有 KB 写权限也不能伪造课程元数据；admin missing course 返回 `NOT_FOUND`。
  - `chapterId` 非空但缺少 `courseId` 返回 `VALIDATION_ERROR`；missing/foreign chapter 返回固定通用 `VALIDATION_ERROR`。
  - requestId hash 继续包含归一化后的 `courseId/chapterId`，不同 course/chapter payload 仍保持冲突语义。
  - TDD RED/GREEN、相邻回归和全量后端验证已通过。

## 2026-06-08 - P3-4-G Grading Evaluation Course Scope

- `POST /api/assessment/grading-evaluations` now requires `courseId`.
- Student requests are denied first; teachers can run evaluations only for own courses; admin missing course returns `NOT_FOUND`.
- Non-blank sample `knowledgePointId` values must belong to the request course.

## 2026-06-08 - P3-4-F Assessment Record List RBAC / Pagination

- Added paginated answer/wrong-question list endpoints.
- Student lists are owner-only; teacher lists require `courseId` and active enrollment; admin lists are global/filterable.
- Summary DTOs omit answer text and internal snapshot/payload fields.

## 2026-06-08 - P3-4-E Assessment Record RBAC Matrix

- Added safe answer/wrong-question detail endpoints.
- Student reads are owner-only; teacher reads require own course plus active enrollment; admin reads are global.
- Non-admin missing/foreign reads collapse to safe `FORBIDDEN`.

## 2026-06-08 - P3-4-D Course Enrollment Scope

- Added V19 `course_enrollment` and centralized `CourseAccessService`.
- Student course list/detail/knowledge-graph reads now use active enrollment scope.
- Course-bound learning path and resource generation creation now check active enrollment.

## 2026-06-08 - P3-3-B Model Call Provider Observability

- Added `model_call_log.provider` persistence with low-cardinality normalization.
- Provider strings are normalized to `none/openai/dashscope/anthropic/gemini/mock/other`.
- Success and failure evidence now carries provider metadata without leaking sensitive endpoint strings.

## 2026-06-07 - P3-5 Observability Hardening

- Structured request logging now emits whitelisted `traceId/userId/route/status/latencyMs/errorCode`.
- `/api/health` probes database, Redis, MinIO, and model provider state with sanitized metadata.
- `LearningOsMetrics` exposes HTTP / RAG / model / token / cost metrics through Actuator.

## 2026-06-07 - P3-4-C Course Read And Grading Evaluation Permission

- `GET /api/courses` is scoped by admin/teacher/student role.
- `GET /api/courses/{courseId}` and `/knowledge-graph` now use service-layer course read authorization.
- Initial grading evaluation course gate was added here and later tightened by P3-4-G.

## 2026-06-07 - Review Gate Course Scope Hardening

- Review list/decision APIs keep `admin` global and limit `teacher` to own-course reviews.
- Non-admin missing/foreign review decisions return safe `FORBIDDEN`.
