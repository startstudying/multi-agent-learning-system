# PRD-20260606-feature-development-workflow-skill-yaml-fix

## 背景

Codex 启动时提示 `.agents/skills/feature-development-workflow/SKILL.md` 因 YAML 解析失败被跳过：

```text
mapping values are not allowed in this context
at line 2 column 425
```

该 Skill 是项目自动功能开发流程的入口。如果加载失败，后续原始需求无法稳定自动进入项目要求的 PRD、REQ、SPEC、PLAN、TASK、Context Pack、测试、证据和验收流程。

## 目标

- 修复 `feature-development-workflow` Skill 的 frontmatter YAML，使 Codex 能正常加载。
- 保留 Skill 原有名称、语义和关键词。
- 不引入新依赖，不改变业务代码，不修改前后端或数据库行为。

## 非目标

- 不重写 Skill 工作流正文。
- 不调整项目功能开发流程。
- 不修复无关文档编码显示问题。

## 用户价值

用户继续只输入原始需求，Codex 能自动使用项目规定的功能开发工作流，不再出现该 Skill 被跳过的警告。

## 成功指标

- 本地 `.agents/skills` 下所有 `SKILL.md` frontmatter 可被 YAML 解析。
- `feature-development-workflow` 的 `name` 和 `description` 字段存在且为字符串。
- 变更范围仅限 Skill 元数据和本次流程记录文档。
