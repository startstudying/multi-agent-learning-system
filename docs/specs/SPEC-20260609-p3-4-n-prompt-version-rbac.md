# SPEC-20260609 P3-4-N PromptVersion 管理 API RBAC 与 promptText 暴露收口

## 1. Scope

本规格只覆盖 PromptVersion HTTP 管理面的最小权限收口。

覆盖入口：

- `POST /api/agent/prompt-versions`
- `GET /api/agent/prompt-versions`
- `GET /api/agent/prompt-versions/{code}/{version}`

## 2. Current Gap

当前调用链：

```text
PromptVersionController.upsert(request)
-> promptVersionService.upsert(request)
-> promptVersionRepository.save(promptVersion)
```

问题：

- Controller 未注入 `CurrentUserService`。
- Service 未接收 role facts，也未做 admin/teacher gate。
- `PromptVersionResponse` 始终返回 `promptText`。
- Controller 测试允许无身份头创建和读取 prompt version。

## 3. Target Behavior

目标调用链：

```text
PromptVersionController.upsert(request)
-> currentUserService.currentUser()
-> promptVersionService.upsert(request, currentUserAdmin)
-> admin gate
-> save
-> PromptVersionResponse.from(promptVersion, includePromptText=true)
```

读取路径：

```text
PromptVersionController.list/get
-> currentUserService.currentUser()
-> promptVersionService.list/get(code, currentUserAdmin, currentUserTeacher)
-> admin/teacher gate
-> PromptVersionResponse.from(promptVersion, includePromptText=currentUserAdmin)
```

`currentUserAdmin/currentUserTeacher` 必须由 `UserContext.roles()` 计算，不能使用 `CurrentUserService.isAdmin()` / `isTeacherUser()`，以避免 test/dev legacy userId inference 影响 Bearer 场景。

## 4. API Contract

API path、method、request DTO 不变。

### POST `/api/agent/prompt-versions`

授权：

- `ADMIN`: allow。
- `TEACHER` / `STUDENT` / `USER`: `FORBIDDEN`。

响应：

- 成功返回完整 `PromptVersionResponse`，包含 `promptText`。

### GET `/api/agent/prompt-versions`

授权：

- `ADMIN`: allow，返回完整响应。
- `TEACHER`: allow，返回 metadata 响应，不包含 `promptText`。
- `STUDENT` / `USER`: `FORBIDDEN`。

### GET `/api/agent/prompt-versions/{code}/{version}`

授权同 list。

missing 语义：

- 已授权的 admin/teacher 查询 missing prompt version 返回 `NOT_FOUND`。
- 未授权用户先返回 `FORBIDDEN`，不执行详情查询。

## 5. DTO Contract

`PromptVersionResponse` 字段保持：

- `id`
- `code`
- `version`
- `promptText`
- `status`
- `createdAt`

新增响应生成策略：

```java
PromptVersionResponse.from(promptVersion, includePromptText)
```

当 `includePromptText == false` 时，`promptText` 为 `null`，并通过 JSON non-null serialization 从响应中省略。

## 6. Permission Rules

| Current roles | POST | GET list/detail | promptText |
|---|---|---|---|
| `ADMIN` | allow | allow | returned |
| `TEACHER` | deny | allow | omitted |
| `STUDENT` | deny | deny | no data |
| `USER` | deny | deny | no data |

Bearer 规则：

- Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` 使用 token role。
- Bearer `STUDENT sub=admin` 不获得 admin。
- Bearer `USER sub=teacher_1` 不获得 teacher。

dev/test 兼容：

- 无 Bearer 时，`DevAuthFilter` 可继续从 `X-User-Id=admin` / `teacher_1` 派生 roles。

## 7. Backend Workflow

1. Controller 读取 `UserContext currentUser = currentUserService.currentUser()`。
2. Controller 用 `hasRole(currentUser, "ADMIN")` 和 `hasRole(currentUser, "TEACHER")` 计算 role facts。
3. Controller 将 role facts 传入 `PromptVersionService`。
4. Service 在查询/写入前执行授权。
5. Service 根据 admin flag 决定是否返回 `promptText`。

## 8. Testing Strategy

Focused:

```powershell
cd backend
mvn --% -Dtest=PromptVersionControllerTest test
```

Adjacent:

```powershell
cd backend
mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest,DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
```

Full:

```powershell
cd backend
mvn test
```

重点测试：

- Bearer admin 可写且忽略 spoofed `X-User-Id`。
- Teacher 不可写。
- Student/User subject 看似 admin/teacher 也不可提权。
- Teacher list/detail 不返回 `promptText`。
- Student 不能读取。
- Admin missing detail 仍为 `NOT_FOUND`。

## 9. Architecture Drift Check

| Check | Expected |
|---|---|
| Controller only handles HTTP/current user extraction | PASS |
| Service owns permission and response redaction decision | PASS |
| Permission in backend code, not Prompt | PASS |
| No frontend change | PASS |
| No new dependency | PASS |
| No schema drift | PASS |

## 10. Out of Scope

- Evaluation Set/Run roles-first。
- GradingEvaluation roles-first。
- RAG KB management RBAC。
- 全仓库 legacy `CourseAccessService` 调用方迁移。
- Prompt 审批/发布/回滚工作流。
- formal OAuth2/JWK/Spring Security。

