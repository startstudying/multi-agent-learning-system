# ACCEPT-20260610 P3-4 子任务：RAG query runtime roles-first RBAC

## Acceptance Summary

Status: ACCEPTED.

P3-4 `RAG query runtime roles-first RBAC` 子任务已完成。`/api/rag/query`、Chat/Tutor runtime、Orchestrator `RAG_QA` replay precheck 与 query execution 均使用 explicit role facts。Legacy RAG query overload 保留但默认非 admin / 非 teacher，不再允许 literal `userId = "admin"` 获得运行时 KB read 语义。

## Acceptance Criteria

| Criteria | Status | Evidence |
|---|---|---|
| Bearer `ADMIN` runtime RAG query can use explicit admin facts despite spoofed `X-User-Id` | PASS | `ChatControllerTest`、`TutorControllerTest`、`OrchestratorWorkflowControllerTest.ragQaWorkflowUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`。 |
| Bearer `USER sub=admin` does not gain admin runtime RAG query semantics | PASS | `ChatControllerTest`、`RagQueryServiceTest`、`OrchestratorWorkflowControllerTest.ragQaWorkflowRejectsBearerUserSubjectAdminRoleConfusionBeforeQueryArtifacts`。 |
| `RagQueryService` role-aware query and requestId replay paths use role-aware permission filtering | PASS | `RagQueryService` calls `PermissionService.requireReadableKbIds(userId, admin, teacher, kbIds)` in normal query and replay context paths; focused service tests passed. |
| Orchestrator `RAG_QA` replay precheck and execution use same role facts | PASS | `OrchestratorWorkflowService` passes role facts into `replayQueryIfPresent(...)` and `queryWithTraceIdAndRequestId(...)`; Orchestrator focused tests passed. |
| Unauthorized runtime query does not write query/citation artifacts | PASS | Orchestrator subject-name role-confusion regression asserts zero `kb_query_log` / `source_citation`; existing RAG service tests cover denied query side effects. |
| No API/DTO/schema/dependency/frontend changes | PASS | Modified backend runtime/tests/docs only; no `pom.xml`, DB migration, REST contract, DTO, frontend, or secret files changed. |
| Expert subagent parallel development/review | PASS | `docs/subagents/runs/RUN-20260610-p3-4-rag-query-runtime-roles-first-rbac.md` records architect/test/security expert outputs and integration decision. |

## Verification

- Orchestrator focused: `mvn --% -Dtest=OrchestratorWorkflowControllerTest test` -> `30 run, 0 failures, 0 errors`.
- Runtime RBAC focused: `mvn --% -Dtest=OrchestratorWorkflowControllerTest,ChatControllerTest,TutorControllerTest,RagQueryServiceTest,PermissionServiceTest test` -> `60 run, 0 failures, 0 errors`.
- Adjacent permission/API: `mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,AssessmentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest test` -> `161 run, 0 failures, 0 errors`.
- Full backend: `mvn test` -> `509 run, 0 failures, 0 errors, 1 skipped`.

## Accepted Limitations / Follow-up

- P3-4 parent is not complete.
- KB-course binding schema / lifecycle governance remains a separate L-sized task.
- Broader class/course permission matrix and remaining business authorization expansion remain open.
- SSE production authentication transport strategy remains open.
- Dev/test legacy fallback cleanup remains open.
