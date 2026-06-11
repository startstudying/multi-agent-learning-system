# TASK-20260609 P3-4 子任务：formal OAuth2/JWK/Spring Security

Status: Done.

## Goal

引入最小 Spring Security OAuth2 Resource Server JWT/JWK 认证边界，让生产/预发 Bearer token 认证从手写 HS256 过渡实现迁移到标准 Spring Security。

## Checklist

- [x] Dependency review completed.
- [x] RED tests written and observed.
- [x] `pom.xml` adds Spring Security dependencies.
- [x] Spring Security `SecurityFilterChain` added.
- [x] JWT decoder supports JWK Set URI and local HS256 compatibility path.
- [x] JWT roles map to project `UserContext.roles()`.
- [x] `CurrentUserService` reads Spring Security authentication first.
- [x] `DevAuthFilter` no longer hand-verifies Bearer token.
- [x] prod/staging no token returns sanitized `UNAUTHORIZED`.
- [x] invalid Bearer never falls back to `X-User-Id`.
- [x] valid Bearer ignores spoofed `X-User-Id`.
- [x] focused tests pass.
- [x] adjacent tests pass.
- [x] full backend tests pass or limitation documented.
- [x] Evidence / Acceptance created.
- [x] Changelog / memory / backend TODO updated.

## Test Commands

Focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest,SecurityFilterChainTest test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest,SecurityFilterChainTest,LearningWorkflowControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest test
```

Full backend:

```powershell
cd D:\多元agent\backend
mvn test
```
