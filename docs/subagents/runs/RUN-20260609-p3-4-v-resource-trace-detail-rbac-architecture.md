# RUN - P3-4-V Architecture Expert

## Scope

只读分析 ResourceGeneration task detail、learner-resources、Agent Trace detail/search/cancel 的 RBAC 边界。

## Findings

1. `ResourceGenerationController.get()` 只传 `currentUserService.currentUserId()`，没有传 `UserContext.roles()` 派生的 admin fact。
2. `ResourceGenerationService.getTask()` 使用 `ensureTaskOwnerOrAdmin(userId, task)`；该方法调用 `isAdmin(userId)`，而 `isAdmin()` 硬编码 `return "admin".equals(userId)`。
3. `learner-resources` 对 existing object 是 owner-only，但 missing object 分支复用 `loadGenerationTaskForDetail()`，仍受 legacy `sub == "admin"` 影响。
4. `AgentTraceController.trace()` 与 `AgentTraceGovernanceController.search()` 只传 current user id。
5. `AgentTraceGovernanceService.search()`、`getTrace()` 与 `scopedMissing()` 均使用 `currentUserId == "admin"` 推断管理员。
6. `cancel` 当前未发现 subject-name admin 放权；`AgentRunRecorder.cancelTask()` 仍是 owner-only。

## Recommended Slice Boundary

处理：

- `GET /api/resources/generation-tasks/{taskId}`
- `GET /api/agent/resource-generation/tasks/{taskId}/learner-resources`
- `GET /api/agent/tasks/{taskId}/trace`
- `GET /api/agent/traces`

不开放 admin cancel；不修改 create/review/model/RAG runtime。

## File Boundary

允许修改：

- `backend/src/main/java/com/learningos/agent/api/ResourceGenerationController.java`
- `backend/src/main/java/com/learningos/agent/api/AgentTraceController.java`
- `backend/src/main/java/com/learningos/agent/api/AgentTraceGovernanceController.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/agent/application/AgentTraceGovernanceService.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/AgentTraceControllerTest.java`

## Verification Recommendation

- Focused RED/GREEN for the new Bearer role tests.
- Adjacent controller/security regression set.
- Full backend `mvn test`.

## Status

Read-only report. No files were modified by the subagent.
