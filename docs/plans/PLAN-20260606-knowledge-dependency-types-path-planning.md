# Knowledge DAG 依赖类型与路径规划开发计划

## 执行方式

启用 L1 并行分析子代理，主 Codex 串行实现。子代理只做代码证据核查和测试建议，不直接修改文件。

## Skill Selection Report

### Task Type

后端 Knowledge DAG 行为优化。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求所有功能变更先走 PRD/REQ/SPEC/PLAN/TASK/CONTEXT，再实现和验收。 |
| `ai-learning-agent-development` | 约束 AI 学习系统后端模块边界和架构规则。 |
| `multi-agent-coder` | 用户明确要求启动多 subagent，并行分析可降低路径规划风险。 |
| `learning-path-planner` | 本切片修改知识 DAG 与学习路径规划行为。 |
| `test-driven-development` | 行为变更必须先写 RED 测试再实现。 |
| `confidence-check` | 实现前确认无重复实现、架构合规和根因明确。 |

### Missing Skills

无。

### GitHub Research Needed

否。本切片是现有 Java/Spring 后端局部行为修正，不引入新技术和外部依赖。

### New Project-Specific Skill To Create

暂不需要。

## Subagent Decision

Use Subagents: Yes

Reason: 任务影响 `knowledge` 与 `learning` 两个后端模块，且用户明确要求多 subagent 并行开发。为避免文件冲突，本次只启用 L1 并行分析。

Parallelism Level: L1

Selected Subagents:

- dependencyType 校验路径核查
- 学习路径依赖排序核查

Implementation Mode: 主 Codex 串行实现

## 步骤

1. 读取项目记忆、TODO、当前代码和相关测试。
2. 启动两个只读子代理分别核查知识依赖创建路径和路径规划策略。
3. 补齐 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
4. 在 `CourseKnowledgeControllerTest` 添加非法依赖类型 RED 测试。
5. 在 `LearningWorkflowControllerTest` 添加 `RELATED` / `ADVANCED` 不锁定路径节点 RED 测试。
6. 运行相关测试确认失败。
7. 在 `KnowledgeCatalogService` 添加依赖类型枚举校验。
8. 在 `LearningWorkflowService` 只保留 `PREREQUISITE` 依赖参与路径规划。
9. 运行相关测试并修复。
10. 更新 evidence、acceptance、changelog、memory、TODO 和 subagent run 文档。

## 风险

- 如果仅在 DTO 注解层校验，错误信息和大小写规范化难以控制；因此使用服务层业务校验。
- 如果过滤发生在查询前，需要新增 repository 方法；本次不改 repository，直接在 service stream 中过滤，避免扩大范围。
- `RELATED` / `ADVANCED` 的长期推荐语义尚未实现，本次只保证它们不干扰前置路径。

## Confidence Check

- 无重复实现：已搜索 `dependencyType`、`prerequisitesByPoint`、`topologicalOrder`，未发现枚举校验或类型过滤。
- 架构合规：沿用 Controller -> Service -> Repository。
- 官方文档：本切片不使用新框架、新 SDK 或外部 API，无需外部官方文档。
- OSS 参考：本切片不引入外部实现。
- 根因明确：任意字符串直接保存，路径规划使用全部依赖类型。

Confidence: 0.95
