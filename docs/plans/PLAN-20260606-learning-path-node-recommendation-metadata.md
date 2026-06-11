# 学习路径节点推荐元数据开发计划

## Skill Selection Report

### Task Type

后端 API 合同、学习路径规划 DTO、持久化字段和测试增强。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求功能开发先文档后实现，并更新证据、验收、记忆。 |
| `ai-learning-agent-development` | 约束 AI 学习系统后端架构边界。 |
| `learning-path-planner` | 本切片修改学习路径节点输出。 |
| `multi-agent-coder` | 用户要求多 subagent，并行只读分析可降低 API/测试风险。 |
| `test-driven-development` | API 行为变更必须先写失败测试。 |
| `Confidence Check` | 实现前确认重复实现、架构合规和根因。 |

### Missing Skills

无。

### GitHub Research Needed

否。本切片是现有后端 DTO 和持久化扩展，不需要外部项目或新依赖。

### New Project-Specific Skill To Create

暂不需要。

## Subagent Decision

Use Subagents: Yes

Reason: 任务涉及后端 API、数据库字段、测试和学习路径规划；用户明确要求启动多 subagent。为避免冲突，本轮仅使用 L1 只读并行分析。

Parallelism Level: L1

Selected Subagents:

- Rawls：路径节点 DTO / 持久化 / 资源测评关系影响分析，因超时关闭，未采用其输出。
- Heisenberg：TDD RED 测试设计，已返回报告。

Implementation Mode: 主 Codex 串行实现。

## Confidence Check

- 无重复实现：当前 `LearningPathNodeResponse` 和 `LearningPathNode` 均未包含四个目标字段。
- 架构合规：只扩展 DTO、Service、Entity、Migration、测试，不引入依赖。
- 官方文档：不涉及外部 SDK/API。
- OSS 参考：不需要外部实现。
- 根因明确：路径节点缺少资源和测评闭环元数据，且查询路径无法恢复此类信息。

Confidence: 0.95。

## 步骤

1. 创建本切片 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / subagent run。
2. 增加 RED 测试，断言节点 JSON 缺少四个新增字段。
3. 扩展 DTO、Entity、Service、V9 迁移和迁移测试。
4. 运行学习路径相关测试。
5. 更新 evidence、acceptance、changelog、memory 和 TODO。
