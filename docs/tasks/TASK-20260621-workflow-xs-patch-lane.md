# TASK-20260621 工作流 XS/Patch Lane 瘦身

## 目标

将当前开发工作流从 `S/M/L` 三档扩展为 `XS/S/M/L`，降低小补丁、单文件修复、文案样式调整的启动和收尾成本，同时保留中大型、Agent/RAG、安全、数据库、API 合同变更的严格治理。

## 任务类型

Docs / config / workflow optimization.

## Skill Selection

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 本次修改项目主开发工作流和 size-specific 执行规则。 |
| `writing-skills` | 本次会修改 `.agents/skills/feature-development-workflow/SKILL.md`。 |
| `Confidence Check` | 修改规则前确认重复实现、架构影响和根因。 |

## Size Classification

- Size: S
- Reason: 仅修改工作流规则、Cursor rules、项目记忆和变更日志；不改业务代码、API、数据库、依赖、部署拓扑或运行时架构。
- Required Documents: 本 mini TASK，内嵌 Context Pack；本任务可在 TASK 内记录最终 evidence/acceptance。
- Can Skip: 独立 PRD/REQ/SPEC/PLAN/CONTEXT、subagent 报告、独立 retrospective。
- Upgrade Trigger: 如果需要改 CLI 脚本、引入自动校验程序、修改业务代码或改变运行时架构，则升级为 M。

## Subagent Decision

- Use Subagents: No
- Reason: 单一工作流配置模块，文件边界清晰。
- Parallelism Level: None
- Implementation Mode: Single Codex

## Confidence Check

- No duplicate implementation: 当前规则中未发现 `XS` 或 `Patch Lane`。
- Architecture compliance: 只改现有文档和 Cursor rule 文件，不引入依赖或新技术栈。
- Official docs / OSS reference: 不涉及外部 API 或第三方库。
- Root cause: 旧 `.cursor/rules` 仍要求完整 PRD/REQ/SPEC/PLAN 流程，S 级仍要求独立 Evidence/Acceptance 和记忆更新。
- Confidence: 0.95

## Context Pack

### Related Memory and Docs

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `.cursor/rules/*.mdc`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/DECISION_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/SUBAGENT_REGISTRY.md`
- `docs/changelog/CHANGELOG.md`

### Allowed Files

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `.cursor/rules/00-project-memory-rule.mdc`
- `.cursor/rules/01-skill-selection-rule.mdc`
- `.cursor/rules/04-workflow-rule.mdc`
- `.cursor/rules/06-requirement-rule.mdc`
- `.cursor/rules/09-task-breakdown-rule.mdc`
- `.cursor/rules/12-acceptance-rule.mdc`
- `.cursor/rules/14-architecture-drift-rule.mdc`
- `.cursor/rules/18-auto-feature-intake-rule.mdc`
- `docs/tasks/TASK-20260621-workflow-xs-patch-lane.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`

### Disallowed Files

- `backend/**`
- `frontend/**`
- `pom.xml`
- `package.json`
- database migration files
- runtime configuration with secrets

### RED / Baseline Evidence

```powershell
rg -n "XS|Patch Lane|No pre-implementation docs|single TASK|REQ / SPEC / PLAN / TASK|PRD\s*$|Skill Extraction" AGENTS.md .agents/skills/feature-development-workflow/SKILL.md .cursor/rules
rg -n "S may use one combined evidence|Combined Evidence/Acceptance exists|REQ, SPEC, PLAN, TASK|Create evidence document|PRD|REQ|SPEC|PLAN|Skill Extraction" AGENTS.md .agents/skills/feature-development-workflow/SKILL.md .cursor/rules\04-workflow-rule.mdc .cursor/rules\12-acceptance-rule.mdc .cursor/rules\18-auto-feature-intake-rule.mdc
```

Baseline result: 当前规则中未出现 `XS/Patch Lane`，且旧规则仍命中完整 `PRD/REQ/SPEC/PLAN` 和独立 Evidence/Acceptance 要求。

### Verification Commands

```powershell
rg -n "XS|Patch Lane|single TASK|Focused verification only|risk-triggered|SPEC \\+ TASK \\+ CONTEXT" AGENTS.md .agents/skills/feature-development-workflow/SKILL.md .cursor/rules
rg -n "Raw Requirement|Patch Lane|M - Standard|Architecture Drift Check|Evidence and Acceptance" .cursor/rules\04-workflow-rule.mdc .cursor/rules\12-acceptance-rule.mdc .cursor/rules\14-architecture-drift-rule.mdc .cursor/rules\18-auto-feature-intake-rule.mdc
rg -n "workflow XS/Patch Lane|工作流 XS/Patch Lane|TASK-20260621-workflow-xs-patch-lane" docs/memory/PROJECT_MEMORY.md docs/changelog/CHANGELOG.md docs/tasks/TASK-20260621-workflow-xs-patch-lane.md
```

## Acceptance Criteria

- [x] `AGENTS.md` 增加 `XS / Patch Lane`，并明确不需要预置文档、默认不更新 memory/changelog。
- [x] S 级改为单个 `TASK` 内嵌 Context Pack、Evidence 和 Acceptance，不再要求独立 Evidence 文件。
- [x] M 级默认降为 `SPEC + TASK + CONTEXT`，`REQ/PLAN/PRD` 按条件触发。
- [x] `.agents/skills/feature-development-workflow/SKILL.md` 与 `AGENTS.md` 的分级和文档要求一致。
- [x] `.cursor/rules` 不再强制所有原始需求创建完整 PRD/REQ/SPEC/PLAN。
- [x] Project Memory、Changelog 记录本次工作流瘦身。

## Evidence / Acceptance

### Verification Commands

```powershell
rg -n "→ PRD$|→ REQ$|→ SPEC$|→ PLAN$|→ TASK$|→ Context Pack$|Size Classification Gate \(S / M / L\)|mini TASK|S may use one combined|Create evidence document" .cursor/rules/04-workflow-rule.mdc .cursor/rules/18-auto-feature-intake-rule.mdc .agents/skills/feature-development-workflow/SKILL.md AGENTS.md
rg -n "XS / Patch Lane|Size Classification Gate \(XS / S / M / L\)|S: one TASK|S: single TASK|M: SPEC \+ TASK|SPEC / TASK / Context Pack|risk-triggered|Architecture drift checks are risk-triggered" AGENTS.md .agents/skills/feature-development-workflow/SKILL.md .cursor/rules/04-workflow-rule.mdc .cursor/rules/12-acceptance-rule.mdc .cursor/rules/14-architecture-drift-rule.mdc .cursor/rules/18-auto-feature-intake-rule.mdc docs/memory/PROJECT_MEMORY.md docs/changelog/CHANGELOG.md
git diff --name-only -- AGENTS.md .agents/skills/feature-development-workflow/SKILL.md .cursor/rules docs/tasks/TASK-20260621-workflow-xs-patch-lane.md docs/memory/PROJECT_MEMORY.md docs/changelog/CHANGELOG.md
```

### Results

- Old forced full-flow search returned no matches in `AGENTS.md`, `.agents/skills/feature-development-workflow/SKILL.md`, `.cursor/rules/04-workflow-rule.mdc`, and `.cursor/rules/18-auto-feature-intake-rule.mdc`.
- New `XS / Patch Lane`, `XS / S / M / L`, S single-TASK, M default `SPEC + TASK + Context Pack`, and risk-triggered drift rules were found in the expected files.
- This task changed workflow docs/rules plus memory/changelog/TASK records. The worktree also contains pre-existing unrelated backend/frontend changes; they were not touched or reverted by this task.
- Architecture drift check was not required because this is docs/rules-only workflow configuration with no runtime architecture change.

### Acceptance Verdict

PASS. 本次工作流瘦身已完成，且保留了高风险任务升级到 S/M/L 严格流程的边界。
