# TASK-20260611 GitHub Public Upload

## 目标

将当前项目代码初始化为 Git 仓库，并发布到 GitHub public 仓库，确保生成产物、浏览器缓存、本地历史和临时目录不进入远端。

## 任务类型

Docs / config / deployment。

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 用户提出原始交付需求，需要按项目 S/M/L 工作流执行。 |
| security-review | public 仓库发布前需要检查 secrets、缓存和本地产物泄露风险。 |
| changelog-writer | 发布完成后需要记录交付变更。 |

Missing Skills: 无。

GitHub Research Needed: No。

New Project-Specific Skill To Create: 无。

## Size Classification

Size: S。

Reason: 只涉及仓库发布配置、Git 初始化和发布记录，不变更业务代码、API、数据库、依赖或前后端契约。

Required Documents: 本 mini TASK；发布后创建 combined Evidence/Acceptance。

Can Skip: PRD / REQ / SPEC / PLAN / standalone Context Pack / Subagent。

Upgrade Trigger: 如需要新增 CI、部署流水线、代码结构调整或依赖变更，则升级为 M。

## Subagent Decision

Use Subagents: No。

Reason: 单一发布任务，边界清晰。

Parallelism Level: N/A。

Implementation Mode: Single Codex。

## Embedded Context Pack

Related memory/docs:

- `docs/memory/PROJECT_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`

Allowed files:

- `.gitignore`
- `docs/tasks/TASK-20260611-github-public-upload.md`
- `docs/evidence/EVIDENCE-20260611-github-public-upload.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- Git metadata / remote configuration created by `git init`

Disallowed files:

- Backend production source
- Frontend production source
- Database migrations
- API contract documents unrelated to this release
- Secrets or private credentials

Test / verification commands:

- `git status --short --branch`
- `git diff --cached --stat`
- secret scan with `rg`
- `gh auth status`
- `git push -u origin main`

Current boundary:

- Do not modify application behavior.
- Do not add dependencies.
- Do not publish generated artifacts such as `frontend/dist`, `backend/target`, `frontend/target-ui-check`, `.history`, `.omc`, or `.omx`.

## Acceptance Criteria

- Git 仓库初始化完成。
- GitHub public 仓库创建完成。
- 本地 `main` 分支推送到远端。
- `.gitignore` 覆盖本地缓存、构建产物和历史目录。
- 发布前完成 secret / large generated artifact 风险检查。
- Evidence 文档给出远端 URL、验证命令和验收结论。
