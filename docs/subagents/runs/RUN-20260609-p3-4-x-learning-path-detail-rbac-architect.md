# RUN - P3-4-X LearningPath Detail Roles-First RBAC Architect

## 1. 结论

`GET /api/learning-paths/{pathId}` 仍存在 subject-name admin 推断。

当前 `POST /api/learning-paths` 已从 `UserContext.roles()` 派生 explicit role facts，但 `GET` 仍只传 `currentUserId` 到 `LearningWorkflowService.getPathForUser(String, String)`，服务层再通过 `"admin".equals(currentUserId)` 判断管理员。这会导致：

- Bearer `ADMIN sub=ops_admin` 无法获得 admin detail 语义。
- Bearer `USER sub=admin` 被错误当作 admin，可读取 foreign path，并对 missing path 得到 admin-only `NOT_FOUND`。

## 2. 当前调用链与风险

| Step | Evidence | Risk |
|---|---|---|
| Controller create | `LearningPathController.create(...)` 读取 `currentUserService.currentUser()` 并传入 `hasRole(..., "ADMIN")` / `hasRole(..., "TEACHER")` | 已 roles-first |
| Controller get | `LearningPathController.get(...)` 调用 `learningWorkflowService.getPathForUser(currentUserService.currentUserId(), pathId)` | role facts 丢失 |
| Service get | `LearningWorkflowService.getPathForUser(String currentUserId, String pathId)` 用 `isAdmin(currentUserId)` 控制 missing/foreign 分支 | subject-name role inference |
| Local helper | `isAdmin(String userId)` 返回 `"admin".equals(userId)` | Bearer `USER sub=admin` 角色混淆 |

## 3. 推荐代码改动

1. `LearningPathController.get(...)` 改为读取完整 `UserContext`。
2. 新增 roles-first service overload：

```java
getPathForUser(String currentUserId, boolean currentUserAdmin, String pathId)
```

3. `GET` HTTP 主路径只调用 roles-first overload。
4. 旧 `getPathForUser(String, String)` 如保留，应默认 non-admin 或后续单独 legacy cleanup；不得继续作为 HTTP 主路径。

## 4. 不应触碰范围

- 不改 `POST /api/learning-paths` 代建语义。
- 不改 ResourceGeneration、Agent Trace、Review Gate、RAG、model provider。
- 不改 `CurrentUserService` / `DevAuthFilter`。
- 不改 DB migration、frontend、formal OAuth2/JWK/Spring Security。
- 不把 P3-4 总项标完成。

## 5. 必测点

- Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` 可读 foreign learning path detail。
- Bearer `ADMIN sub=ops_admin` missing path 返回 `NOT_FOUND`。
- Bearer `USER sub=admin` foreign path 返回 `FORBIDDEN` 且无 `data`。
- Bearer `USER sub=admin` missing path 返回 `FORBIDDEN` 且无 `data`。
- Bearer owner + spoofed header 仍可读 own path。
- Bearer non-owner foreign/missing 均安全 `FORBIDDEN`。

## 6. P3-4 剩余项

- broader class/course authorization matrix。
- formal OAuth2/JWK/Spring Security。
- broader permission penetration tests。

