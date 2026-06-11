# API_MEMORY.md

## API Conventions

- Base path: `/api/v1/`
- Auth: Bearer token or session cookie (backend validates); Bearer JWT takes precedence, and dev/test may additionally accept `X-User-Id` / `dev_user` when Bearer is absent
- Response envelope: `{ "code": 0, "data": {}, "message": "" }`
- Error codes: documented per endpoint in SPEC
- traceId: returned in response headers (`X-Trace-Id`)

## Existing API Groups

| Group | Base Path | Status |
|---|---|---|
| Health | `/api/v1/health` | active |
| User | `/api/v1/users` | active |
| Learning | `/api/v1/learning` | active |
| Assessment | `/api/v1/assessment` | active |
| RAG / Chat | `/api/v1/rag`, `/api/v1/chat` | active |
| Orchestrator | `/api/v1/orchestrator` | active |
| Tutor | `/api/v1/tutor` | active |
| Analytics | `/api/v1/analytics` | active |
| Knowledge | `/api/v1/knowledge` | active |

## Rules

- API contract changes require SPEC update before code change.
- Frontend uses typed API client modules in `frontend/src/api/`.
- SSE endpoints for AI streaming documented separately.

## Authentication Context Compatibility

- Bearer JWT is the preferred backend identity path for current-user construction and is handled by Spring Security OAuth2 Resource Server.
- `learning-os.auth.jwk-set-uri` is the preferred production verification path; local/test HS256 compatibility uses `learning-os.auth.jwt-secret` and requires at least 32 bytes.
- Backend validates signature, `sub`, `exp`, `iss`, and optional configured `audience` before building `UserContext`.
- Valid tokens establish `UserContext(sub, name/sub, roles)` through `JwtAuthenticationToken`.
- Invalid Bearer tokens return `UNAUTHORIZED` and never fall back to `X-User-Id`.
- `prod` / `production` / `staging` do not trust `X-User-Id`, require a real `JwtAuthenticationToken`, and fail fast if neither JWK Set URI nor HS256 secret is configured.
- Production-like no-context fallback reports `unauthenticated` rather than `dev_user` for helper/logging paths.
- `dev` / `test` keep `X-User-Id` / `dev_user` fallback only when Bearer is absent.

## Formal OAuth2/JWK/Spring Security Notes

- P3-4 formal auth slice introduced Spring Security Resource Server without changing REST paths, request DTOs, response DTOs, or database schema.
- 401 and 403 from Spring Security use the project `ApiResponse` envelope and do not expose token, secret, signature, JWK, issuer config, or raw exception text.
- JWT roles are whitelisted to `ADMIN`, `TEACHER`, `STUDENT`, and `USER`; unknown role claims are ignored and empty roles default to `USER`.
- Subject strings such as `sub=admin` or `sub=teacher_1` do not grant admin/teacher role semantics unless the JWT role claim includes the matching whitelisted role.
- Full backend verification passed after the formal auth slice: `500 run, 0 failures, 0 errors, 1 skipped`.

## Analytics Class Summary Authorization Notes

- P3-4 broader class/course permission penetration tests exposed and fixed an HTTP-level analytics role boundary issue without changing the REST contract.
- `AnalyticsController` now derives admin/teacher facts from `UserContext.roles()` for analytics HTTP entrypoints instead of using `CurrentUserService.isAdmin()` / `isTeacherUser()` subject-name fallback.
- Bearer `USER sub=teacher_1` cannot read class summary for a course whose `teacherId` matches the subject unless the token has the `TEACHER` role.
- Bearer `TEACHER` class summary requests ignore spoofed `X-User-Id` and use token subject plus active enrollment membership.
- Dropped and never-enrolled learners are not counted in class analytics even if they have learning path, wrong-question, or resource-task signals.
- `GET /api/analytics/classes/{courseId}/summary` has no REST contract change for the P3-4 Analytics teacherClassSummary legacy subject-name cleanup.
- HTTP class summary continues to derive explicit admin/teacher facts from `UserContext.roles()` in the controller path.
- `AnalyticsService` no longer exposes the legacy two-argument `teacherClassSummary(String, String)` service method or the local `isLegacyTeacherUser(String)` subject-name helper.
- Service-level class summary authorization now requires the roles-first overload with explicit admin/teacher facts.

## LearningPath Create Authorization Notes

- `POST /api/learning-paths` has no REST contract change for the P3-4 LearningPath create legacy overload cleanup.
- HTTP create continues to derive explicit admin/teacher facts from `UserContext.roles()` in the controller path.
- `LearningWorkflowService` no longer exposes the legacy two-argument `createPathForUser(String, CreateLearningPathRequest)` service method or the local `isAdmin(String)` subject-name helper.
- Service-level LearningPath create authorization now requires the roles-first overload with explicit admin/teacher facts.

## Prompt Version Management

Related SPEC: `docs/specs/SPEC-20260605-prompt-version-management.md`

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/agent/prompt-versions` | POST | Create or upsert Prompt Version records by `code/version` |
| `/api/agent/prompt-versions` | GET | List Prompt Versions, optionally filtered by `code` |
| `/api/agent/prompt-versions/{code}/{version}` | GET | Query a single Prompt Version |

## Evaluation Set Management

Related SPEC: `docs/specs/SPEC-20260606-evaluation-set-management.md`

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/evaluation-sets` | POST | Create or replace an evaluation set with samples for `RAG_QUESTION`, `GRADING_SAMPLE`, or `RESOURCE_GENERATION_SAMPLE` |
| `/api/evaluation-sets` | GET | List evaluation sets, optionally filtered by `type`, `courseId`, or `promptCode` |
| `/api/evaluation-sets/{setId}` | GET | Query a single evaluation set with structured sample details |

## Evaluation Run And Prompt Version Comparison

Related SPEC: `docs/specs/SPEC-20260606-prompt-version-quality-comparison.md`

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/evaluation-runs` | POST | Record an evaluation run and whitelisted quality metrics for a prompt version |
| `/api/evaluation-runs/comparison` | GET | Compare succeeded evaluation runs by `evaluationSetId`, `promptCode`, and prompt versions |

## RAG And Grading Evaluation

Related SPECs:

- `docs/specs/SPEC-20260606-rag-quality-evaluation.md`
- `docs/specs/SPEC-20260606-grading-quality-evaluation.md`
- `docs/specs/SPEC-20260608-grading-evaluation-course-scope.md`

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/rag/evaluations` | POST | Evaluate legacy RAG source/citation inputs or benchmark samples with RAG quality metrics |
| `/api/assessment/grading-evaluations` | POST | Evaluate course-scoped grading samples with required `courseId`, MAE, grade agreement, wrong-cause agreement, and grouped analysis |

## Agent Trace Governance

Related SPEC: `docs/specs/SPEC-20260606-agent-trace-governance-dashboard.md`

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/agent/traces` | GET | Admin trace search filtered by user, agent type, status, time range, and failure reason; HTTP path uses explicit `ADMIN` role facts from `UserContext.roles()` |
| `/api/agent/tasks/{taskId}/trace` | GET | Trace detail with steps, sanitized tool calls, and retention policy; owner or explicit `ADMIN` can read, while `USER sub=admin` role-confusion is denied |

## Token / Cost Budget Governance

Related SPEC: `docs/specs/SPEC-20260606-token-budget-governance.md`

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/analytics/token-budget/governance` | GET | Admin cost governance view with user/course/Agent/time-window stats, budget decision, high-cost task warnings, and abnormal model-call detection |

## Operations Alerting

Related SPEC: `docs/specs/SPEC-20260607-backend-architecture-completion.md`

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/analytics/ops/alerts` | GET | Admin query-time alert view for slow RAG queries, slow model calls, RAG no-source rate/count, and Review Gate backlog with whitelisted alert DTOs |

## Permission / Security Hardening

Related SPEC: `docs/specs/SPEC-20260606-permission-security-hardening.md`

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/courses` | GET | Returns scoped course list: admin all courses, teacher own courses, student empty list until class/enrollment model exists. |
| `/api/courses/{courseId}` | GET | Returns course detail only to admin or owning teacher; non-admin missing/foreign course returns `FORBIDDEN` without `data`. |
| `/api/courses/{courseId}/knowledge-graph` | GET | Uses the same course read scope as course detail; non-admin missing/foreign course returns `FORBIDDEN` without `data`. |
| `/api/profile/dialogue/extract` | POST | Requires request `learnerId` to match the current user. |
| `/api/learning-paths` | POST | Direct create uses roles-first current-user facts. Explicit Bearer `ADMIN` can create for another learner and bypass course enrollment; non-admin users remain owner-only and course-bound creates require active enrollment. `USER sub=admin` role-confusion is denied. |
| `/api/learning-paths/{pathId}` | GET | Returns path detail to the path owner or explicit Bearer `ADMIN` derived from `UserContext.roles()`. Bearer `USER sub=admin` role-confusion is denied; non-admin missing/foreign returns safe `FORBIDDEN`, while admin missing returns `NOT_FOUND`. |
| `/api/resources/generation-tasks` | POST | Direct create uses roles-first current-user facts while preserving owner-only semantics. Admin/teacher do not gain代创建 ability; course-bound direct creates require active learner enrollment and forbidden requests are denied before task/resource/review/trace/model/token side effects. |
| `/api/resources/generation-tasks/{taskId}` | GET | Task detail uses explicit admin facts from `UserContext.roles()`; owner or explicit `ADMIN` can read existing tasks, `USER sub=admin` role-confusion is denied, non-admin missing/foreign returns safe `FORBIDDEN`, admin missing returns `NOT_FOUND`. |
| `/api/agent/resource-generation/tasks/{taskId}/learner-resources` | GET | Learner resource release view remains owner-only; explicit admin only affects missing-object `NOT_FOUND` semantics and does not gain learner resource read access. |
| `/api/analytics/overview` | GET | Admin-only analytics overview. |
| `/api/analytics/students/{learnerId}/summary` | GET | Student analytics summary. Optional `courseId` scopes aggregation to one course. Student reads require own learner and active enrollment when scoped; teacher reads require `courseId`, own course, and active enrolled learner; admin reads are global or course-scoped. Course-scoped reads now use role-aware `CourseAccessService` so Bearer admin/teacher subjects do not depend on legacy userId names. |
| `/api/health` | GET | Returns coarse dependency/configuration status without raw deployment details. |
| `/api/rag/query` | POST / GET | Strictly rejects any requested `kbIds` that are not readable by the current user. |
| `/api/knowledge-bases` | POST | Create a personal KB using roles-first current-user context; existing personal KB capability remains available without adding course binding schema. |
| `/api/knowledge-bases` | GET | List active KBs readable by the current user. Bearer `ADMIN` can list all active KBs; ordinary users keep owner/public/explicit-permission scope. |
| `/api/knowledge-bases/{kbId}/documents` | POST | Multipart document upload uses roles-first KB write checks and keeps requestId semantics; Bearer `ADMIN` can upload to active KBs despite spoofed headers; non-empty `courseId` now requires role-aware course manage scope, non-empty `chapterId` must belong to request `courseId`, and course/chapter validation runs before storage/document/index side effects. |
| `/api/knowledge-bases/{kbId}/documents` | GET | Lists documents only after roles-first KB read permission passes. |
| `/api/documents/{documentId}` | GET | Returns document detail only after roles-first KB read permission passes; non-admin missing/foreign requests return `FORBIDDEN` without `data`, admin missing returns `NOT_FOUND`. |
| `/api/documents/{documentId}/reindex` | POST | Reindex requires roles-first KB write permission; non-admin missing/foreign requests return `FORBIDDEN`, admin missing returns `NOT_FOUND`. |
| `/api/assessment/grading-evaluations` | POST | Requires `courseId`; student returns `FORBIDDEN`; teacher can evaluate only own courses; admin can evaluate any existing course; non-blank sample `knowledgePointId` must belong to request course. HTTP path uses admin/teacher facts derived from `UserContext.roles()`, not subject-name inference. |
| `/api/assessment/answers` | GET | Returns paginated answer summaries after assessment list scope passes: student owner-only, teacher required-course own-course active-enrollment learner, or admin global/filter. `page >= 0`; `size` defaults to `20` and is capped at `50`. Summary omits answer text and internal snapshot/payload fields. HTTP path uses roles-first admin/teacher facts. |
| `/api/assessment/answers/{answerId}` | GET | Returns answer detail only after assessment record scope passes: student owner, teacher own-course active-enrollment learner, or admin global. Non-admin missing/foreign returns `FORBIDDEN` without `data`; admin missing returns `NOT_FOUND`. HTTP path uses roles-first admin/teacher facts. |
| `/api/assessment/wrong-questions` | GET | Returns paginated wrong-question summaries with the same assessment list scope. Teacher must provide `courseId`; teacher unenrolled learner filter returns an empty page; admin missing course returns `NOT_FOUND`. HTTP path uses roles-first admin/teacher facts. |
| `/api/assessment/wrong-questions/{wrongQuestionId}` | GET | Returns wrong-question detail with the same assessment record scope. Response omits request/response snapshot internals. HTTP path uses roles-first admin/teacher facts. |

## RAG Index Task Detail

Related SPEC: `docs/specs/SPEC-20260606-rag-index-worker-progress.md`

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/index-tasks/{taskId}` | GET | Return RAG index task status, progress, phase, retry state, heartbeat, lease expiry, and safe error code after roles-first KB read permission passes; non-admin missing/foreign requests return `FORBIDDEN`, admin missing returns `NOT_FOUND`. |

## Recent Changes

### 2026-06-09 P3-4-X LearningPath Detail Roles-First RBAC

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/learning-paths/{pathId}` | Detail read now receives explicit admin facts from `UserContext.roles()` through `LearningPathController` and `LearningWorkflowService`; Bearer admin works despite spoofed `X-User-Id`, `USER sub=admin` role-confusion is denied, non-admin missing/foreign remains safe `FORBIDDEN`, and admin missing keeps `NOT_FOUND`. No API contract change. | `docs/specs/SPEC-20260609-p3-4-x-learning-path-detail-rbac.md` |

### 2026-06-09 P3-4-W CourseAccessService Legacy Overload Cleanup

| Scope | Change | Related SPEC |
|---|---|---|
| Backend authorization API surface | `CourseAccessService` removed legacy subject-name inference overloads for course read/manage/enrollment/list and removed `scopedCourseMissing(String)`, `isAdmin(String)`, and `isTeacherUser(String)`. Public course authorization entrypoints now require explicit roles-first facts. No REST API path, request DTO, response DTO, schema, or frontend contract change. | `docs/specs/SPEC-20260609-p3-4-w-course-access-legacy-cleanup.md` |

### 2026-06-09 P3-4-V ResourceGeneration / Agent Trace Detail Roles-First RBAC

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/resources/generation-tasks/{taskId}` | Task detail now receives explicit admin facts from `UserContext.roles()` through `ResourceGenerationController` and `ResourceGenerationService`; Bearer admin works despite spoofed `X-User-Id`, `USER sub=admin` role-confusion is denied, and admin missing keeps `NOT_FOUND`. No API contract change. | `docs/specs/SPEC-20260609-p3-4-v-resource-trace-detail-rbac.md` |
| `GET /api/agent/resource-generation/tasks/{taskId}/learner-resources` | Missing-object semantics now use explicit admin facts, while released learner resources remain owner-only. `USER sub=admin` missing requests return safe `FORBIDDEN`. No API contract change. | `docs/specs/SPEC-20260609-p3-4-v-resource-trace-detail-rbac.md` |
| `GET /api/agent/tasks/{taskId}/trace` | Trace detail now receives explicit admin facts from `UserContext.roles()`; Bearer admin can read existing foreign traces despite spoofed headers, while `USER sub=admin` is denied. No API contract change. | `docs/specs/SPEC-20260609-p3-4-v-resource-trace-detail-rbac.md` |
| `GET /api/agent/traces` | Trace search is now explicit-admin-only through `UserContext.roles()`; Bearer `USER sub=admin` no longer gains global trace search. No API contract change. | `docs/specs/SPEC-20260609-p3-4-v-resource-trace-detail-rbac.md` |

### 2026-06-09 P3-4-U Review Gate ResourceReview Roles-First RBAC

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/reviews/resources` | Review list now receives explicit admin/teacher facts from `UserContext.roles()` through `ResourceReviewController` and `ReviewGovernanceService`; Bearer admin works despite spoofed `X-User-Id`, Bearer teacher no-prefix sees only own-course reviews, and `USER sub=admin/teacher_1` role-confusion is denied. No API contract change. | `docs/specs/SPEC-20260609-p3-4-u-review-gate-rbac.md` |
| `POST /api/reviews/resources/{reviewId}/decision` | Review decision now uses roles-first facts; Bearer teacher can decide own-course review without a `teacher_` prefix, non-admin missing/foreign review remains safe `FORBIDDEN`, and subject-name role-confusion is denied. No API contract change. | `docs/specs/SPEC-20260609-p3-4-u-review-gate-rbac.md` |

### 2026-06-09 P3-4-T Orchestrator RESOURCE_GENERATION Create Roles-First RBAC

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/orchestrator/workflows` | `RESOURCE_GENERATION` create now receives explicit admin/teacher facts from `UserContext.roles()` through Orchestrator controller/service and ResourceGeneration workflow create. Bearer `USER sub=admin` cannot bypass course enrollment; Bearer admin/teacher still cannot create for another learner; Bearer token ignores spoofed `X-User-Id`. No API contract change. | `docs/specs/SPEC-20260609-p3-4-t-orchestrator-resource-create-rbac.md` |
| `POST /api/orchestrator/workflows/{workflowId}/retry` | Retry now uses the retry caller's roles-first facts and routes the new workflow through the roles-first create overload instead of the legacy subject-name inference path. No API contract change. | `docs/specs/SPEC-20260609-p3-4-t-orchestrator-resource-create-rbac.md` |

### 2026-06-09 P3-4-S LearningPath / ResourceGeneration Direct Create Roles-First RBAC

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/learning-paths` | Direct create now receives explicit admin/teacher facts from `UserContext.roles()` through `LearningPathController` and `LearningWorkflowService`; Bearer `ADMIN sub=ops_admin` works despite spoofed `X-User-Id`, while Bearer `USER sub=admin` cannot gain admin/enrollment-bypass semantics. No API contract change. | `docs/specs/SPEC-20260609-p3-4-s-learning-resource-create-rbac.md` |
| `POST /api/resources/generation-tasks` | Direct create now receives explicit role facts from `UserContext.roles()` while preserving owner-only semantics; admin/teacher cannot create for other learners, `USER sub=admin` cannot bypass course enrollment, and forbidden direct create leaves no task/resource/review/trace/model/token side effects. No API contract change. | `docs/specs/SPEC-20260609-p3-4-s-learning-resource-create-rbac.md` |

### 2026-06-09 P3-4-R Assessment / GradingEvaluation Roles-First RBAC

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/assessment/answers` | List scope now receives explicit admin/teacher facts from `UserContext.roles()` through `AssessmentController` and `AssessmentService`; Bearer `TEACHER` no-prefix works for own-course active-enrolled learners, while `USER sub=admin/teacher_1` role-confusion is denied. No API contract change. | `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md` |
| `GET /api/assessment/answers/{answerId}` | Detail scope now uses roles-first facts; Bearer `ADMIN` works despite spoofed `X-User-Id`, and non-admin missing/foreign anti-enumeration semantics are preserved. | `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md` |
| `GET /api/assessment/wrong-questions` | List scope now uses roles-first facts and preserves existing pagination/redaction semantics. | `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md` |
| `GET /api/assessment/wrong-questions/{wrongQuestionId}` | Detail scope now uses roles-first facts and preserves safe response redaction. | `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md` |
| `POST /api/assessment/grading-evaluations` | Grading evaluation now uses explicit `ADMIN` / `TEACHER` role facts and role-aware `CourseAccessService.requireCourseRead(...)`; Bearer admin/teacher subjects no longer depend on legacy `admin` or `teacher_*` names. | `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md` |

### 2026-06-09 P3-4-Q Analytics Student Summary Roles-First RBAC

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/analytics/students/{learnerId}/summary?courseId=...` | Course read now calls the role-aware `CourseAccessService` overload. Bearer `ADMIN sub=ops_admin` works despite spoofed `X-User-Id`; Bearer `TEACHER sub=instructor_1` works for own-course active-enrolled learner summaries without a `teacher_` prefix; Bearer `USER sub=teacher_1` role-confusion is denied; non-admin missing/foreign courses return safe `FORBIDDEN`. No API contract change. | `docs/specs/SPEC-20260609-p3-4-q-analytics-student-summary-rbac.md` |

### 2026-06-09 P3-4-P RAG KB Management Roles-First RBAC

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/knowledge-bases` | Controller now passes explicit role facts from `UserContext.roles()` to `KnowledgeBaseService`; no API/DTO/schema change and personal KB creation remains available. | `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md` |
| `GET /api/knowledge-bases` | Uses roles-first KB visibility. Bearer `ADMIN` can list all active KBs despite spoofed `X-User-Id`; subject-name role confusion does not elevate ordinary users. | `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md` |
| `POST /api/knowledge-bases/{kbId}/documents` | Uses roles-first KB write and role-aware course metadata scope. Bearer `TEACHER` no longer needs a `teacher_` subject prefix for own-course metadata; `USER sub=teacher_1` is denied. | `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md` |
| `GET /api/knowledge-bases/{kbId}/documents` | Uses roles-first KB read permission while preserving response DTOs. | `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md` |
| `GET /api/documents/{documentId}` | Missing/foreign anti-enumeration now depends on explicit admin role facts: admin missing returns `NOT_FOUND`, non-admin missing/foreign returns `FORBIDDEN`. | `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md` |
| `POST /api/documents/{documentId}/reindex` | Uses roles-first KB write permission and the same missing/foreign anti-enumeration semantics. | `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md` |
| `GET /api/index-tasks/{taskId}` | Uses roles-first KB read permission and the same missing/foreign anti-enumeration semantics. | `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md` |

### 2026-06-08 P3-4-J Analytics Student Summary Course Scope

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/analytics/students/{learnerId}/summary` | Added optional `courseId`. Student course-scoped reads require own learner plus active enrollment; teacher reads require `courseId`, own course, and active enrolled learner; admin can read global or course-scoped summaries. Course-scoped aggregation filters path, mastery, and wrong-question signals to the requested course. | `docs/specs/SPEC-20260608-analytics-student-summary-course-scope.md` |

### 2026-06-08 P3-4-I Real Auth Context / RBAC Compatibility

| Scope | Change | Related SPEC |
|---|---|---|
| Auth context | Bearer HS256 JWT now establishes `UserContext` from `sub`, optional `name`, and roles. | `docs/specs/SPEC-20260608-real-auth-rbac-context.md` |
| Auth fallback | Invalid Bearer tokens return `UNAUTHORIZED` and never fall back to `X-User-Id`; `prod` / `production` / `staging` no longer trust `X-User-Id`, while `dev` / `test` keep `X-User-Id` / `dev_user` only when Bearer is absent. | `docs/specs/SPEC-20260608-real-auth-rbac-context.md` |

### 2026-06-08 P3-4-H RAG Document Course/Chapter Metadata Scope

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/knowledge-bases/{kbId}/documents` | Non-empty `courseId` now requires centralized course read/manage authorization before requestId/hash/replay/storage/save/index side effects. Teachers can upload only own-course metadata; students cannot spoof course metadata; admin missing course returns `NOT_FOUND`; teacher missing/foreign course returns safe `FORBIDDEN` without `data`. | `docs/specs/SPEC-20260608-rag-document-course-scope.md` |
| `POST /api/knowledge-bases/{kbId}/documents` | Non-empty `chapterId` requires `courseId`; missing or foreign chapter returns generic `VALIDATION_ERROR` without exposing the offending id. RequestId replay and metadata conflict semantics remain unchanged. | `docs/specs/SPEC-20260608-rag-document-course-scope.md` |

### 2026-06-08 P3-4-G Grading Evaluation Course Scope

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/assessment/grading-evaluations` | Requires `courseId` for teacher/admin HTTP requests. Students are denied before course/sample validation; teachers can run only own-course evaluations; teacher missing/foreign course returns safe `FORBIDDEN`; admin missing course returns `NOT_FOUND`; legacy score arrays remain supported only with valid `courseId`. | `docs/specs/SPEC-20260608-grading-evaluation-course-scope.md` |
| `POST /api/assessment/grading-evaluations` | Added sample course consistency validation: non-blank `samples[].knowledgePointId` must belong to the request course; outside/missing knowledge points return generic `VALIDATION_ERROR` without exposing the offending id. | `docs/specs/SPEC-20260608-grading-evaluation-course-scope.md` |

### 2026-06-08 P3-4-F Assessment Record List RBAC / Pagination

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/assessment/answers` | Added safe paginated answer summary list with student owner-only, teacher required-course own-course active-enrollment learner, and admin global/filter read semantics. Response omits answer text, `requestId`, `requestHash`, `responseJson`, `payloadJson`, `gradingResultId`, `causeAnalysis`, and `replanRecordId`. | `docs/specs/SPEC-20260608-assessment-record-list-rbac.md` |
| `GET /api/assessment/wrong-questions` | Added safe paginated wrong-question summary list with the same assessment list scope. Teacher missing `courseId` returns `VALIDATION_ERROR`; teacher foreign/missing course returns `FORBIDDEN`; teacher unenrolled learner filter returns empty page; admin missing course returns `NOT_FOUND`. | `docs/specs/SPEC-20260608-assessment-record-list-rbac.md` |

### 2026-06-08 P3-4-E Assessment Record RBAC Matrix

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/assessment/answers/{answerId}` | Added safe answer detail API with student owner-only, teacher own-course active-enrollment learner, and admin global read semantics. Non-admin missing/foreign returns `FORBIDDEN` without `data`; response omits `requestId`, `requestHash`, and `responseJson`. | `docs/specs/SPEC-20260608-assessment-record-rbac.md` |
| `GET /api/assessment/wrong-questions/{wrongQuestionId}` | Added safe wrong-question detail API reusing assessment record scope; response omits internal payload/snapshot fields. | `docs/specs/SPEC-20260608-assessment-record-rbac.md` |

### 2026-06-08 P3-4-C Course Read And Grading Evaluation Permission

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/courses` | Added scoped list semantics: admin sees all courses, teacher sees own courses, student receives an empty list until class/enrollment exists. | `docs/specs/SPEC-20260608-rbac-course-class-answer-matrix.md` |
| `GET /api/courses/{courseId}` | Added service-layer read authorization; non-admin missing and foreign course reads both return safe `FORBIDDEN` without `data`. | `docs/specs/SPEC-20260608-rbac-course-class-answer-matrix.md` |
| `GET /api/courses/{courseId}/knowledge-graph` | Reuses course read authorization so foreign/missing graph reads for non-admins return safe `FORBIDDEN`. | `docs/specs/SPEC-20260608-rbac-course-class-answer-matrix.md` |
| `POST /api/assessment/grading-evaluations` | Added initial teacher/admin gate in P3-4-C; this endpoint was later tightened by P3-4-G to require `courseId` and enforce course-scoped teacher authorization. | `docs/specs/SPEC-20260608-rbac-course-class-answer-matrix.md`, `docs/specs/SPEC-20260608-grading-evaluation-course-scope.md` |

### 2026-06-06 Evaluation Set Management

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/evaluation-sets` | Added persistent evaluation set creation/replacement with service-layer sample validation and teacher/admin access checks. | `docs/specs/SPEC-20260606-evaluation-set-management.md` |
| `GET /api/evaluation-sets` | Added management list endpoint; list responses omit sample detail payloads. | `docs/specs/SPEC-20260606-evaluation-set-management.md` |
| `GET /api/evaluation-sets/{setId}` | Added detail endpoint returning structured sample fields for RAG, grading, and resource-generation evaluation sets. | `docs/specs/SPEC-20260606-evaluation-set-management.md` |

### 2026-06-06 Prompt Version Quality Comparison

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/evaluation-runs` | Added persisted evaluation run metric recording; `SUCCEEDED` runs require positive `sampleCount`, metric sample counts must be positive, and duplicate metric names are rejected. | `docs/specs/SPEC-20260606-prompt-version-quality-comparison.md` |
| `GET /api/evaluation-runs/comparison` | Added prompt-version comparison over succeeded runs only, with weighted metric aggregation, deltas, and winner by metric. | `docs/specs/SPEC-20260606-prompt-version-quality-comparison.md` |

### 2026-06-06 RAG And Grading Evaluation Metrics

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/rag/evaluations` | Added benchmark input and metrics for `Recall@K`, `Citation Accuracy`, `Groundedness`, `No-source Refusal Rate`, sample results, benchmark summary, and report text. | `docs/specs/SPEC-20260606-rag-quality-evaluation.md` |
| `POST /api/assessment/grading-evaluations` | Added structured grading samples, `gradeAgreementRate`, `wrongCauseAgreementRate`, and grouped analysis by question type, knowledge point, and rubric version. | `docs/specs/SPEC-20260606-grading-quality-evaluation.md` |

### 2026-06-06 Agent Trace Governance

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/agent/traces` | Added admin trace search filters by user, agent type, status, time range, and failure reason. | `docs/specs/SPEC-20260606-agent-trace-governance-dashboard.md` |
| `GET /api/agent/tasks/{taskId}/trace` | Detail responses now include sanitized `toolCalls` and `retentionPolicy`. | `docs/specs/SPEC-20260606-agent-trace-governance-dashboard.md` |

### 2026-06-06 Token / Cost Budget Governance

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/analytics/token-budget/governance` | Added admin analytics-layer governance query for Token / cost stats by user, course, Agent type, and time window; response includes budget decisions, high-cost task warnings, and abnormal model-call detection. | `docs/specs/SPEC-20260606-token-budget-governance.md` |

### 2026-06-07 Operations Alerting

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/analytics/ops/alerts` | Added admin-only query-time operations alert endpoint for `SLOW_RAG_QUERY`, `SLOW_MODEL_CALL`, `RAG_NO_SOURCE`, and `REVIEW_BACKLOG`; response returns default thresholds, safe aggregate metrics, severity, and reason codes without exposing prompt/question/raw response/model errors/generated markdown/review-private fields. | `docs/specs/SPEC-20260607-backend-architecture-completion.md` |

### 2026-06-06 Permission / Security Hardening

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/profile/dialogue/extract` | Cross-learner `learnerId` requests now return `FORBIDDEN` before profile persistence. | `docs/specs/SPEC-20260606-permission-security-hardening.md` |
| `POST /api/learning-paths` | Cross-learner path creation now returns `FORBIDDEN`. | `docs/specs/SPEC-20260606-permission-security-hardening.md` |
| `GET /api/learning-paths/{pathId}` | Non-owner path detail access now returns `FORBIDDEN` without exposing path data. | `docs/specs/SPEC-20260606-permission-security-hardening.md` |
| `GET /api/analytics/overview` | Overview is now limited to `admin`. | `docs/specs/SPEC-20260606-permission-security-hardening.md` |
| `GET /api/health` | Health output now omits raw JDBC URL, storage endpoint/bucket, and provider/model names. | `docs/specs/SPEC-20260606-permission-security-hardening.md` |
| `POST /api/rag/query` / `GET /api/rag/query` | Direct RAG query now strictly rejects mixed allowed/forbidden `kbIds` before retrieval/logging/citation writes. | `docs/specs/SPEC-20260606-permission-security-hardening.md` |

### 2026-06-06 RAG Index Worker Progress

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/index-tasks/{taskId}` | Added index task detail API with KB read permission checks and safe status/progress/retry/heartbeat/lease fields. | `docs/specs/SPEC-20260606-rag-index-worker-progress.md` |

### 2026-06-06 Model Call Prompt Metadata

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/analytics/overview` | No new fields exposed; analytics continues aggregating model/token data without returning prompt text, schema body, or provider error messages. | `docs/specs/SPEC-20260606-model-call-prompt-metadata.md` |
| `GET /api/agent/tasks/{taskId}/trace` | Failed model-call trace summaries now use safe `MODEL_PROVIDER_ERROR` instead of raw provider exception messages. | `docs/specs/SPEC-20260606-model-call-prompt-metadata.md` |

### 2026-06-06 Resource Generation Recovery State

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/resources/generation-tasks` | Response contract now includes `retryCount`, `nextRetryAt`, `lastError`, and `recoverable`; successful tasks default to non-recoverable state. | `docs/specs/SPEC-20260606-resource-generation-recovery-state.md` |
| `GET /api/resources/generation-tasks/{taskId}` | Failed recoverable model-generation tasks return persisted recovery metadata with safe `lastError = MODEL_CALL_FAILED`. | `docs/specs/SPEC-20260606-resource-generation-recovery-state.md` |

### 2026-06-06 Orchestrator Node Contract Policy

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/orchestrator/workflows` | Workflow `steps[]` now expose `inputDto`, `outputDto`, `failurePolicy`, `retryPolicy`, and `retryable`. | `docs/specs/SPEC-20260606-orchestrator-node-contract-policy.md` |
| `GET /api/orchestrator/workflows/{workflowId}` | `recentFailedStep` exposes the same node contract fields; failed `RAG_QA` and `ANSWER_SUBMISSION` now return `RESUBMIT_ORIGINAL_REQUEST` instead of unavailable endpoint retry. | `docs/specs/SPEC-20260606-orchestrator-node-contract-policy.md` |
| `POST /api/orchestrator/workflows/{workflowId}/retry` | Retry policy is now reflected in workflow step contract metadata; endpoint retry remains limited to failed `RESOURCE_GENERATION`. | `docs/specs/SPEC-20260606-orchestrator-node-contract-policy.md` |

### 2026-06-06 Resource Citation Governance

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/resources/generation-tasks` | Generated resources now return `citationSummary` values that distinguish `COURSE_RAG` from `NO_SOURCE`; task-level source citations are persisted under the response `traceId` when available. | `docs/specs/SPEC-20260606-resource-citation-governance.md` |
| `GET /api/resources/generation-tasks/{taskId}` | Stored generation task details preserve the same citation summary semantics and review-gated content visibility. | `docs/specs/SPEC-20260606-resource-citation-governance.md` |
| `GET /api/reviews/resources` | Initial Critic reviews can expose generated `citationCheck` evidence. | `docs/specs/SPEC-20260606-resource-citation-governance.md` |
| `POST /api/reviews/resources/{reviewId}/decision` | `NO_SOURCE` resources cannot be directly `APPROVED`; the endpoint returns `409 CONFLICT`. | `docs/specs/SPEC-20260606-resource-citation-governance.md` |

### 2026-06-06 Learner Profile Snapshot Context

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/profile/dialogue/extract` | Profile draft dimensions now include seven explicit fields and `lastEvidenceId`. | `docs/specs/SPEC-20260606-learner-profile-snapshot-context.md` |
| `POST /api/learning-paths` | Response now includes persisted `profileSnapshot`. | `docs/specs/SPEC-20260606-learner-profile-snapshot-context.md` |
| `GET /api/learning-paths/{pathId}` | Response returns the saved path `profileSnapshot`. | `docs/specs/SPEC-20260606-learner-profile-snapshot-context.md` |
| `POST /api/resources/generation-tasks` | Response now includes persisted `profileSnapshot`; same request replay preserves the original snapshot. | `docs/specs/SPEC-20260606-learner-profile-snapshot-context.md` |
| `GET /api/resources/generation-tasks/{taskId}` | Response returns the saved task `profileSnapshot`. | `docs/specs/SPEC-20260606-learner-profile-snapshot-context.md` |

### 2026-06-06 Learning Path Node Recommendation Metadata

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/learning-paths` | Path node responses now include `recommendationReason`, `estimatedDurationMinutes`, `resourceType`, and `assessmentBindingRelation`. | `docs/specs/SPEC-20260606-learning-path-node-recommendation-metadata.md` |
| `GET /api/learning-paths/{pathId}` | Persisted path nodes now restore the same recommendation metadata fields. | `docs/specs/SPEC-20260606-learning-path-node-recommendation-metadata.md` |

### 2026-06-06 Knowledge DAG Mastery Remediation Priority

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/learning-paths` | Course DAG path planning now prioritizes ready prerequisite nodes with mastery below `0.6` when they unlock downstream knowledge; existing node statuses remain `DONE`, `ACTIVE`, and `LOCKED`. | `docs/specs/SPEC-20260606-knowledge-mastery-threshold-remediation.md` |

### 2026-06-06 Knowledge Dependency Types

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/knowledge-dependencies` | `dependencyType` is validated and normalized to one of `PREREQUISITE`, `RELATED`, or `ADVANCED`; invalid values return `VALIDATION_ERROR`. | `docs/specs/SPEC-20260606-knowledge-dependency-types-path-planning.md` |
| `POST /api/learning-paths` | Course DAG path planning now treats only `PREREQUISITE` dependencies as prerequisite edges; `RELATED` and `ADVANCED` do not lock or reorder nodes. | `docs/specs/SPEC-20260606-knowledge-dependency-types-path-planning.md` |

### 2026-06-06 Teacher Class Analytics Summary

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/analytics/classes/{courseId}/summary` | Adds teacher-facing course analytics summary with weak knowledge points, wrong-cause distribution, resource completion, and pending review metadata; course teacher/admin access only. | `docs/specs/SPEC-20260606-teacher-class-analytics-summary.md` |

### 2026-06-07 Review Gate Course Scope Hardening

| Endpoint | Change | Related SPEC |
|---|---|---|
| `GET /api/reviews/resources` | 权限语义收口为 `admin` 全局可见，`teacher` 仅返回自己课程的 review；课程链路为 `ResourceGenerationTask.goalId -> Course.teacherId`。 | `docs/specs/SPEC-20260607-review-gate-course-scope-hardening.md` |
| `POST /api/reviews/resources/{reviewId}/decision` | `teacher` 只能处理自己课程的 review；非管理员对 missing review 和 foreign review 均返回安全 `FORBIDDEN`，避免对象存在性探测。 | `docs/specs/SPEC-20260607-review-gate-course-scope-hardening.md` |

### 2026-06-06 Review Gate State Model

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/reviews/resources/{reviewId}/decision` | 请求和响应支持 `reason`、`citationCheck`、`safetyCheck`、`revisionSuggestion`，`decision` 支持 `REJECTED` | `docs/specs/SPEC-20260606-review-gate-state-model.md` |
| `GET /api/reviews/resources` | Review summary 返回结构化审核字段和资源发布状态 | `docs/specs/SPEC-20260606-review-gate-state-model.md` |

### 2026-06-05 Orchestrator Workflow Query

| Endpoint | Change | Related SPEC |
|---|---|---|
| `POST /api/orchestrator/workflows` | 创建响应补充 `steps`、`recentFailedStep`、`traceSummary`、`nextActions` | `docs/specs/SPEC-20260605-orchestrator-workflow-query.md` |
| `GET /api/orchestrator/workflows/{workflowId}` | 新增 workflow 状态上下文查询，missing/inaccessible 返回 `NOT_FOUND` | `docs/specs/SPEC-20260605-orchestrator-workflow-query.md` |

| Date | Endpoint | Change | Related SPEC |
|---|---|---|---|
| — | — | — | — |
# 2026-06-08 P3-4-D Course Enrollment Scope

- No new REST endpoint was added.
- Existing course read APIs now have stricter student scope: `GET /api/courses`, `GET /api/courses/{courseId}`, and `GET /api/courses/{courseId}/knowledge-graph` use active course enrollment for students.
- Existing create APIs now reject unenrolled course-bound student requests: `POST /api/learning-paths` and `POST /api/resources/generation-tasks` return `FORBIDDEN` when `goalId` is an existing course and learner has no active enrollment.
- Existing analytics API `GET /api/analytics/classes/{courseId}/summary` now counts active enrolled learners only; legacy learning paths no longer define class membership.
- Verification passed with focused, adjacent, and full backend Maven tests.
