# 前端 UI 界面审查任务

## 目标

对当前 Vue 前端工作台进行一次只读 UI/UX 审查，识别“界面丑、原型感重、信息层级混乱”的具体原因，并形成可执行的后续修复方向。

## 任务类型

UI/UX 审查 / 前端质量审查。

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 用户提出原始需求，项目规则要求先走 Project Memory、Skill Selection、Size Classification 和证据留痕。 |
| `frontend-design` | 本次核心是界面审美、信息架构、视觉层级和移动端体验审查。 |
| `code-review` | 使用严重级别表达问题，区分阻塞项、体验问题和改版建议。 |
| `gstack` | 任务涉及页面视觉审查和截图取证；本次 Playwright 依赖链不可用，改用已有截图和构建验证作为证据。 |

## Missing Skills

无。本次不需要 GitHub 研究或新增项目技能。

## GitHub Research Needed

No。问题可由现有项目截图、源码、构建输出和前端设计规则判断。

## Size Classification

Size: S

Reason: 本次只做审查和文档记录，不修改生产 UI 代码，不改变 REST API、DTO、数据库、依赖、前后端契约或 Agent/RAG 行为。

Required Documents: 本 mini TASK + 合并 Evidence/Acceptance。

Can Skip: PRD、REQ、SPEC、PLAN、独立 Context Pack、subagent 报告、独立 Acceptance。

Upgrade Trigger: 如果进入实际 UI 改版，且涉及 3 个以上页面、公共布局重构、组件抽取或可见交互语义变化，应升级为 M 或 L。

## Subagent Decision

Use Subagents: No

Reason: 审查范围集中在前端 4 个页面和全局样式，单 Codex 足以完成；不进行并行实现。

Parallelism Level: N/A

Implementation Mode: Single Codex, read-only audit.

## Embedded Context Pack

### Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/specs/SPEC-20260606-frontend-ui-prototype-refactor.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/harness/TEST_COMMANDS.md`

### Allowed Files

- `docs/tasks/TASK-20260611-frontend-ui-audit.md`
- `docs/evidence/EVIDENCE-20260611-frontend-ui-audit.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/changelog/CHANGELOG.md`

### Disallowed Files

- `frontend/src/**`
- `backend/**`
- 数据库 migration
- 依赖声明文件

### Test Commands

- `cd frontend && pnpm build`
- 读取并检查 `frontend/target-ui-check/*.png`
- 源码定位：`rg` 检查前端页面、样式和构建错误位置

### Current Boundary

只给出 UI 审查结论、严重级别、证据和后续改版建议；不修复 UI 和不修改前端生产代码。

## Acceptance Criteria

- 给出清晰的 UI 审查结论。
- 至少覆盖学生端、教师端、管理员端和移动端。
- 标出阻塞项、主要观感问题和可执行修复方向。
- 记录构建验证结果及限制。
