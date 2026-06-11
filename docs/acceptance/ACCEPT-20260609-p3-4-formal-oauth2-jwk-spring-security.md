# ACCEPT-20260609 P3-4 子任务：formal OAuth2/JWK/Spring Security

## Acceptance Summary

Status: ACCEPTED.

P3-4 formal OAuth2/JWK/Spring Security 最小生产化认证边界切片已完成并通过 focused、adjacent、full backend 验证。

## Acceptance Criteria

| Criteria | Status | Evidence |
|---|---|---|
| 引入 Spring Security / OAuth2 Resource Server 依赖 | PASS | `backend/pom.xml` 新增 `spring-boot-starter-security`、`spring-boot-starter-oauth2-resource-server`、`spring-security-test`。 |
| 生产/预发 Bearer JWT 由 Spring Security Resource Server 处理 | PASS | `SecurityConfig` 配置 `oauth2ResourceServer().jwt(...)`；`DevAuthFilter` 不再手写验证 Bearer。 |
| 支持 JWK Set URI | PASS | `SecurityConfig.jwtDecoder(...)` 优先 `NimbusJwtDecoder.withJwkSetUri(...)`；测试覆盖 JWK 分支无需本地 secret。 |
| 保留本地 HS256 兼容路径 | PASS | `jwt-secret` 非空时使用 `NimbusJwtDecoder.withSecretKey(...)`；HS256 secret 至少 32 bytes。 |
| production-like 缺少认证材料 fail-fast | PASS | `SecurityJwtAuthenticationTest.securityConfigFailsFastInProductionWhenAuthMaterialIsMissing` 覆盖 prod/production/staging 缺少 JWK/secret 时启动/decoder 失败。 |
| issuer 校验 | PASS | `JwtValidators.createDefaultWithIssuer(...)`。 |
| audience 可选校验 | PASS | `SecurityFilterChainTest.productionWrongAudienceBearerTokenIsUnauthorized` 覆盖 `learning-os.auth.audience` 非空时错误 `aud` 返回 401。 |
| `CurrentUserService` 优先从 Spring Security JWT 构建 `UserContext` | PASS | `SecurityJwtAuthenticationTest` 覆盖。 |
| roles 白名单，不从 subject 推断角色 | PASS | `SecurityJwtAuthenticationTest` 与 `SecurityFilterChainTest` 覆盖 `sub=admin roles=USER` / `sub=teacher_1 roles=USER` 不提权。 |
| dev/test 无 Bearer 可使用 `X-User-Id` fallback | PASS | `DevAuthFilterTest` 覆盖。 |
| prod/staging 不信任 `X-User-Id` | PASS | `SecurityFilterChainTest` 覆盖 no token / invalid Bearer + spoofed header。 |
| production 无认证上下文不落 `dev_user` | PASS | `CurrentUserServiceTest.productionDoesNotFallbackToDevUserWhenNoContextIsAvailable` 覆盖生产兜底身份为 `unauthenticated`。 |
| valid Bearer 忽略 spoofed `X-User-Id` | PASS | `SecurityFilterChainTest.productionValidBearerTokenUsesJwtRolesAndIgnoresSpoofedUserHeader`。 |
| 401 / 403 使用统一安全 envelope | PASS | `ApiAuthenticationEntryPoint` / `ApiAccessDeniedHandler` 与 MockMvc 测试覆盖。 |
| 不改变 REST API / DTO / DB / frontend | PASS | 本切片仅修改 backend auth/config/tests/docs，无 schema/frontend/API contract 变更。 |
| 专家 subagent 并行开发/审查 | PASS | 已落盘 architect/security/test/integration 初始报告和 follow-up 报告。 |
| full backend verification | PASS | `mvn test` -> `500 run, 0 failures, 0 errors, 1 skipped`。 |

## Verification

- Focused: `27 run, 0 failures, 0 errors`
- Adjacent: `106 run, 0 failures, 0 errors`
- Regression slice: `11 run, 0 failures, 0 errors`
- Full backend: `500 run, 0 failures, 0 errors, 1 skipped`

## Accepted Limitations / Follow-up

- P3-4 父项不标完成；broader class/course authorization matrix 和部分后续权限扩展仍开放。
- SSE/EventSource 生产认证传递方案未在本切片解决，应另起任务。
- 完整第三方 IdP discovery / `issuer-uri` 自动配置兼容未在本切片实现；当前使用项目 `learning-os.auth.*` 明确创建 `JwtDecoder`。
- 全仓 dev/test legacy subject fallback 清理不是本切片目标。
