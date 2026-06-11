# Subagent Registry

## Role Positioning

```text
Main Codex     = 总调度 / 总集成 / 最终决策
Subagent       = 专家并行分析、设计、审查；必要时隔离实现
Cursor         = IDE 内规则约束和局部开发辅助
```

## Expert Registry

| Subagent | Responsibility | When To Use | Output |
|---|---|---|---|
| Product Analyst | 产品判断、用户故事、MVP、非目标范围 | 新功能、需求模糊、产品取舍 | PRD 建议 |
| Spec Architect | 系统边界、模块拆分、接口契约、数据流 | 跨模块功能、复杂架构调整 | SPEC 草案 |
| Frontend Expert | Vue 页面、组件、状态、SSE、可视化 | 前端页面、AI 工作台、看板 | 前端方案 |
| Backend Expert | Spring Boot API、Service、DB、权限、事务 | 后端接口、数据库、业务逻辑 | 后端方案 |
| Agent/RAG Expert | Agent 编排、Tool Calling、RAG、Trace、Citation | AI 工作流、知识库、资源生成 | Agent/RAG 方案 |
| Security & Quality | 权限、安全、依赖、测试、架构漂移 | 涉及权限、依赖、DB、Agent/RAG 的任务 | 风险报告 |
| Integration Reviewer | 合并专家输出、解决冲突、形成最终任务 | 多专家完成后 | PLAN / TASK / 冲突解决 |

## Common Expert Combinations

Most used in this project:

```text
Frontend Expert
Backend Expert
Agent/RAG Expert
Security & Quality
Integration Reviewer
```

Add Product Analyst when requirements are ambiguous.
Add Spec Architect when architecture boundaries need redesign.

## When To Enable (Hard Rules)

| Condition | Action |
|---|---|
| 影响 1 个模块 | 默认单 Codex |
| 影响 2 个模块 | 可启用相关专家并行分析 |
| 影响 3 个以上模块 | 必须启用 Multi-Expert Subagent |
| 涉及 Agent/RAG | 必须启用 Agent/RAG Expert |
| 涉及权限、数据库、依赖、安全 | 必须启用 Security & Quality |
| 涉及前后端接口联动 | 必须启用 Frontend + Backend + Integration Reviewer |

## Good Candidates

- 对话式学习画像构建
- 多 Agent 资源生成
- 学习路径规划
- Agent Trace Console
- RAG Citation Viewer
- 智能辅导 SSE 流式输出
- 学习效果评估看板
- 后端 Agent Orchestrator
- RAG 知识库模块
- 资源审核工作台

## Not For

- 改按钮文案
- 改一个样式
- 修一个简单字段
- 补一个简单校验
- 改 README 一句话

## Parallelism Levels

| Level | What | Default |
|---|---|---|
| L1 | 并行分析 | Yes for medium/large |
| L2 | 并行设计 | Yes when cross-module |
| L3 | 并行实现 | No — only with worktree + no overlap |
