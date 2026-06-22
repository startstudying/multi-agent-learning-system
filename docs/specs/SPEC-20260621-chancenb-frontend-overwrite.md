# SPEC：覆盖迁移 chanceNB/mu 前端

## 任务类型

前端迁移 / 前端重构。

## Skill Selection Report

| Skill | 选择原因 |
|---|---|
| `feature-development-workflow` | 用户提出原始功能需求，必须执行项目尺寸分级、文档、测试、证据和验收流程。 |
| `frontend-design` | 本次迁移核心是 Vue 前端体验与视觉结构，需关注组件、布局、响应式和可用性。 |
| `Confidence Check` | 覆盖前需要确认没有重复误判、依赖风险、架构边界和迁移源。 |
| `using-git-worktrees` | 当前工作区有大量未提交改动，需评估是否隔离；用户已明确允许覆盖，因此在当前工作区执行但限制范围。 |
| `test-driven-development` | 前端行为变化需要测试保护；本次为覆盖迁移，优先使用迁移源现有测试并运行红/绿可行性验证。 |
| `verification-before-completion` | 完成声明前必须运行测试和构建，记录真实证据。 |

缺失技能：无。

GitHub Research Needed：否。用户已指定 `chanceNB/mu.git`，且本地已抓取 `chanceNB/main` 作为参考源，不需要额外搜索其他仓库。

## Size Classification

Size：L。

原因：

- 覆盖前端入口、路由、页面、组件、样式、测试与配置。
- 改变用户首屏体验和角色导航形态。
- 当前工作区已有另一套前端工作区逻辑，覆盖存在集成和回归风险。

Required Documents：

- `docs/product/PRD-20260621-chancenb-frontend-overwrite.md`
- `docs/requirements/REQ-20260621-chancenb-frontend-overwrite.md`
- `docs/specs/SPEC-20260621-chancenb-frontend-overwrite.md`
- `docs/plans/PLAN-20260621-chancenb-frontend-overwrite.md`
- `docs/tasks/TASK-20260621-chancenb-frontend-overwrite.md`
- `docs/context/CONTEXT-20260621-chancenb-frontend-overwrite.md`

## Subagent Decision

Use Subagents：No。

原因：本次只修改前端模块与流程文档，不修改后端、数据库、Agent/RAG 编排、权限或依赖；并且当前 Codex 运行时要求未获用户明确要求时不主动 spawn 子代理。采用单 Codex 执行，主 Codex 同时承担集成审查。

Parallelism Level：无。

Implementation Mode：Single Codex。

## 架构边界

- 前端仍是 Vue 3 + TypeScript + Vite。
- 所有业务 API 调用继续经由 `frontend/src/api/*` 模块和共享请求封装。
- 不新增 LLM provider 调用，不新增密钥暴露。
- 不修改后端 Controller、Service、Repository 或数据库迁移。

## 覆盖策略

1. 使用 `chanceNB/main` 的 `frontend` tree 作为目标前端。
2. 覆盖当前 `frontend` 下同名文件。
3. 删除当前 `frontend/src` 中不属于 `chanceNB/main` 的残留源文件，避免旧路由、旧组件或旧测试继续参与维护。
4. 保留不在迁移源中的本地环境文件不作为本次目标，但若它们不影响构建，可作为未跟踪文件保留。

## 测试策略

- 运行 `cd frontend && pnpm test -- --run`。
- 运行 `cd frontend && pnpm build`。
- 如构建通过，启动或复用本地预览进行视觉检查；如构建失败，记录失败并不声称完成。

## Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 不修改后端。 |
| Frontend rules | PASS 待验证 | 不新增 LLM 直连或前端密钥；构建后用搜索确认。 |
| Agent / RAG rules | PASS | 不修改 Agent/RAG 后端流程。 |
| Security | PASS 待验证 | 不新增依赖，不写入 secrets；覆盖后用搜索确认。 |
| API / Database | PASS | 不修改 API 合同或数据库 schema。 |

## 风险

- 覆盖会丢弃当前本地未提交的三端 workspace 和 AI QA 前端整合代码。
- `chanceNB/main` 的前端可能依赖其未迁移的后端聊天会话接口；本次不迁移后端，因此部分 UI API 可能需要后续补齐。
- 当前工作区仍有大量非前端未提交文件，本次不清理。
