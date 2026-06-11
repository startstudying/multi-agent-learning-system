# REQ-20260609 P3-4 子任务：formal OAuth2/JWK/Spring Security

## 功能需求

### REQ-1 Spring Security Resource Server

- 后端必须新增 Spring Security / OAuth2 Resource Server JWT 能力。
- 生产/预发环境中 Bearer token 必须由 Spring Security JWT 资源服务器验证。
- 支持 `issuer-uri` / `jwk-set-uri` 配置。
- 支持可选 audience 校验。

### REQ-2 当前用户上下文

- `CurrentUserService.currentUser()` 必须优先从 Spring Security `Authentication` 读取已验证身份。
- JWT `sub` 映射为 `UserContext.userId`。
- JWT `name` 映射为 `displayName`，缺失时使用 `sub`。
- JWT roles claim 映射为项目角色：`ADMIN`、`TEACHER`、`STUDENT`、`USER`。
- 不得从 `sub=admin` 或 `sub=teacher_1` 推断角色。

### REQ-3 dev/test fallback

- dev/test 在无 Bearer token 时可以继续使用 `X-User-Id` 兼容路径。
- dev/test 如果存在 Bearer token，必须先由 Spring Security 验证；invalid Bearer 不得 fallback。

### REQ-4 prod/staging 安全边界

- prod/staging 无 Bearer token 时必须返回 `UNAUTHORIZED`。
- prod/staging 不得信任 `X-User-Id`。
- valid Bearer + spoofed `X-User-Id` 必须使用 Bearer subject 和 roles。

### REQ-5 错误响应

- 401 / 403 响应必须使用项目 `ApiResponse` envelope。
- 错误响应不得包含 token、secret、JWK、signature、raw exception、issuer config。

### REQ-6 测试

- 必须覆盖 no token、invalid token、wrong issuer、expired token、Bearer spoofed header、subject-name role-confusion、role claim mapping。
- 必须保留至少一组业务 controller adjacent 回归，证明 roles-first 授权仍可工作。

## 非功能需求

- 不引入裸 Nimbus/JJWT/Auth0 直接依赖，优先使用 Spring Boot 管理依赖。
- 不改变业务 Service 授权语义。
- 不改变 API / DB / frontend 合同。

