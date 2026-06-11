# RUN-20260609 P3-4 formal OAuth2/JWK/Spring Security - Test

## Verdict

Use TDD with auth-focused RED tests first.

## Recommended Focused Tests

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest test
```

## Recommended Adjacent Tests

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest,LearningWorkflowControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest test
```

## RED Matrix

- no Bearer in prod/staging -> 401.
- invalid Bearer + spoofed header -> 401, no fallback.
- valid Bearer + spoofed header -> token subject.
- `sub=admin roles=USER` -> not admin.
- `sub=teacher_1 roles=USER` -> not teacher.
- wrong issuer / expired token -> 401.
- sanitized 401 response.

