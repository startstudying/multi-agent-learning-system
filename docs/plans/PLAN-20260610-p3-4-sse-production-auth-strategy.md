# PLAN-20260610 P3-4 子任务：SSE production auth strategy

## Skill Selection Report

| Skill | Why |
|---|---|
| feature-development-workflow | 执行项目 S/M/L 工作流。 |
| security-review | SSE production auth 是认证边界。 |
| spring-boot-architecture | 验证 Spring Security / Controller 边界。 |
| auth-context-boundary | 遵循 Bearer/JWT、dev/test fallback、roles-first 规则。 |
| api-contract-design | 认证失败 envelope 与 endpoint 行为需稳定。 |
| test-generator | 增加 MockMvc 回归矩阵。 |
| architecture-drift-check | 防止引入 query token 或前端直连 LLM。 |

Missing skills：无。  
GitHub research：No。  
New project skill：No。

## Size Classification

Size：M。

原因：影响后端生产认证策略与 SSE runtime 回归，但不改数据库、依赖、公开 DTO，也不实现完整前端生产 streaming 客户端。

Required docs：REQ / SPEC / PLAN / TASK / CONTEXT。  
PRD：不需要，非产品行为新能力；是安全策略收口。

## Subagent Decision

Use Subagents：Yes。  
Parallelism Level：L1/L2 并行分析设计。  
Implementation Mode：主 Codex 单线实现，专家并行分析/测试设计。

Reports:

- `docs/subagents/runs/RUN-20260610-p3-4-sse-production-auth-strategy-architect.md`
- `docs/subagents/runs/RUN-20260610-p3-4-sse-production-auth-strategy-test.md`
- `docs/subagents/runs/RUN-20260610-p3-4-sse-production-auth-strategy-security.md`

## Implementation Plan

1. 新增 `SseProductionAuthStrategyTest`。
2. 覆盖 Chat/Tutor production no Bearer / invalid Bearer / valid Bearer / subject-name role-confusion。
3. 覆盖 Chat/Tutor staging header-only 拒绝。
4. 如测试已绿，说明当前生产策略已具备，代码不做无意义改动。
5. 更新 Evidence / Acceptance / Changelog / Memory。
6. 更新 `backend-architecture-todolist.md`：关闭 `SSE production auth strategy` 子项，但不关闭 P3-4 父项。

## Test Commands

Focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SseProductionAuthStrategyTest test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SseProductionAuthStrategyTest,ChatControllerTest,TutorControllerTest,SecurityFilterChainTest,DevAuthFilterTest,SecurityJwtAuthenticationTest test
```

Full:

```powershell
cd D:\多元agent\backend
mvn test
```

## Risks

- 原生 `EventSource` 生产可用性仍未解决；后续需要 fetch-streaming 或 cookie/signed-token 设计。
- 当前 Chat/Tutor SSE 内部授权失败的 event/error 合同仍不稳定；本轮只覆盖认证链前置拒绝和 valid Bearer role facts。
