# EVIDENCE - P3-4-X LearningPath Detail Roles-First RBAC

## 1. Scope

本证据覆盖 `GET /api/learning-paths/{pathId}` detail 读取的 roles-first RBAC 收口：

- HTTP 主路径不再通过 `currentUserId == "admin"` 推断管理员。
- Controller 从 `UserContext.roles()` 派生 explicit `ADMIN` fact。
- Service 使用 explicit admin fact 决定 admin foreign/missing 与 non-admin anti-enumeration 语义。
- 不修改 REST API path、request DTO、response DTO、schema、dependency、frontend、formal OAuth2/JWK/Spring Security。

## 2. RED Evidence

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest#learningPathDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+learningPathDetailRejectsBearerUserSubjectAdminRoleConfusion+learningPathDetailBearerAdminMissingPathReturnsNotFound+learningPathDetailRejectsBearerUserSubjectAdminMissingPathAsForbidden+learningPathDetailAllowsBearerOwnerDespiteSpoofedUserIdHeader+learningPathDetailRejectsBearerNonOwnerForeignPathAsSafeForbidden+learningPathDetailRejectsBearerNonOwnerMissingPathAsSafeForbidden test
```

结果：

```text
Tests run: 7, Failures: 4, Errors: 0, Skipped: 0
```

关键失败形态：

- Bearer `ADMIN sub=ops_admin` 读取 foreign path 期望 `200 OK`，实际 `403 FORBIDDEN`。
- Bearer `ADMIN sub=ops_admin` 读取 missing path 期望 `404 NOT_FOUND`，实际 `403 FORBIDDEN`。
- Bearer `USER sub=admin` 读取 foreign path 期望 `403 FORBIDDEN`，实际 `200 OK`。
- Bearer `USER sub=admin` 读取 missing path 期望 `403 FORBIDDEN`，实际 `404 NOT_FOUND`。

## 3. Implementation Evidence

改动摘要：

- `LearningPathController.get(...)` 改为读取 `UserContext currentUser = currentUserService.currentUser()`。
- `LearningPathController.get(...)` 调用 `learningWorkflowService.getPathForUser(currentUser.userId(), hasRole(currentUser, "ADMIN"), pathId)`。
- `LearningWorkflowService` 新增 roles-first overload：

```java
public LearningPathResponse getPathForUser(String currentUserId, boolean currentUserAdmin, String pathId)
```

- `LearningWorkflowService.getPathForUser(String currentUserId, String pathId)` 保留为兼容入口，并委托为 `currentUserAdmin = false`，避免 detail legacy 调用继续获得 subject-name admin 语义。
- 本切片未清理 `createPathForUser(String, CreateLearningPathRequest)` 的 legacy `isAdmin(currentUserId)`，该 cleanup 仍属后续独立切片。

## 4. Verification Evidence

### Focused GREEN

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest#learningPathDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+learningPathDetailRejectsBearerUserSubjectAdminRoleConfusion+learningPathDetailBearerAdminMissingPathReturnsNotFound+learningPathDetailRejectsBearerUserSubjectAdminMissingPathAsForbidden+learningPathDetailAllowsBearerOwnerDespiteSpoofedUserIdHeader+learningPathDetailRejectsBearerNonOwnerForeignPathAsSafeForbidden+learningPathDetailRejectsBearerNonOwnerMissingPathAsSafeForbidden test
```

结果：

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

本轮复验时间：

```text
Finished at: 2026-06-09T22:05:00+08:00
```

### LearningWorkflow Controller Regression

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest test
```

结果：

```text
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
```

### Adjacent Regression

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,AgentTraceControllerTest,LearningWorkflowServiceTest test
```

结果：

```text
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
```

### Full Backend

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 474, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. Limitations

- 未执行真实 MySQL migration smoke；本切片无 DB migration。
- 未执行 frontend build；本切片无 frontend 变更。
- 未引入 formal OAuth2/JWK/Spring Security；仍属后续 P3-4 工作。
- 未扩展 broader class/course 权限矩阵。
- 未清理 `LearningWorkflowService.createPathForUser(String currentUserId, CreateLearningPathRequest request)` legacy subject-name helper。
