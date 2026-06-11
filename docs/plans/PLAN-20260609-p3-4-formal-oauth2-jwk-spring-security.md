# PLAN-20260609 P3-4 子任务：formal OAuth2/JWK/Spring Security

## Task Type

Security architecture / dependency change / backend authentication boundary.

## Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 按项目 S/M/L 工作流推进。 |
| `subagent-driven-development` | 用户要求专家 subagent 并行开发。 |
| `security-review` | 修改认证边界与新增安全依赖。 |
| `auth-context-boundary` | 当前用户上下文、Bearer、header fallback 规则。 |
| `object-scope-authorization` | 确认本轮不修改业务对象授权矩阵。 |
| `test-driven-development` | 认证行为必须先 RED 后 GREEN。 |
| `verification-before-completion` | 完成前必须运行 focused / adjacent / full 验证。 |

Missing Skills: 无。

GitHub Research Needed: No。使用 Spring 官方文档和项目内专家审查足够。

New Project-Specific Skill To Create: 完成后更新 `auth-context-boundary`，不新增单独 skill。

## Size Classification

Size: L - Large Feature / Architecture Change, small execution scope.

Reason:

- 新增 Spring Security / OAuth2 Resource Server 依赖。
- 替换全局认证边界。
- 影响所有 HTTP 请求的认证路径。

Required Documents:

- PRD / REQ / SPEC / PLAN / TASK / CONTEXT
- dependency review
- subagent reports
- evidence / acceptance

Can Skip:

- Frontend PRD。
- DB migration。

Upgrade Trigger:

- 如果实现过程中需要改业务权限矩阵、SSE token 方案、登录体系或前端认证流，停止并拆新任务。

## Subagent Decision

Use Subagents: Yes。

Parallelism Level: L1 Parallel Analysis。

Selected Subagents:

- Architect
- Security Reviewer
- Test Engineer
- Integration Reviewer

Implementation Mode: Single Codex implementation after integrating expert reports。

## Architecture Drift Check - Before

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 认证层在 `common/auth`，业务授权仍在 Service。 |
| Frontend rules | PASS | 不改 frontend。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime。 |
| Security | WATCH | 新增依赖需 `docs/security/` 审查。 |
| API / Database | PASS | 不改 API / DB。 |

## Implementation Plan

1. 写 dependency review。
2. 写 RED 测试：
   - Spring Security JWT decoder / user context bridge。
   - prod/staging no-token 401。
   - invalid Bearer no fallback。
   - valid Bearer ignores spoofed header。
   - subject-name role-confusion remains denied。
3. 新增 Maven 依赖。
4. 新增安全配置：
   - `SecurityConfig`
   - JSON entry point / access denied handler
   - JWT decoder / validator
   - role converter
5. 收窄 `DevAuthFilter` 为 dev/test no-Bearer fallback。
6. 更新 `CurrentUserService` 从 Spring Security 读取 JWT。
7. 运行 focused / adjacent / full tests。
8. 更新 Evidence / Acceptance / Changelog / Memory / TODO。

## Allowed Files

见 `docs/context/CONTEXT-20260609-p3-4-formal-oauth2-jwk-spring-security.md`。

