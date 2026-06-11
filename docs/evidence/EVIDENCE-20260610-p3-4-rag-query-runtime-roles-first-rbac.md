# EVIDENCE-20260610 P3-4 子任务：RAG query runtime roles-first RBAC

## Scope

本证据记录 P3-4 子任务 `RAG query runtime roles-first RBAC` 的实现、测试和专家复核结果。

目标：让 RAG query runtime 使用认证上下文中的 explicit role facts，而不是从 `userId` 字符串推断 admin/teacher。

## Implementation Evidence

| Area | Evidence |
|---|---|
| `/api/rag/query` POST / GET | `backend/src/main/java/com/learningos/rag/api/ChatController.java` 读取 `CurrentUserService.currentUser()`，将 `UserContext.roles()` 派生的 `ADMIN` / `TEACHER` facts 传给 `RagQueryService`。 |
| Chat SSE | `backend/src/main/java/com/learningos/rag/api/ChatController.java` stream 分支同样传 role facts。 |
| Tutor ask / stream | `backend/src/main/java/com/learningos/tutor/api/TutorController.java` 传入 role-aware RAG query facts。 |
| RAG service overloads | `backend/src/main/java/com/learningos/rag/application/RagQueryService.java` 新增 role-aware `query` / `queryWithTraceId` / `queryWithRequestId` / `queryWithTraceIdAndRequestId` / `replayQueryIfPresent` overload。 |
| Legacy safety default | `RagQueryService` legacy overload 保留，但统一 delegate `currentUserAdmin=false` / `currentUserTeacher=false`。 |
| Permission filtering | `RagQueryService` 普通 query 与 requestId replay context 均调用 `PermissionService.requireReadableKbIds(userId, admin, teacher, kbIds)`。 |
| Orchestrator `RAG_QA` | `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java` replay precheck 与实际 query 均传入同一组 role facts。 |

## Test Evidence

| Test Area | Evidence |
|---|---|
| ChatController Bearer admin | `backend/src/test/java/com/learningos/rag/api/ChatControllerTest.java` 覆盖 Bearer `ADMIN sub=ops_admin` 忽略 spoofed `X-User-Id` 并传 `admin=true`。 |
| ChatController subject-name denial | `ChatControllerTest` 覆盖 Bearer `USER sub=admin` 不传 admin facts。 |
| ChatController requestId path | `ChatControllerTest` 覆盖 requestId 分支也传 role facts。 |
| TutorController | `backend/src/test/java/com/learningos/tutor/api/TutorControllerTest.java` 覆盖 tutor ask 使用 Bearer admin role facts 并忽略 spoofed header。 |
| RagQueryService | `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java` 覆盖 role-aware admin 可 query foreign private KB、legacy literal `admin` 不可 query foreign private KB、role-aware requestId replay 不重复写 artifacts。 |
| Orchestrator `RAG_QA` admin role | `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java` 新增 `ragQaWorkflowUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`。 |
| Orchestrator `RAG_QA` role-confusion denial | `OrchestratorWorkflowControllerTest` 新增 `ragQaWorkflowRejectsBearerUserSubjectAdminRoleConfusionBeforeQueryArtifacts`，断言 403 且无 query/citation side effects。 |

## RED / GREEN Evidence

### RED

本任务实现阶段曾先加入 controller/service 期望 role-aware overload 的测试，运行：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ChatControllerTest,TutorControllerTest,RagQueryServiceTest test
```

结果为预期 `testCompile` RED：缺少 role-aware overload，出现 19 个编译错误。

### GREEN - Focused Orchestrator

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest test
```

Result:

```text
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### GREEN - Runtime RBAC Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest,ChatControllerTest,TutorControllerTest,RagQueryServiceTest,PermissionServiceTest test
```

Result:

```text
Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### GREEN - Adjacent Permission/API

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,AssessmentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest test
```

Result:

```text
Tests run: 161, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### GREEN - Full Backend

```powershell
cd D:\多元agent\backend
mvn test
```

Result:

```text
Tests run: 509, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## Expert Review Evidence

Full report: `docs/subagents/runs/RUN-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`.

| Expert | Verdict | Summary |
|---|---|---|
| Agent/RAG Architect | CONDITIONAL PASS | Runtime RBAC slice is valid M scope; KB-course binding lifecycle remains L follow-up. |
| Test Engineer | PASS after coverage | Orchestrator `RAG_QA` Bearer admin and subject-name role-confusion tests were the missing regression and are now added. |
| Security Reviewer | PASS | No runtime RAG query entrance was found missing explicit role facts; no `userId == "admin"` spoof escalation path found in this scope. |

## Architecture Drift Check After

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controllers derive identity/role facts only; RAG service and permission service enforce authorization. |
| Frontend | PASS | No frontend changes. |
| Agent / RAG | PASS | Permission filtering remains before retrieval, query log, and source citation writes. |
| Security | PASS | Permission remains backend-code based; no Prompt permission; no secrets added. |
| API / Database | PASS | No API path, DTO, schema, migration, or dependency change. |

## Accepted Follow-up

- KB-course binding schema / lifecycle governance remains open as a separate L-sized P3-4 task.
- Broader class/course permission matrix follow-up remains open.
- SSE production auth transport strategy remains open.
- Full dev/test legacy fallback cleanup remains open.
- Dependency-check was not required because no dependency changed; security reviewer notes an optional CI rerun after network stabilizes.

## Acceptance Verdict

Verdict: PASS for this M-sized child task.

P3-4 parent remains open.
