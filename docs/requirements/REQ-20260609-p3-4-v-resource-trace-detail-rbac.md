# REQ - P3-4-V ResourceGeneration / Agent Trace detail roles-first RBAC

## 功能需求

| ID | Requirement |
|---|---|
| R1 | `GET /api/resources/generation-tasks/{taskId}` 必须使用 Bearer `UserContext.roles()` 判断 admin，而不是从 `userId` 字符串推断。 |
| R2 | `GET /api/agent/resource-generation/tasks/{taskId}/learner-resources` 保持 owner-only，但 missing 分支的 admin/not-admin 语义必须来自 explicit role facts。 |
| R3 | `GET /api/agent/tasks/{taskId}/trace` 必须使用 explicit admin role 支持 admin detail read，并拒绝 `USER sub=admin`。 |
| R4 | `GET /api/agent/traces` 必须 admin-only，且 admin-only 判断来自 explicit `ADMIN` role。 |
| R5 | Bearer `ADMIN sub=ops_admin` 加 spoofed `X-User-Id` 时，detail/search 使用 token subject 与 token roles，不信任 spoofed header。 |
| R6 | Bearer `USER sub=admin` 不得获得 ResourceGeneration detail、Trace detail 或 Trace search 的 admin 语义。 |
| R7 | 非管理员 missing/foreign ResourceGeneration task 与 Trace detail 继续返回安全 `FORBIDDEN`，无 `data`，不泄露对象 id。 |
| R8 | Admin missing ResourceGeneration task / Trace detail 返回 `NOT_FOUND`，满足运维排障语义。 |

## 质量需求

- 不新增依赖。
- 不修改 API path、request DTO、response DTO。
- 不修改数据库 schema。
- 必须先观察 RED 测试失败，再实现。
- 必须运行 focused、adjacent、full backend Maven 验证，或记录无法运行的限制。
