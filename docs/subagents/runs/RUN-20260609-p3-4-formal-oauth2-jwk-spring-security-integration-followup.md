# RUN-20260609 P3-4 formal OAuth2/JWK/Spring Security Integration Follow-up

## Role

Integration Reviewer.

## Integrated Decision

本切片保持单 Codex 实现，专家 subagent 用于并行诊断、安全复核和测试矩阵复核。实现边界保持在认证根边界，不修改业务对象授权语义。

## Resolved Conflicts

| Topic | Decision |
|---|---|
| `AuthProperties` 绑定 | 保留 record 和便利构造器，但显式标记 canonical constructor binding。 |
| prod/staging no token | 必须要求 `JwtAuthenticationToken`，不接受 anonymous authentication。 |
| HS256 fallback | 仅 dev/test 可无配置使用本地固定假 secret；production-like 缺 JWK/secret fail-fast。 |
| audience | `learning-os.auth.audience` 非空时强制校验 `aud` claim。 |
| MVC slice tests | 排除 Spring Security auto-configuration，避免非安全测试被默认安全链截断；正式安全行为由专门安全链测试覆盖。 |

## Final Verification

- `mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest,SecurityFilterChainTest test`
  - `24 run, 0 failures, 0 errors`
- `mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest,SecurityFilterChainTest,LearningWorkflowControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest test`
  - `103 run, 0 failures, 0 errors`
- `mvn test`
  - `497 run, 0 failures, 0 errors, 1 skipped`

## Verdict

PASS. P3-4 formal OAuth2/JWK/Spring Security 最小生产化认证边界切片可验收；P3-4 父项仍保持开放。

