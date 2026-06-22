# TASK-20260621 记忆系统与回答质量计划/小白注释版调研报告

## 1. 目标

基于已完成的记忆系统与回答质量路线图，补充一份更容易执行的计划文档，以及一份左侧带“小白注释”的调研报告。

## 2. Task Type

Docs / research packaging。

## 3. Skill Selection

| Skill | Why |
|---|---|
| `feature-development-workflow` | 项目要求原始需求必须经过 size gate、Context Pack、证据和验收记录 |
| `analyze` | 本任务复用前序只读调研结论，需要保持证据与推断边界 |
| `plan-writer` | 项目技能注册表中用于写开发计划、风险和文件变更 |

Missing skills: 无阻塞。后续可沉淀 `memory-privacy-governance`。

GitHub research needed: No。本任务是把现有调研结论重新组织成可读文档，不新增外部方案。

## 4. Size Classification

Size: S

Reason:

- 只新增文档，不改生产代码。
- 不改变 API、DTO、DB schema、依赖、部署或前后端契约。
- 基于已完成的 L-size 路线图做二次整理。

Required Documents:

- 本 mini TASK，内嵌 Context Pack、Evidence、Acceptance。

Can Skip:

- PRD / REQ / SPEC / standalone CONTEXT / standalone EVIDENCE / standalone ACCEPT。

Upgrade Trigger:

- 如果用户要求开始实现 `Memory/RAG Privacy Guard` 或改后端/前端代码，需要升级为 M/L slice。

## 5. Context Pack

Related docs:

- `docs/product/PRD-20260621-memory-answer-quality-roadmap.md`
- `docs/requirements/REQ-20260621-memory-answer-quality-roadmap.md`
- `docs/specs/SPEC-20260621-memory-answer-quality-roadmap.md`
- `docs/plans/PLAN-20260621-memory-answer-quality-roadmap.md`
- `docs/evidence/EVIDENCE-20260621-memory-answer-quality-roadmap.md`
- `docs/acceptance/ACCEPT-20260621-memory-answer-quality-roadmap.md`
- `docs/subagents/runs/RUN-20260621-memory-chatgpt-quality-external-research.md`
- `docs/subagents/runs/RUN-20260621-memory-chatgpt-quality-security-quality.md`

Allowed files:

- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/research/REPORT-20260621-memory-answer-quality-beginner-notes.md`
- `docs/tasks/TASK-20260621-memory-answer-quality-plan-report.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`

Disallowed files:

- `backend/src/**`
- `frontend/src/**`
- `backend/src/main/resources/db/migration/**`
- dependency / env / lock files

Test commands:

- Docs-only，不运行后端/前端测试。
- 验证文件存在、关键标题存在、git 状态范围符合预期。

## 6. 实施记录

新增：

- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/research/REPORT-20260621-memory-answer-quality-beginner-notes.md`
- `docs/tasks/TASK-20260621-memory-answer-quality-plan-report.md`

更新：

- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`

## 7. Evidence

- 目标文件创建前不存在。
- 新增计划文档覆盖 P0-1 到 P2 的落地路线。
- 新增调研报告使用 Markdown 表格实现“左侧小白注释 / 右侧调研正文”。
- 本 TASK 是 docs / research packaging 范围，未要求修改 backend/frontend 生产代码。
- 当前工作区存在另一个已记录的前端覆盖迁移任务改动；该改动不属于本 TASK 的验收范围。

### 2026-06-21 复验证据

- 已执行 `git -c http.proxy= -c https.proxy= fetch origin main --prune`，远端 `main` 刷新成功。
- 已执行 `git rev-list --left-right --count main...origin/main`，结果为 `0 0`，当前 `main` 与 `origin/main` 无提交差异。
- `PLAN-20260621-memory-answer-quality-execution-readable.md` 结构审计：22 个标题，无待办占位词，覆盖 `P0-1`、`P0-2`、`P0-3`、`P0-4`、`P1`、`P2`。
- `REPORT-20260621-memory-answer-quality-beginner-notes.md` 结构审计：5 个标题，41 行 Markdown 表格，左栏为“小白注释”，右栏为正式调研结论，无待办占位。
- `TASK-20260621-memory-answer-quality-plan-report.md` 结构审计：包含目标、任务类型、Skill Selection、Size Classification、Context Pack、实施记录、Evidence、Acceptance。
- 已执行 `git diff --check -- docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md docs/research/REPORT-20260621-memory-answer-quality-beginner-notes.md docs/tasks/TASK-20260621-memory-answer-quality-plan-report.md docs/changelog/CHANGELOG.md docs/memory/PROJECT_MEMORY.md`，无 whitespace error；仅有既有 LF/CRLF 提示。

## 8. Acceptance

| Criteria | Verdict |
|---|---|
| 有一份可执行计划 | PASS |
| 有一份调研报告 | PASS |
| 调研报告左侧包含小白注释 | PASS |
| 文档基于前序调研结论 | PASS |
| 本 TASK 范围未要求生产代码改动 | PASS |
| `main` 与 `origin/main` 提交同步 | PASS |
| 目标文档无待办占位词 | PASS |

Acceptance Verdict: PASS
