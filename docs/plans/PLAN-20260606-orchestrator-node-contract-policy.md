# Orchestrator 节点契约与策略显式化计划

## 1. Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求功能开发先建文档和上下文 |
| `ai-learning-agent-development` | 当前任务属于 AI learning 后端 Orchestrator |
| `ai-learning-architecture` | 需要明确 workflow 节点输入、输出、失败和重试策略 |
| `spring-ai-agent-backend` | 修改 Spring Boot Service/DTO/API 测试 |
| `agent-trace-governance` | 契约挂在 `agent_trace` 派生响应上 |
| `test-driven-development` | 行为变更先写 RED 测试 |
| `verification-before-completion` | 完成前必须运行验证命令 |

Missing skills: `Confidence Check` 本地路径首次读取失败，随后用 `confidence-check` 路径读取并执行简化自检。

GitHub Research Needed: No。该切片是本地 Orchestrator 策略显式化，不新增外部依赖或框架。

New Project-Specific Skill To Create: 暂不需要。

## 2. Subagent Decision

Use Subagents: Yes。

Reason: 涉及 Orchestrator 架构、失败/重试治理、API 测试三类判断，按项目规则启用 L1 只读并行分析。

Parallelism Level: L1 Parallel Analysis。

Selected Subagents:

- Architect: workflow/node 边界和 API 形状。
- Security & Quality: 失败/重试、安全脱敏和权限风险。
- Test Engineer: RED 测试断言设计。

Implementation Mode: Single Codex。子代理不改文件，主 Codex 单点集成。

## 3. 实施步骤

1. 创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/RUN。
2. 在 `OrchestratorWorkflowControllerTest` 添加 RED 断言。
3. 扩展 `OrchestratorWorkflowStepResponse`。
4. 在 `OrchestratorWorkflowService` 增加节点契约映射和 workflow-aware `nextActions`。
5. 运行聚焦测试和相关回归。
6. 更新证据、验收、TODO、记忆、changelog、复盘。

## 4. 风险

- `LEARNING_GOAL_CREATION` 枚举存在但无完整执行链路，本切片不伪造其节点契约。
- 当前 workflow 查询仍基于 `agent_task.inputJson` marker，后续若要强一致节点级 retry，需要独立 schema。
