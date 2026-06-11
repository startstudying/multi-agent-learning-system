# ACCEPT-20260606-feature-development-workflow-skill-yaml-fix

## 验收结论

通过。

## 验收项

| 验收项 | 状态 | 证据 |
|---|---|---|
| `feature-development-workflow` frontmatter 可解析 | PASS | `OK 1 local skill(s) parsed` |
| `name` 字段保持不变 | PASS | `name: feature-development-workflow` |
| `description` 字段为字符串 | PASS | Node + `yaml` parser 校验 |
| 保留中英文关键词 | PASS | `description` 中保留 feature、fix、需求、实现、开发等关键词 |
| 无架构漂移 | PASS | 未修改 backend/frontend/Agent/RAG/API/database |
| 无新增依赖和密钥 | PASS | 仅使用已有 `frontend/node_modules/yaml` 进行验证 |

## 后续建议

后续新增或编辑本地 Skill 时，含冒号、逗号和中英文混排的长描述建议统一使用 `description: >`，避免 frontmatter 解析失败。
