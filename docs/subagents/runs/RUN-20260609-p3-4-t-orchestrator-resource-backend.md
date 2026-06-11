# RUN - P3-4-T Orchestrator RESOURCE_GENERATION Backend / Architect

## 结论

当前风险真实存在：`POST /api/orchestrator/workflows` 的 `RESOURCE_GENERATION` 分支只传 `currentUserId()`，没有传 Bearer `UserContext.roles()` 派生出的角色事实。随后 `OrchestratorWorkflowService` 调用 `ResourceGenerationService.createTaskInWorkflow(userId, request, context)` 旧入口，该入口通过 `isAdmin(userId)` 推断 admin，导致 `USER sub=admin` 可能绕过 course enrollment。

## 根因

- `OrchestratorWorkflowController.create(...)` 只调用 `currentUserService.currentUserId()`。
- `OrchestratorWorkflowService.createWorkflow(ownerUserId, request)` 没有 `currentUserAdmin/currentUserTeacher` 参数。
- `RESOURCE_GENERATION` 分支仍调用 legacy `createTaskInWorkflow(ownerUserId, request, context)`。
- legacy workflow create 入口会调用 `isAdmin("admin")`，形成 subject-name role confusion。

## 最小方案

1. Controller 读取 `UserContext currentUser = currentUserService.currentUser()`。
2. 从 `currentUser.roles()` 派生 `ADMIN` / `TEACHER` facts。
3. 新增 `OrchestratorWorkflowService.createWorkflow(ownerUserId, admin, teacher, request)`。
4. `RESOURCE_GENERATION` 分支调用已有 roles-first `resourceGenerationService.createTaskInWorkflow(ownerUserId, admin, teacher, request, context)`。
5. `retryWorkflow(...)` 同步新增 roles-first overload，避免 retry 重新走旧 create。

## 范围建议

不改 API、DTO、DB、依赖、前端。保留 ResourceGeneration owner-only 语义：即使 Bearer roles 包含 `ADMIN`，也不能通过 Orchestrator 为其他 learner 创建资源生成任务。

