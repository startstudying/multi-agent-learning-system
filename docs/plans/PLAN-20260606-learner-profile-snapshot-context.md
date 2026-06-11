# Learner Profile 维度与快照开发计划

## Skill Selection Report

### Task Type

后端画像 DTO、路径规划上下文、资源生成上下文和数据库迁移。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求先文档后实现并更新证据链。 |
| `ai-learning-agent-development` | 约束 AI 学习系统后端边界。 |
| `learner-profile-agent` | 本切片修改画像维度和证据字段。 |
| `learning-path-planner` | 学习路径需要引用并保存画像快照。 |
| `learning-resource-agent` | 资源生成需要引用并保存画像快照。 |
| `multi-agent-coder` | 用户要求多 subagent，本轮使用只读并行分析。 |
| `test-driven-development` | 先写 RED 测试再实现。 |

### Missing Skills

无。

### GitHub Research Needed

否。当前任务是本项目已有 JSON 模型和 JPA 实体扩展，不需要外部依赖或参考实现。

## Subagent Decision

Use Subagents: Yes

Parallelism Level: L1

Selected Subagents:

- Linnaeus：profile/schema/API 影响分析。
- Hooke：TDD RED 测试设计。

Implementation Mode: 主 Codex 串行实现。

## Confidence Check

- 无重复实现：当前没有 `profileSnapshot` 字段，`ProfileDimension` 缺 `lastEvidenceId`。
- 架构合规：沿用 Service + JPA + JSON 字段，不新增依赖。
- 官方文档：不涉及外部 SDK/API。
- OSS 参考：不需要。
- 根因明确：画像更新已存在，但下游路径和资源生成没有可审计快照。

Confidence: 0.94。

## 步骤

1. 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / subagent run。
2. 增加 profile、path、resource、migration RED 测试。
3. 扩展 DTO、实体、Service 和 V10 migration。
4. 运行最小验证和相关回归。
5. 更新 evidence / acceptance / changelog / memory / TODO。
