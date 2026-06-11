# PLAN-20260606-feature-development-workflow-skill-yaml-fix

## Skill Selection Gate

| 项 | 内容 |
|---|---|
| Task Type | 工作流工具配置修复 |
| Selected Skills | `feature-development-workflow`、`security-review`、`architecture-drift-check`、`test-generator` |
| 选择原因 | 修复目标是功能开发工作流 Skill；需要确认无密钥/依赖风险、无架构漂移，并用解析测试验证 |
| Missing Skills | 无 |
| GitHub Research Needed | No |
| New Project-Specific Skill | 不需要 |

## Subagent Decision

| 项 | 内容 |
|---|---|
| Use Subagents | No |
| Reason | 只影响一个本地 Skill 元数据文件和流程记录文档 |
| Parallelism Level | Single Codex |
| Implementation Mode | 单任务执行 |

## 执行计划

1. 复现并定位 YAML 解析失败原因。
2. 将 `description` 改为 YAML folded block scalar。
3. 使用 Node + `yaml` parser 验证 `.agents/skills` 下所有 `SKILL.md` frontmatter。
4. 补齐本次 PRD、REQ、SPEC、PLAN、TASK、Context Pack、Evidence、Acceptance。
5. 更新 Changelog 和 Project Memory。

## 依赖审查

不新增依赖。验证使用 frontend 已安装的 `yaml` 包，不改变项目依赖声明。

## 风险与回滚

| 风险 | 处理 |
|---|---|
| YAML 多行字段缩进错误 | parser 验证所有本地 Skill |
| 修改范围扩大 | Context Pack 限制允许修改文件 |
| 回滚需要 | 将 `description` 恢复为合法引号字符串或 folded block scalar |
