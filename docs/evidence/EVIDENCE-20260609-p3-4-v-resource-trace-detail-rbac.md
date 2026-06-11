# EVIDENCE - P3-4-V ResourceGeneration / Agent Trace detail roles-first RBAC

## 1. Scope

本证据覆盖：

- `GET /api/resources/generation-tasks/{taskId}`
- `GET /api/agent/resource-generation/tasks/{taskId}/learner-resources`
- `GET /api/agent/tasks/{taskId}/trace`
- `GET /api/agent/traces`

## 2. RED Evidence

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest#resourceGenerationDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+resourceGenerationDetailRejectsBearerUserSubjectAdminRoleConfusion+resourceGenerationDetailBearerAdminMissingTaskReturnsNotFound+learnerResourcesRejectsBearerUserSubjectAdminMissingTaskAsForbidden,AgentTraceControllerTest#traceGovernanceSearchUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+traceGovernanceSearchRejectsBearerUserSubjectAdminRoleConfusion+traceDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+traceDetailRejectsBearerUserSubjectAdminRoleConfusion+traceDetailBearerAdminMissingTaskReturnsNotFound test
```

结果：

```text
Tests run: 9, Failures: 9, Errors: 0, Skipped: 0
```

关键失败形态：

- Bearer `ADMIN sub=ops_admin` detail/search 被旧逻辑拒绝为 `403`。
- Bearer `USER sub=admin` detail/search 被旧逻辑放行为 `200`。
- Bearer admin missing detail 被旧逻辑返回 `403`，而不是 `404`。
- Bearer `USER sub=admin` missing learner-resources 被旧逻辑返回 `404`，暴露 admin 语义。

## 3. Implementation Evidence

改动摘要：

- `ResourceGenerationController` 的 task detail 与 learner-resources 现在读取 `currentUser()` 并传入 explicit `ADMIN` role fact。
- `AgentTraceController.trace(...)` 现在传入 explicit `ADMIN` role fact。
- `AgentTraceGovernanceController.search(...)` 现在传入 explicit `ADMIN` role fact。
- `ResourceGenerationService` 新增 roles-first `getTask(userId, currentUserAdmin, taskId)` 与 `getLearnerResources(userId, currentUserAdmin, taskId)` overload。
- `AgentTraceGovernanceService` 新增 roles-first `search(...)` 与 `getTrace(...)` overload。
- Legacy overload 保留兼容。

## 4. Verification Evidence

### Focused GREEN

命令同 RED focused command。

结果：

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Adjacent Regression

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest,AgentTraceControllerTest,ResourceReviewControllerTest,OrchestratorWorkflowControllerTest,AnalyticsControllerTest test
```

结果：

```text
Tests run: 108, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Surefire class summary after full run:

```text
ResourceGenerationControllerTest: Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
AgentTraceControllerTest: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
ResourceReviewControllerTest: Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
OrchestratorWorkflowControllerTest: Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
AnalyticsControllerTest: Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
```

### Full Backend

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 463, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. Limitations

- 未执行真实 MySQL migration smoke；本切片无 DB migration。
- 未执行 frontend build；本切片无 frontend 变更。
- 未引入 formal OAuth2/JWK/Spring Security；仍属后续 P3-4 工作。
