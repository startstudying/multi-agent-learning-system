# SPEC - P3-4-V ResourceGeneration / Agent Trace detail roles-first RBAC

## 1. 范围

目标接口：

- `GET /api/resources/generation-tasks/{taskId}`
- `GET /api/agent/resource-generation/tasks/{taskId}/learner-resources`
- `GET /api/agent/tasks/{taskId}/trace`
- `GET /api/agent/traces`

`POST /api/agent/tasks/{taskId}/cancel` 只做风险评估：当前仍是 owner-only，不新增 admin cancel 能力。

## 2. 授权模型

### 2.1 角色事实来源

HTTP 主路径只从 `CurrentUserService.currentUser().roles()` 派生：

```java
currentUserAdmin = roles contains ADMIN
```

不得在 HTTP 主路径中通过 `currentUserId == "admin"` 推断管理员身份。

### 2.2 ResourceGeneration task detail

| Case | Response |
|---|---|
| owner existing task | `200 OK` |
| explicit admin existing task | `200 OK` |
| non-admin foreign task | `FORBIDDEN`, no `data` |
| non-admin missing task | `FORBIDDEN`, no `data` |
| explicit admin missing task | `NOT_FOUND` |

### 2.3 learner-resources

`learner-resources` 保持 learner owner-only。Explicit admin 只影响 missing 分支的 `NOT_FOUND` 语义，不获得读取他人 learner resources 的能力。

### 2.4 Agent Trace detail/search

| Endpoint | Rule |
|---|---|
| `GET /api/agent/tasks/{taskId}/trace` | owner 或 explicit admin 可读；非 admin missing/foreign 返回 `FORBIDDEN`；admin missing 返回 `NOT_FOUND`。 |
| `GET /api/agent/traces` | explicit admin only；普通用户、teacher、`USER sub=admin` 均返回 `FORBIDDEN`。 |

## 3. Service API

新增 roles-first overload：

```java
getTask(String userId, boolean currentUserAdmin, String taskId)
getLearnerResources(String userId, boolean currentUserAdmin, String taskId)

search(String currentUserId, boolean currentUserAdmin, ...)
getTrace(String currentUserId, boolean currentUserAdmin, String taskId)
```

旧签名保留兼容，继续按 legacy inference 委托；HTTP Controller 不得调用旧签名。

## 4. 测试策略

新增或扩展 controller integration tests：

1. Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` 可读他人 ResourceGeneration detail。
2. Bearer `USER sub=admin` 读他人 ResourceGeneration detail 被拒。
3. Bearer admin missing ResourceGeneration detail 返回 `NOT_FOUND`。
4. Bearer `USER sub=admin` 访问 missing learner-resources 返回 `FORBIDDEN`。
5. Bearer admin 可 Trace search，spoofed header 无效。
6. Bearer `USER sub=admin` Trace search 被拒。
7. Bearer admin 可读他人 Trace detail。
8. Bearer `USER sub=admin` 读他人 Trace detail 被拒。
9. Bearer admin missing Trace detail 返回 `NOT_FOUND`。

## 5. Architecture Drift

| Check | Expected |
|---|---|
| Backend layering | Controller 只提取 auth facts，Service 执行业务授权。 |
| Frontend | 无变更。 |
| Agent/RAG | 不改变 Agent 执行、Trace 记录、RAG 或模型调用链路。 |
| Security | 权限由后端代码执行，不依赖 prompt 或 subject-name inference。 |
| API/DB | 无 path/DTO/schema 变更。 |
