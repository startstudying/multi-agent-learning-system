# Knowledge DAG 掌握度阈值补救优先开发计划

## 执行方式

启用 L1 并行分析子代理，主 Codex 串行实现。子代理只读核查算法边界和测试设计，不直接修改文件。

## Skill Selection Report

### Task Type

后端学习路径规划策略优化。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求功能变更先文档后实现，并更新证据和记忆。 |
| `ai-learning-agent-development` | 约束 AI 学习系统后端架构边界。 |
| `multi-agent-coder` | 用户明确要求多 subagent，并行只读分析可降低算法和测试风险。 |
| `learning-path-planner` | 本切片修改 Knowledge DAG 路径规划策略。 |
| `test-driven-development` | 行为变更必须先写失败测试再实现。 |
| `confidence-check` | 实现前确认没有重复实现、根因明确、架构合规。 |

### Missing Skills

无。

### GitHub Research Needed

否。本切片是现有路径规划服务的局部规则增强，不涉及外部框架或新依赖。

### New Project-Specific Skill To Create

暂不需要。

## Subagent Decision

Use Subagents: Yes

Reason: 路径规划规则影响排序、状态和 reason，且用户要求多 subagent 并行。为避免冲突，本次只用 L1 并行分析。

Parallelism Level: L1

Selected Subagents:

- 算法边界核查子代理
- RED 测试设计子代理

Implementation Mode: 主 Codex 串行实现

## 步骤

1. 读取项目记忆、TODO、当前路径规划代码和测试。
2. 启动两个只读子代理分别核查算法边界和测试设计。
3. 补齐 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
4. 在 `LearningWorkflowControllerTest` 添加低掌握度前置补救优先 RED 测试。
5. 运行 `LearningWorkflowControllerTest` 确认 RED。
6. 在 `LearningWorkflowService` 读取 mastery 后进行分批拓扑排序，并在同批可学习节点中优先低掌握度前置知识。
7. 运行相关测试并修复。
8. 更新 evidence、acceptance、changelog、memory、backend TODO 和 subagent run。

## Confidence Check

- 无重复实现：已搜索 `0.6`、`mastery`、`topologicalOrder`，当前路径规划没有补救优先规则。
- 架构合规：只改 Service 策略和测试，不改 Controller/DTO/DB。
- 官方文档：不涉及外部 SDK/API。
- OSS 参考：不引入外部实现。
- 根因明确：当前拓扑排序只考虑依赖满足情况，未在可学习节点内使用低掌握度补救优先级。

Confidence: 0.95
