# SPEC - P3-4-X LearningPath Detail Roles-First RBAC

## 1. Scope

目标接口：

```text
GET /api/learning-paths/{pathId}
```

目标代码：

- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`

## 2. Current Behavior

`LearningPathController.get(...)` 当前调用：

```java
learningWorkflowService.getPathForUser(currentUserService.currentUserId(), pathId)
```

`LearningWorkflowService.getPathForUser(String, String)` 通过本地 `isAdmin(currentUserId)` 判断 admin，其中 `isAdmin` 是 `"admin".equals(userId)`。

## 3. Target Behavior

Controller：

- 读取 `UserContext currentUser = currentUserService.currentUser()`。
- 从 `currentUser.roles()` 派生 `currentUserAdmin`。
- 调用 roles-first service overload。

Service：

- 使用 `currentUserAdmin` 控制 admin missing/foreign 语义。
- non-admin missing path 转换为 `FORBIDDEN`。
- non-admin foreign path 返回 `FORBIDDEN`。
- admin missing path 保留 `NOT_FOUND`。

## 4. API Contract

无 API contract 变更。

| Aspect | Change |
|---|---|
| Path | No |
| Method | No |
| Request DTO | No |
| Response DTO | No |
| Error envelope | No |
| DB schema | No |

## 5. Authorization Matrix

| Identity | Existing own path | Existing foreign path | Missing path |
|---|---:|---:|---:|
| Bearer `ADMIN` | `200 OK` | `200 OK` | `404 NOT_FOUND` |
| Bearer owner `USER/STUDENT` | `200 OK` | `403 FORBIDDEN` | `403 FORBIDDEN` |
| Bearer non-owner `USER/STUDENT` | `403 FORBIDDEN` | `403 FORBIDDEN` | `403 FORBIDDEN` |
| Bearer `USER sub=admin` | owner-only if path belongs to `admin`; otherwise `403 FORBIDDEN` | `403 FORBIDDEN` | `403 FORBIDDEN` |

## 6. Tests

Focused tests:

- `learningPathDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
- `learningPathDetailRejectsBearerUserSubjectAdminRoleConfusion`
- `learningPathDetailBearerAdminMissingPathReturnsNotFound`
- `learningPathDetailRejectsBearerUserSubjectAdminMissingPathAsForbidden`
- `learningPathDetailAllowsBearerOwnerDespiteSpoofedUserIdHeader`
- `learningPathDetailRejectsBearerNonOwnerForeignPathAsSafeForbidden`
- `learningPathDetailRejectsBearerNonOwnerMissingPathAsSafeForbidden`

## 7. Architecture Drift

Expected: no drift.

- Controller remains HTTP/context extraction only.
- Service remains object authorization owner.
- No frontend, Agent/RAG/model, DB, dependency changes.

