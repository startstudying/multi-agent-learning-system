# TASK - 基于 jc-ai 参考的系统优化路线

## 1. Goal

基于用户提供的 `jc-ai` 参考仓库和本地 zip，形成一份适配当前 AI 学习系统的优化路线，并按项目 L 级流程创建研究、PRD、REQ、SPEC、PLAN、TASK、CONTEXT、Evidence、Acceptance 和记忆更新。

## 2. Task Type

优化计划 / 架构路线 / RAG / Agent / Evaluation / Frontend / Security governance。

## 3. Size

L

Reason:

- 涉及 frontend、backend、RAG、Agent workflow、evaluation、security、operations 和 protocol boundary。
- 本轮是路线设计，不实施代码；后续实现必须拆成 S/M/L 子任务。

## 4. Scope

In scope:

- 分析 `jc-ai` 本地参考包。
- 创建参考研究报告。
- 创建 L 级路线文档。
- 创建文档化 Multi-Expert Gate 报告。
- 更新 changelog 和相关 memory。
- 创建 docs-only evidence / acceptance。

Out of scope:

- 不修改 `backend/**`。
- 不修改 `frontend/**`。
- 不新增 API、DTO、DB migration。
- 不新增依赖。
- 不实现 HyDE、MCP、A2A、红队评测、工具审批或前端工作台。

## 5. Context Pack Summary

完整上下文见：

- `docs/context/CONTEXT-20260611-jc-ai-reference-optimization-roadmap.md`

## 6. Allowed Files

- `docs/research/github-references/GITHUB-20260611-jc-ai-optimization-reference.md`
- `docs/product/PRD-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/requirements/REQ-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/specs/SPEC-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/plans/PLAN-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/tasks/TASK-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/context/CONTEXT-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/subagents/runs/RUN-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/evidence/EVIDENCE-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/acceptance/ACCEPT-20260611-jc-ai-reference-optimization-roadmap.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/memory/DECISION_MEMORY.md`

## 7. Disallowed Files

- `backend/**`
- `frontend/**`
- `docs/superpowers/**`
- `pom.xml`
- `package.json`
- Flyway migration files
- runtime configuration containing secrets

## 8. Test Commands

```powershell
rg -n "jc-ai-reference-optimization-roadmap|jc-ai 参考|HyDE|MCP|A2A|红队" docs\research docs\product docs\requirements docs\specs docs\plans docs\tasks docs\context docs\evidence docs\acceptance docs\subagents docs\memory docs\changelog
git diff --stat
```

No runtime tests are required for this docs-only task.

## 9. Acceptance Criteria

- [x] 参考报告说明 GitCode 获取限制、本地 zip 证据和不复制代码约束。
- [x] Skill Selection Report 已记录。
- [x] Size Classification = L 已记录。
- [x] Multi-Expert Gate 以文档化评审方式记录。
- [x] PRD / REQ / SPEC / PLAN / TASK / CONTEXT 已创建。
- [x] 优化路线覆盖 P0/P1/P2/P3，并给出第一推荐切片。
- [x] Evidence / Acceptance 已创建。
- [x] Changelog 和 memory 已更新。
- [x] 未修改 backend/frontend 运行时代码。

## 10. Current Boundary

本任务完成后，只代表优化路线已准备好。下一步若要实施，应从 `P0-1 红队与 Prompt 注入评测扩展` 开始重新创建对应 M 级任务文档。
