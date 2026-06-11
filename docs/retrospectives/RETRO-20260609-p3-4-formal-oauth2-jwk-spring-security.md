# RETRO-20260609 P3-4 子任务：formal OAuth2/JWK/Spring Security

## 1. Feature Summary

完成 P3-4 formal OAuth2/JWK/Spring Security 最小认证边界切片：Bearer token 由 Spring Security Resource Server 处理，支持 JWK Set URI 和本地 HS256 兼容路径，补齐 issuer/audience 校验、production-like fail-fast、统一 401/403 envelope、`JwtAuthenticationToken` 到项目 `UserContext` 的桥接，以及 production-like no-context `unauthenticated` 兜底。

## 2. What Went Well

- 专家 subagent 并行给出 architecture / security / test / integration review，后续 debug/security/test follow-up 报告也已落盘。
- TDD/回归链路捕获了 `AuthProperties` 构造绑定、anonymous authentication 放行、HS256 secret 长度、`@WebMvcTest` 默认安全链截断等真实集成问题。
- 认证根边界和业务对象权限保持分层：认证层建立可信身份，业务 RBAC 仍在 Service 层收口。

## 3. What Didn't Go Well

- 新增 Spring Security 后，非安全目标的 MVC slice tests 被默认安全 auto-configuration 截断，说明后续新增全局 filter/security 配置时需要优先检查 slice test 隔离。
- 旧测试 fixture secret 长度不足，暴露出手写 JWT 兼容测试和标准 Nimbus/Spring Security 约束之间的差异。
- 首轮专家审查后发现 audience mismatch 和 production auth material fail-fast 缺少 focused 断言，已补测试并重新跑 focused/adjacent/full backend。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Spring Security Resource Server + dev/test fallback 边界规则 | Yes | `docs/skills/project-specific/auth-context-boundary.md` |
| 新增全局 SecurityFilterChain 后的 MVC slice 隔离检查 | Yes | `docs/skills/project-specific/auth-context-boundary.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | L/L-lite 安全切片已走完整文档和专家审查 | 后续 P3-4 子任务继续使用父计划 ID + 语义子任务命名，不再使用人工 X/Y/Z 分叉 |
| Testing | focused / adjacent / full backend 均已记录 | 新增全局安全组件时，把非安全 `@WebMvcTest` slice regression 作为固定检查项 |
| Documentation | Evidence / Acceptance / Changelog / Memory 已更新 | 当前状态记忆中不得继续写 formal OAuth2/JWK/Spring Security 未实现，只保留 SSE、IdP discovery、legacy fallback 清理等后续边界 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 继续扩展 class/course/answer-record RBAC 矩阵 | Main Codex | 后续 P3-4 语义子任务 |
| 设计 SSE/EventSource 生产认证传递方案 | Main Codex + Security Expert | 后续 P3-4 语义子任务 |
| 如需对接第三方 IdP discovery / `issuer-uri` 自动配置，另起 dependency/security review | Main Codex + Dependency/Security Expert | 后续 P3-4 或 P3-3 子任务 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] SKILL_REGISTRY.md
- [ ] ARCHITECTURE_BASELINE.md（本切片未改变高层架构基线；仅在后续正式部署文档中补认证配置示例）
