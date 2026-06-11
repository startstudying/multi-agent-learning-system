# SPEC-20260606-feature-development-workflow-skill-yaml-fix

## 当前问题

`SKILL.md` frontmatter 原写法为单行 `description: ... Keywords: ...`。未加引号的 YAML 标量中包含 `Keywords:`，解析器会将冒号识别为映射语法，导致：

```text
mapping values are not allowed in this context
```

## 技术方案

将 `description` 改为 YAML folded block scalar：

```yaml
description: >
  Use this skill whenever ...
  ... Keywords: feature, requirement, ...
```

该写法允许描述中包含冒号，同时保持解析后的字段类型为字符串。

## 文件变更

| 文件 | 变更 |
|---|---|
| `.agents/skills/feature-development-workflow/SKILL.md` | 将 `description` 从未引号单行改为 folded block scalar |
| `docs/product/PRD-20260606-feature-development-workflow-skill-yaml-fix.md` | 本次 PRD |
| `docs/requirements/REQ-20260606-feature-development-workflow-skill-yaml-fix.md` | 本次 REQ |
| `docs/specs/SPEC-20260606-feature-development-workflow-skill-yaml-fix.md` | 本次 SPEC |
| `docs/plans/PLAN-20260606-feature-development-workflow-skill-yaml-fix.md` | 本次 PLAN |
| `docs/tasks/TASK-20260606-feature-development-workflow-skill-yaml-fix.md` | 本次 TASK |
| `docs/context/CONTEXT-20260606-feature-development-workflow-skill-yaml-fix.md` | 本次 Context Pack |
| `docs/evidence/EVIDENCE-20260606-feature-development-workflow-skill-yaml-fix.md` | 本次验证证据 |
| `docs/acceptance/ACCEPT-20260606-feature-development-workflow-skill-yaml-fix.md` | 本次验收报告 |
| `docs/changelog/CHANGELOG.md` | 记录工具链修复 |
| `docs/memory/PROJECT_MEMORY.md` | 记录项目记忆 |

## 架构漂移检查

| 检查 | 状态 | 说明 |
|---|---|---|
| Backend layering | PASS | 未修改后端代码 |
| Frontend rules | PASS | 未修改前端代码 |
| Agent / RAG rules | PASS | 未修改运行时 Agent/RAG 行为 |
| Security | PASS | 未新增密钥、依赖或敏感数据 |
| API / Database | PASS | 未修改 API 或数据库 |

## 兼容性

Codex 或其他 Markdown Skill 加载器只需要读取 YAML frontmatter 的 `name` 和 `description` 字段。`description: >` 是标准 YAML 语法，兼容常见解析器。
