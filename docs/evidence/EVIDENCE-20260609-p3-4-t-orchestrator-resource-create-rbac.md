# EVIDENCE - P3-4-T Orchestrator RESOURCE_GENERATION create roles-first RBAC

## 1. Scope

本证据文档覆盖：

- `POST /api/orchestrator/workflows` 中 `workflowType=RESOURCE_GENERATION` 的 create 调用链。
- `POST /api/orchestrator/workflows/{workflowId}/retry` 的 retry 调用链。
- Orchestrator 到 ResourceGeneration workflow create 的 roles-first RBAC 传递。

不覆盖：

- ResourceGeneration direct create；该路径已由 P3-4-S 完成。
- ResourceGeneration detail / trace / cancel / review RBAC。
- broader class/course 权限矩阵。
- formal OAuth2/JWK/Spring Security 迁移。

## 2. Code Evidence

| Area | Evidence |
|---|---|
| Orchestrator controller create | `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java` reads `CurrentUserService.currentUser()` and derives `ADMIN` / `TEACHER` facts from `UserContext.roles()`. |
| Orchestrator controller retry | `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java` passes the same roles-first facts into retry. |
| Orchestrator service create overload | `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java` exposes `createWorkflow(ownerUserId, currentUserAdmin, currentUserTeacher, request)`. |
| Orchestrator service retry overload | `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java` exposes `retryWorkflow(ownerUserId, currentUserAdmin, currentUserTeacher, workflowId)` and routes retry through roles-first create. |
| ResourceGeneration workflow call | The `RESOURCE_GENERATION` branch calls `resourceGenerationService.createTaskInWorkflow(ownerUserId, currentUserAdmin, currentUserTeacher, request, context)`. |
| Regression tests | `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java` covers role-confusion denial, admin other-learner denial, and student owner success despite spoofed `X-User-Id`. |

## 3. RED Evidence

Command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest test
```

Observed pre-fix result:

```text
Tests run: 28, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

Expected RED failure:

- `resourceGenerationWorkflowRejectsBearerUserSubjectAdminRoleConfusionBeforeResourceSideEffects`
- Expected `403 FORBIDDEN`; actual old behavior returned `200 OK` and created a `RESOURCE_GENERATION` workflow, proving the legacy `sub=admin` role-confusion bypass existed.

## 4. GREEN Evidence

Focused command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest test
```

Result:

```text
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Adjacent command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,LearningWorkflowControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
```

Result:

```text
Tests run: 94, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full backend command:

```powershell
cd D:\多元agent\backend
mvn test
```

Result:

```text
Tests run: 449, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. Side-Effect Evidence

Forbidden `USER sub=admin` course-bound Orchestrator request now returns `FORBIDDEN` and does not persist ResourceGeneration business/model/cost/citation artifacts:

- no `resource_generation_task` for the request id
- no `learning_resource`
- no `resource_review`
- no `source_citation`
- no `model_call_log`
- no `token_usage_log`

The denied course-enrollment case is allowed to retain safe Orchestrator failure evidence:

- one `FAILED agent_task`
- `workflow_start`
- `step_runtime_failure`
- safe `FORBIDDEN` summary without leaking the course id or request id in the HTTP response

Owner mismatch for Bearer `ADMIN` creating for another learner is denied before workflow envelope creation and leaves no Orchestrator or ResourceGeneration durable side effects.

## 6. Integration Review Evidence

Integration review file:

- `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-integration-review.md`

Verdict:

- PASS for the P3-4-T Orchestrator `RESOURCE_GENERATION` create/retry scope.
- ResourceGeneration remains owner-only; admin/teacher代创建 is intentionally not added.
- broader class/course authorization and formal OAuth2/JWK/Spring Security remain out of scope.

## 7. Architecture Drift Evidence

| Check | Result |
|---|---|
| API path / DTO | No change |
| Database schema | No change |
| Dependencies | No change |
| Frontend | No change |
| Backend layering | Controller derives role facts; Service performs workflow orchestration and authorization propagation |
| Agent/RAG runtime | Orchestrator trace/failure evidence remains safe and bounded |
| Secrets | No secrets or credentials added |

## 8. Remaining Risk

P3-4-T closes the Orchestrator `RESOURCE_GENERATION` create/retry roles-first gap. P3-4 remains broader than this slice: class/course authorization expansion, formal OAuth2/JWK/Spring Security, and broader penetration tests remain follow-up work.
