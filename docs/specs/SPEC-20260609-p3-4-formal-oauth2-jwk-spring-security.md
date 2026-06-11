# SPEC-20260609 P3-4 子任务：formal OAuth2/JWK/Spring Security

## 设计概览

本切片引入 Spring Security OAuth2 Resource Server，新增安全配置和 JWT claim 到项目 `UserContext` 的桥接逻辑。业务 Controller 和 Service 继续通过 `CurrentUserService` 获取当前用户，不直接依赖 Spring Security 类型。

## 配置

扩展 `learning-os.auth`：

| Property | Default | Meaning |
|---|---|---|
| `issuer` | `learning-os` | 兼容既有 issuer；也用于本地 HS256 测试解码 |
| `jwt-secret` | empty | 仅 dev/test 或测试兼容用 HS256 decoder |
| `jwk-set-uri` | empty | 生产推荐 JWK Set URI |
| `audience` | empty | 可选 audience 校验 |
| `dev-header-fallback-enabled` | `true` | dev/test 无 Bearer 时是否允许 `X-User-Id` fallback |

本切片通过项目配置 `learning-os.auth.*` 显式创建 Spring Security `JwtDecoder`。不直接依赖自动绑定的 `spring.security.oauth2.resourceserver.jwt.*` 配置；如后续要接入 IdP discovery / `issuer-uri`，应另起配置兼容切片。

## SecurityFilterChain

- Bearer-only API 禁用 CSRF。
- prod/staging:
  - `/api/health/**`、`/actuator/health`、`/actuator/info` permitAll。
  - 其他请求 authenticated。
- dev/test:
  - 允许无 Bearer 请求进入旧 `X-User-Id` fallback。
  - 如果请求带 Bearer，Resource Server 必须处理 token；invalid token 401，不 fallback。
- OAuth2 Resource Server 使用自定义 JWT authentication converter。
- 401 / 403 使用项目 envelope entry point / access denied handler。

## JWT Decoder 策略

优先级：

1. 如果配置了 `jwk-set-uri`，使用 `NimbusJwtDecoder.withJwkSetUri(...)`。
2. 否则如果配置了 `jwt-secret`，使用 Spring Security `NimbusJwtDecoder.withSecretKey(...)` 作为本地测试/兼容路径；HS256 secret 必须至少 32 bytes。
3. 生产/预发如果两者都缺失，启动失败；不得回退到 header identity 或源码内固定 secret。

Issuer / audience：

- `issuer` 必须校验。
- `audience` 非空时必须校验。
- `exp`、`nbf` 等标准校验由 Spring Security JWT validators 处理。

## Role Claim Mapping

支持 claim：

- `roles`: `["ADMIN", "TEACHER"]`
- `scope` / `scp`: Spring 默认 scope 可保留为 authority，但项目角色必须仍来自白名单角色集合。

项目角色白名单：

- `ADMIN`
- `TEACHER`
- `STUDENT`
- `USER`

映射规则：

- trim + uppercase。
- 不认识的 role 忽略。
- 角色为空时默认为 `USER`。
- 不从 `sub`、`preferred_username`、`name` 推断角色。

## CurrentUserService

读取顺序：

1. Spring Security `SecurityContextHolder` 中已认证 JWT。
2. `UserContextHolder` 中 dev/test fallback context。
3. `dev_user` fallback，仅用于 dev/test 兼容路径。

## DevAuthFilter

职责收窄：

- 不再手写验证 Bearer token。
- 如果请求有 Bearer token，直接放行给 Spring Security。
- 只在 dev/test、无 Bearer、启用 fallback 时读取 `X-User-Id`。
- prod/staging 不建立 header identity。

## 错误响应

401:

```json
{
  "code": "UNAUTHORIZED",
  "message": "Unauthorized",
  "data": null
}
```

403:

```json
{
  "code": "FORBIDDEN",
  "message": "Forbidden",
  "data": null
}
```

## 不变项

- REST API path 不变。
- DTO 不变。
- DB schema 不变。
- 业务对象授权仍在 Service 层。
