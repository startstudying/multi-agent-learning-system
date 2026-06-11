# RUN - 20260609 P3-4-S LearningPath / ResourceGeneration create RBAC Security Review

## 1. 结论

风险等级：HIGH。

风险类别：OWASP A01 Broken Access Control。

`POST /api/learning-paths` 与 `POST /api/resources/generation-tasks` 的 course-bound create 链路仍存在 subject-name role inference。Bearer `USER sub=admin` 可触发 legacy admin 语义，Bearer `ADMIN sub=ops_admin` 反而无法稳定获得 roles-first admin 语义。

## 2. 主要风险

### 2.1 LearningPath create subject-name admin bypass

当前 `LearningWorkflowService.createPathForUser(...)` 用 `isAdmin(currentUserId)` 判断是否可跨 learner 创建，并调用旧签名 enrollment helper。

风险：

- `USER sub=admin` 被当作 admin。
- 可以跨 learner owner check。
- 可以绕过 course-bound active enrollment。

修复要求：

- HTTP 主路径只从 `UserContext.roles()` 派生 `ADMIN`。
- `USER sub=admin` 必须视为普通用户。
- 非 admin 仍必须 owner-only。

### 2.2 ResourceGeneration course-bound enrollment bypass

ResourceGeneration create 本身保持 owner-only，这是正确边界。但当 `USER sub=admin` 且请求 `learnerId=admin` 时：

```text
ensureLearnerOwner(admin, admin) passes
-> legacy enrollment helper treats admin as admin
-> active enrollment bypass
```

修复要求：

- ResourceGeneration 不新增 admin/teacher 代创建。
- course-bound resource generation 仍要求 active enrollment。
- 拒绝必须发生在以下持久化副作用之前：
  - `agent_task`
  - `resource_generation_task`
  - `learning_resource`
  - `resource_review`
  - `model_call_log`
  - `token_usage_log`

## 3. 攻击矩阵

| 场景 | LearningPath | ResourceGeneration |
|---|---|---|
| Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` | 可为任意 learner 创建 course-bound path | 仍不能为其他 learner 创建 |
| Bearer `USER sub=admin` | 不能跨 learner；不能绕过 enrollment | 不能绕过 enrollment；无副作用 |
| Bearer `TEACHER sub=instructor_1` | 不新增 teacher 代创建 | 不新增 teacher 代创建 |
| active enrolled learner | 可创建 | 可创建 |
| dropped / missing enrollment | `FORBIDDEN`，无 `data` | `FORBIDDEN`，无持久化副作用 |

## 4. 必须保持的响应语义

- 非授权响应返回 `FORBIDDEN`，不返回 `data`。
- 响应体不暴露 foreign/missing `courseId`、`pathId`、`taskId`、`traceId`、title、markdownContent。
- 本切片不改变 API path、request DTO、response DTO。
- 不写入 secrets、API keys、raw token 或 provider 原始错误。

## 5. 修复建议

1. Controller 读取 `CurrentUserService.currentUser()`。
2. Controller 本地 `hasRole(UserContext, "ADMIN")` / `hasRole(UserContext, "TEACHER")`。
3. `LearningWorkflowService` 增加 roles-first create overload。
4. `ResourceGenerationService` 增加 roles-first create overload。
5. `CourseAccessService` 增加 roles-first enrollment overload。
6. ResourceGeneration 在 `agentRunRecorder.startRun(...)` 之前完成 owner/enrollment/safety 前的权限拒绝。

## 6. 非目标

- 不引入 Spring Security / OAuth2 / JWK。
- 不新增依赖。
- 不改 DB schema。
- 不改 frontend。
- 不改 Agent/RAG runtime。
- 不扩大 ResourceGeneration admin/teacher 代创建能力。

