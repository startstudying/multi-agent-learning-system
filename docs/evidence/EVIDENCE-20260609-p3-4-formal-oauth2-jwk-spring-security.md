# EVIDENCE-20260609 P3-4 子任务：formal OAuth2/JWK/Spring Security

## Scope

本证据记录 P3-4 formal OAuth2/JWK/Spring Security 最小生产化认证边界切片。

## Implementation Evidence

- `backend/pom.xml`
  - 新增 `spring-boot-starter-security`。
  - 新增 `spring-boot-starter-oauth2-resource-server`。
  - 新增 test scope `spring-security-test`。
- `backend/src/main/java/com/learningos/common/auth/SecurityConfig.java`
  - 新增 Spring Security `SecurityFilterChain`。
  - API 使用 stateless session，关闭 CSRF。
  - `/api/health/**`、`/actuator/health`、`/actuator/info` permitAll。
  - production-like 环境下除 health/info 外只接受 `JwtAuthenticationToken`，不接受 anonymous authentication。
  - `JwtDecoder` 优先使用 `learning-os.auth.jwk-set-uri`，否则使用本地 HS256 secret 兼容路径。
  - production-like 环境缺少 JWK Set URI 和 HS256 secret 时 fail-fast。
  - HS256 secret 长度要求至少 32 bytes。
  - issuer 必校验；`audience` 非空时校验 JWT `aud` claim。
- `backend/src/main/java/com/learningos/config/AuthProperties.java`
  - 扩展 `jwtSecret`、`issuer`、`jwkSetUri`、`audience`、`devHeaderFallbackEnabled`。
  - record canonical constructor 显式 `@ConstructorBinding`。
  - `devHeaderFallbackEnabled` 使用 `@DefaultValue("true")` 保持兼容。
- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
  - 优先从 Spring Security `JwtAuthenticationToken` 构建 `UserContext`。
  - `sub` 映射 `userId`，`name` 映射 `displayName`。
  - roles 仅接受 `ADMIN` / `TEACHER` / `STUDENT` / `USER` 白名单，不从 `sub` 推断角色。
  - production-like 无认证上下文时不再返回 `dev_user`，审计/日志侧使用 `unauthenticated` 兜底身份。
- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`
  - Bearer token 不再由该 filter 手写验证，直接交给 Spring Security。
  - 仅 dev/test 且无 Bearer 时允许 `X-User-Id` fallback。
  - prod/staging 不建立 header identity。
- `backend/src/main/java/com/learningos/common/auth/ApiAuthenticationEntryPoint.java`
  - 401 使用项目 `ApiResponse` envelope，不暴露 token/secret/raw exception。
- `backend/src/main/java/com/learningos/common/auth/ApiAccessDeniedHandler.java`
  - 403 使用项目 `ApiResponse` envelope。
- 测试 fixture
  - 现有 MockMvc Bearer JWT 测试的 HS256 fake secret 统一更新为 32 bytes 以上，符合 Spring Security/Nimbus HS256 约束。
  - 非安全目标的 `@WebMvcTest` 切片排除 Spring Security auto-configuration，避免新增全局安全链截断 exception/logging/health 切片测试。

## RED / Failure Evidence

### RED-1 `AuthProperties` 绑定失败

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest,SecurityFilterChainTest test
```

结果：

- `CurrentUserServiceTest`: pass
- `DevAuthFilterTest`: pass
- `SecurityFilterChainTest`: Spring context 启动失败
- 关键错误：`Failed to instantiate [com.learningos.config.AuthProperties]: No default constructor found`

Root cause：

- `AuthProperties` record 有 canonical constructor 和额外二参构造器，Spring Boot 配置绑定构造器选择不明确。

### RED-2 production no-token 被 anonymous 放行

修复绑定后再次运行 focused，结果：

- `SecurityFilterChainTest.productionRequestWithoutBearerTokenRejectsSpoofedUserHeaderWithSanitizedEnvelope`
- 期望 `401`，实际 `404`

Root cause：

- `SecurityConfig` 使用 `Authentication.isAuthenticated()`，anonymous authentication 可能被视作已认证，导致请求进入业务 controller。

### RED-3 HS256 secret 长度不符合 Spring Security/Nimbus 约束

扩展 invalid/wrong issuer/expired/valid Bearer 测试后运行 focused，结果：

- `SecurityFilterChainTest` 5 个用例 error。
- 关键错误：`The secret length must be at least 256 bits`

Root cause：

- 旧测试 fixture `unit-test-secret` 太短。

### RED-4 full backend MVC slice 被默认安全链截断

首次 full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- `497 run, 10 failures, 0 errors, 1 skipped`
- 失败集中于：
  - `GlobalExceptionHandlerTest`
  - `StructuredRequestLoggingFilterTest`
  - `HealthControllerTest`

Root cause：

- 新增 Spring Security 后，`@WebMvcTest` 切片自动装配默认安全过滤器，测试请求在进入目标 controller/filter 前被拦截为 401/403。

Fix：

- 仅在这些非安全目标的 MVC slice tests 中排除 Spring Security auto-configuration。
- 正式安全链仍由 `SecurityFilterChainTest` 覆盖。

## Verification Commands

### Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest,SecurityFilterChainTest test
```

Result:

```text
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

专家 follow-up 后补齐 audience mismatch、production auth material fail-fast、production no-context fallback 断言后重新运行 focused：

```text
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest,SecurityFilterChainTest,LearningWorkflowControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest test
```

Result:

```text
Tests run: 106, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Regression Slice After Full Failure

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=GlobalExceptionHandlerTest,StructuredRequestLoggingFilterTest,HealthControllerTest test
```

Result:

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Full Backend

```powershell
cd D:\多元agent\backend
mvn test
```

Result:

```text
Tests run: 500, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## Security Acceptance Evidence

| Case | Evidence |
|---|---|
| production no token | `SecurityFilterChainTest.productionRequestWithoutBearerTokenRejectsSpoofedUserHeaderWithSanitizedEnvelope` returns `UNAUTHORIZED` envelope. |
| invalid Bearer | `SecurityFilterChainTest.productionInvalidBearerTokenRejectsSpoofedUserHeaderWithSanitizedEnvelope` returns `UNAUTHORIZED` and ignores spoofed header. |
| wrong issuer | `SecurityFilterChainTest.productionWrongIssuerBearerTokenIsUnauthorized` returns `UNAUTHORIZED`. |
| wrong audience | `SecurityFilterChainTest.productionWrongAudienceBearerTokenIsUnauthorized` returns `UNAUTHORIZED`. |
| expired token | `SecurityFilterChainTest.productionExpiredBearerTokenIsUnauthorized` returns `UNAUTHORIZED`. |
| valid Bearer + spoofed header | `SecurityFilterChainTest.productionValidBearerTokenUsesJwtRolesAndIgnoresSpoofedUserHeader` succeeds through Bearer `ADMIN`, not spoofed `X-User-Id`. |
| subject-name role-confusion | `SecurityFilterChainTest.productionBearerSubjectNameDoesNotGrantAdminRole` returns `FORBIDDEN` for `sub=admin roles=USER`. |
| JWK Set URI branch | `SecurityJwtAuthenticationTest.securityConfigUsesJwkSetUriWithoutRequiringLocalHmacSecret` verifies JWK decoder path does not require local HS256 secret. |
| production fail-fast | `SecurityJwtAuthenticationTest.securityConfigFailsFastInProductionWhenAuthMaterialIsMissing` verifies prod requires JWK Set URI or HS256 secret. |
| CurrentUser bridge | `SecurityJwtAuthenticationTest.currentUserServiceBuildsUserContextFromSpringSecurityJwtAuthentication` verifies JWT -> `UserContext`. |
| production no-context fallback | `CurrentUserServiceTest.productionDoesNotFallbackToDevUserWhenNoContextIsAvailable` verifies unauthenticated production logging/auth helper fallback does not use `dev_user`. |
| Bearer not hand-verified by DevAuthFilter | `SecurityJwtAuthenticationTest.devAuthFilterDoesNotHandVerifyBearerTokensAnymore` and `DevAuthFilterTest` verify Bearer bypasses dev fallback filter logic. |

## Architecture Drift Check After

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Authentication stays in `common/auth`; business authorization remains in Service layer. |
| Frontend rules | PASS | No frontend changes. |
| Agent / RAG rules | PASS | No Agent/RAG runtime changes. |
| Security | PASS | New dependencies reviewed in `docs/security/`; production-like missing auth material fails fast; 401/403 use sanitized envelope. |
| API / Database | PASS | No REST path/DTO/schema changes. |

## Sources

- Spring Security Resource Server JWT / JWK reference: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- Spring Boot external configuration reference: https://docs.spring.io/spring-boot/reference/features/external-config.html
