# Auth Context Boundary

## 使用场景

当实现或审查后端认证上下文、Bearer token 校验、`UserContext` 建立、开发环境身份兼容、admin/teacher/student role 判定、header spoofing 防护时使用。

## 核心规则

1. 生产环境不得信任客户端可伪造身份头，例如 `X-User-Id`。
2. 如果请求提供 `Authorization: Bearer <token>`，所有环境都必须校验 token；invalid token 不得 fallback 到开发身份头。
3. `prod` / `production` / `staging` 缺少 Bearer token 时应返回统一 `UNAUTHORIZED`，且不能把 Spring Security anonymous authentication 当成业务认证身份。
4. `dev` / `test` 可以保留 `X-User-Id` 兼容路径，但只能作为无 Bearer token 时的 fallback。
5. `CurrentUserService.isAdmin()` / `isTeacherUser()` 应优先使用 roles；仅 dev/test 兼容模式允许旧 userId 字符串推断。
6. 认证失败响应不得包含 token 原文、secret、签名片段、raw exception 或配置值。
7. 认证层只负责建立可信 `UserContext`；对象级权限仍应留在业务 Service 层。
8. formal Spring Security Resource Server 场景下，业务认证身份应来自 `JwtAuthenticationToken`，而不是 header identity 或 anonymous principal。
9. `jwk-set-uri` 是生产推荐路径；HS256 `jwt-secret` 仅作为本地/测试兼容路径，且必须至少 32 bytes。
10. `audience` 非空时必须校验 JWT `aud` claim。
11. Runtime service calls that need role semantics must receive explicit role facts from `UserContext.roles()`; do not infer admin/teacher from literal `userId` values such as `admin` or `teacher_*`.

## 推荐测试矩阵

- dev/test 无 token + `X-User-Id`：使用 header 身份。
- dev/test 无 token + 无 header：使用 `dev_user`。
- dev/test valid token + spoofed `X-User-Id`：使用 token 身份。
- dev/test invalid token + spoofed `X-User-Id`：返回 `UNAUTHORIZED`，不 fallback。
- prod/staging 无 token + 任意 `X-User-Id`：返回 `UNAUTHORIZED`。
- prod/staging valid token + spoofed `X-User-Id`：使用 token 身份。
- roles 包含 `ADMIN` / `TEACHER` 时驱动权限判断。
- non-dev 环境不再通过 `admin` / `teacher_*` userId 字符串推断角色。
- Bearer `ADMIN` + spoofed `X-User-Id`：runtime service sees token subject and `admin=true`.
- Bearer `USER sub=admin`：runtime service sees `admin=false` and foreign private object access is denied.

## 已知边界

- 当前项目已完成最小 formal OAuth2/JWK/Spring Security 认证根边界：Bearer 由 Spring Security Resource Server 处理，`DevAuthFilter` 不再手写验证 Bearer。
- 第三方 IdP discovery / `issuer-uri` 自动配置兼容、SSE 生产认证传递、全仓 dev/test legacy subject fallback 清理仍属于后续切片。
- 不应在内存文件、测试日志或示例中保存真实 secret；测试 secret 必须是固定假值。

# Runtime Role-Fact Propagation Pattern

- Controller should call `CurrentUserService.currentUser()` once per request and derive booleans from `UserContext.roles()`.
- Application services should expose role-aware overloads when object authorization needs admin/teacher semantics.
- Legacy overloads may remain for compatibility, but must delegate with `admin=false` / `teacher=false` unless they are explicitly dev/test compatibility helpers.
- Replay/idempotency prechecks must use the same role facts as the actual execution path; otherwise stale response replay can become an authorization bypass.
- Tests should cover both explicit-role success and subject-name role-confusion denial for every runtime path, including Orchestrator/workflow wrappers.
