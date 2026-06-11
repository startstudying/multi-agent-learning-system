# TASK-20260606-feature-development-workflow-skill-yaml-fix

## Task

修复 `.agents/skills/feature-development-workflow/SKILL.md` frontmatter YAML，消除 Codex 启动时的 Skill 跳过警告。

## Done Criteria

- [x] 定位根因：未引号 `description` 中的 `Keywords:` 触发 YAML 解析错误。
- [x] 将 `description` 改为合法 YAML folded block scalar。
- [x] 保留 `name: feature-development-workflow`。
- [x] 保留中英文关键词。
- [x] 运行 parser 验证 `.agents/skills` 下所有 `SKILL.md`。
- [x] 更新 Changelog 和 Project Memory。
- [x] 创建 Evidence 和 Acceptance。

## 允许修改文件

- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/product/PRD-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/requirements/REQ-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/specs/SPEC-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/plans/PLAN-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/tasks/TASK-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/context/CONTEXT-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/evidence/EVIDENCE-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/acceptance/ACCEPT-20260606-feature-development-workflow-skill-yaml-fix.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`

## 禁止修改文件

- `backend/**`
- `frontend/**`
- `docs/superpowers/**`
- 数据库 migration 文件
- `.env*` 中的真实密钥文件
