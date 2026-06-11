# RUN-20260609 P3-4 formal OAuth2/JWK/Spring Security Security Follow-up

## Role

Security Reviewer.

## Scope

只读复核认证根边界、dev/test fallback、prod/staging fail-closed、401/403 envelope、roles-first bridge。

## Must Fix Findings

- `Authentication.isAuthenticated()` 会把 anonymous 视作已认证风险，prod/staging 不能仅依赖该布尔值。
- 缺少 JWK/secret 时不能回退到源码内固定本地 secret。
- `audience` 配置存在但未校验。
- HTTP 级测试缺 invalid Bearer、wrong issuer、expired、valid Bearer + spoofed header、subject-name role-confusion。

## Integration Result

主线已处理：

- production-like 请求必须是 `JwtAuthenticationToken` 才算认证通过。
- production/staging 缺少 `AUTH_JWK_SET_URI` 和 `AUTH_JWT_SECRET` 时启动失败。
- `audience` 非空时校验 JWT `aud` claim。
- `SecurityFilterChainTest` 覆盖 no token、invalid token、wrong issuer、expired token、valid Bearer ignores spoofed `X-User-Id`、`sub=admin roles=USER` 不提权。

## Remaining Follow-up

- SSE/EventSource 生产认证方案仍需单独切片。
- 全仓 dev/test legacy subject fallback 清理仍需独立计划。

