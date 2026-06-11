# RUN - 20260609 P3-4-S LearningPath / ResourceGeneration Integration Review

## 1. Verdict

CONDITIONAL PASS。

本切片在明确限定为以下两个直连 HTTP API 时可以验收：

- `POST /api/learning-paths`
- `POST /api/resources/generation-tasks`

如果验收口径扩大到 Orchestrator 发起的 `RESOURCE_GENERATION` 工作流创建，则本切片不能声明覆盖完成；该路径仍调用 legacy `createTaskInWorkflow(ownerUserId, request, context)`，需要后续单独切片迁移到 roles-first 调用。

## 2. Reviewed Scope

### 2.1 LearningPath direct create

- `LearningPathController.create(...)` 读取 `CurrentUserService.currentUser()`。
- Controller 仅从 `UserContext.roles()` 派生 `ADMIN` / `TEACHER` facts。
- `LearningWorkflowService.createPathForUser(currentUserId, currentUserAdmin, currentUserTeacher, request)` 使用 explicit `currentUserAdmin` 决定跨 learner 创建和 enrollment bypass。
- `USER sub=admin` 不会因 subject 字符串获得 admin 语义。

### 2.2 ResourceGeneration direct create

- `ResourceGenerationController.create(...)` 读取 `CurrentUserService.currentUser()`。
- Controller 仅从 `UserContext.roles()` 派生 role facts。
- `ResourceGenerationService.createTask(currentUserId, currentUserAdmin, currentUserTeacher, request)` 保持 owner-only。
- HTTP direct create 路径调用 course enrollment helper 时不允许 admin enrollment bypass。
- owner/enrollment 检查发生在 request replay、safety check、Agent run、task/resource/review/model/token 写入之前。

### 2.3 Course enrollment helper

- `CourseAccessService.requireLearnerEnrolledForExistingCourse(currentUserId, currentUserAdmin, learnerId, courseId)` 已提供 roles-first overload。
- 旧签名保留，兼容未迁移调用方。
- existing course 需要 active enrollment；明确允许 admin 代操作的调用方可传入 `currentUserAdmin=true` bypass。

## 3. Test Evidence Reviewed

| Evidence | Result |
|---|---|
| RED focused run | `Tests run: 32, Failures: 3, Errors: 0, Skipped: 0`, expected role-confusion failures observed before implementation |
| GREEN focused run | `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0` |
| Adjacent run | `Tests run: 91, Failures: 0, Errors: 0, Skipped: 0` |
| Full backend run | `Tests run: 446, Failures: 0, Errors: 0, Skipped: 1` |

Covered regression cases:

- Bearer `ADMIN sub=ops_admin` can create a course-bound learning path for another learner despite spoofed `X-User-Id`.
- Bearer `USER sub=admin` cannot create a learning path across learner/enrollment boundaries and leaves no path/node/event persistence.
- Bearer `ADMIN` cannot create a resource generation task for another learner.
- Bearer `USER sub=admin` cannot bypass resource generation course enrollment.
- Forbidden resource generation create leaves no durable side effects across task/resource/review/trace/model/token repositories.

## 4. Integration Risks

| Risk | Severity | Decision |
|---|---|---|
| `OrchestratorWorkflowService` still calls legacy `createTaskInWorkflow(ownerUserId, request, context)` for `RESOURCE_GENERATION` workflows. A Bearer `USER sub=admin` workflow may still reach the legacy `isAdmin(userId)` enrollment bypass if learner owner checks also pass. | Medium | Accepted as out-of-scope for P3-4-S direct API slice. Must be tracked as a follow-up roles-first Orchestrator slice before claiming full ResourceGeneration create coverage. |
| `ResourceGenerationService` roles-first direct overload intentionally ignores admin/teacher facts for代创建. | Low | Accepted. This preserves owner-only semantics and prevents privilege expansion. |
| Legacy overloads remain for compatibility. | Low / Medium | Accepted only where caller scope is documented. Future migrations should remove subject-name inference from remaining external entrypoints. |

## 5. Merge / Acceptance Recommendation

Accept P3-4-S as complete for the two direct HTTP create APIs.

Do not mark the broader P3-4 permission hardening item as complete. Recommended next security slice:

`P3-4-T Orchestrator RESOURCE_GENERATION roles-first create RBAC`

Expected follow-up:

- Pass `UserContext.roles()` facts through Orchestrator controller/service.
- Call roles-first `ResourceGenerationService.createTaskInWorkflow(...)`.
- Add RED/GREEN coverage for Bearer `USER sub=admin` workflow create role-confusion and zero/expected side-effect behavior.
