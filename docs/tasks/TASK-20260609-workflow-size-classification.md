# TASK-20260609 工作流 S/M/L 分级与父子任务命名

## 目标

将项目开发工作流从“所有需求都走完整大功能流程”调整为按任务体量分级执行，避免小切片被迫创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/subagent/retro 全套文档。

## 任务类型

Docs / workflow config。

## Size Classification

- Size: S
- 原因：只修改工作流说明文件，不改业务代码、API、数据库、依赖、前后端合同或运行时架构。
- Required Documents: 本 mini TASK，内嵌 Context Pack。
- Can Skip: 独立 PRD/REQ/SPEC/PLAN/CONTEXT、subagent 报告、独立 acceptance、独立 retrospective。
- Upgrade Trigger: 如果需要改 CLI 脚本、自动化工具或引入校验程序，则升级为 M。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `brainstorming` | 用户要求设计 3 套工作流方案并选择。 |
| `feature-development-workflow` | 本次修改的是项目需求到开发的主工作流。 |
| `writing-skills` | 修改 `.agents/skills/feature-development-workflow/SKILL.md`。 |
| `Confidence Check` | 修改规则前确认无业务实现风险和重复方向。 |

## Context Pack

### Related Memory and Docs

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/changelog/CHANGELOG.md`

### Allowed Files

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/tasks/TASK-20260609-workflow-size-classification.md`
- `docs/evidence/EVIDENCE-20260609-workflow-size-classification.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`

### Disallowed Files

- `backend/**`
- `frontend/**`
- `pom.xml`
- `package.json`
- database migration files

### Test / Verification Commands

```powershell
Select-String -LiteralPath 'D:\多元agent\AGENTS.md' -Pattern 'Size Classification Gate|S Done Definition|P3-4 子任务'
Select-String -LiteralPath 'D:\多元agent\.agents\skills\feature-development-workflow\SKILL.md' -Pattern 'Size Classification Gate|Create Size-Specific Documents|Epic / Parent-Child Naming'
```

## Acceptance Criteria

- [x] `AGENTS.md` 明确 S/M/L 分级。
- [x] 小切片 S 可跳过独立 PRD/REQ/SPEC/PLAN/CONTEXT/subagent/retro。
- [x] M/L 仍保留足够严谨的文档和验证要求。
- [x] `P3-4-X/Y/Z` 这类人工字母切片命名被替换为“父计划 ID + 语义子任务”。
- [x] `feature-development-workflow` skill 与 `AGENTS.md` 保持一致。
- [x] 不修改业务代码。
