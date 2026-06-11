# REQ-20260606-feature-development-workflow-skill-yaml-fix

## 需求说明

修复 `.agents/skills/feature-development-workflow/SKILL.md` 的 YAML frontmatter，使本地 Skill 加载器不再跳过该 Skill。

## 功能需求

| 编号 | 需求 | 验收标准 |
|---|---|---|
| REQ-1 | `description` 必须是合法 YAML 字符串 | YAML parser 能解析 frontmatter |
| REQ-2 | Skill 名称保持 `feature-development-workflow` | `name` 字段不变 |
| REQ-3 | 描述语义和中英文关键词保留 | `description` 中仍包含 feature、fix、需求、实现、开发等关键词 |
| REQ-4 | 不影响业务模块 | 不修改 backend、frontend、database 代码 |

## 约束

- 不新增依赖。
- 不写入密钥、令牌、私有凭据或原始日志。
- 不创建 `docs/superpowers/` 新文件。

## 风险

| 风险 | 缓解 |
|---|---|
| 多行 YAML 折叠后描述字段变成非字符串 | 使用 `description: >` 并用 parser 验证 |
| PowerShell 默认编码显示中文为乱码 | 验证时使用 `-Encoding utf8` 或 Node 读取 UTF-8 |
