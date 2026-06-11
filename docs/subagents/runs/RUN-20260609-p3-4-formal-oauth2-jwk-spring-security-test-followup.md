# RUN-20260609 P3-4 formal OAuth2/JWK/Spring Security Test Follow-up

## Role

Test Engineer / Verifier.

## Scope

复核 formal OAuth2/JWK/Spring Security 最小切片测试矩阵。

## Findings

原测试矩阵缺口：

- invalid Bearer 的 MockMvc/SecurityFilterChain 级 401。
- wrong issuer 的 401。
- expired token 的 401。
- valid Bearer + spoofed `X-User-Id` 使用 token subject/roles。
- `sub=admin roles=USER` 不能获得 admin 权限。
- JWK Set URI 分支不应要求本地 HS256 secret。
- TASK focused 命令应纳入 `SecurityFilterChainTest`。

## Integration Result

主线已处理：

- 新增/扩展 `SecurityFilterChainTest` 和 `SecurityJwtAuthenticationTest`。
- focused 命令更新为包含 `SecurityFilterChainTest`。
- full backend 暴露的 `@WebMvcTest` security auto-configuration 回归已通过排除安全自动配置修复，正式安全链仍由 `SecurityFilterChainTest` 覆盖。

## Verification Summary

- focused: `24 run, 0 failures, 0 errors`
- adjacent: `103 run, 0 failures, 0 errors`
- full backend: `497 run, 0 failures, 0 errors, 1 skipped`

