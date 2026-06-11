# Prompt Version 质量对比开发计划

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| `ai-learning-agent-development` | 当前任务属于 AI 学习系统后端优化 |
| `feature-development-workflow` | 项目要求每个功能补 PRD/REQ/SPEC/PLAN/TASK/Context |
| `multi-agent-coder` | 用户明确要求启动多 subagent 并行开发 |
| `subagent-driven-development` | 当前任务存在架构、测试、安全三个可并行分析面 |
| `spring-ai-agent-backend` | 后端 API、Service、DTO、JPA 实现 |
| `assessment-feedback-agent` | 质量指标涉及自动批改评估 |
| `agent-trace-governance` | prompt version、评测结果、治理日志边界 |
| `test-driven-development` | 先写失败测试，再实现 |
| `verification-before-completion` | 完成声明前必须有新鲜验证证据 |

## Subagent Decision

- Use Subagents: Yes
- Reason: 用户明确要求多 subagent；本任务涉及架构、测试、安全/治理。
- Parallelism Level: L1 Parallel Analysis
- Selected Subagents:
  - Architect: evaluation run / metric schema and API boundary
  - Test Engineer: RED/GREEN 测试策略
  - Security Reviewer: 权限、IDOR、敏感数据治理
- Implementation Mode: Single Codex integration

## Confidence Check

| Check | Result |
|---|---|
| Duplicate implementation | 未发现已有 `evaluation_run` / prompt quality comparison 实现 |
| Architecture compliance | 沿用现有 Spring Boot + JPA + API envelope + Service 边界 |
| Official docs | 本轮不引入新 SDK/API，使用现有项目内 JPA/MockMvc/Flyway 模式 |
| OSS references | 用户已给出评测闭环/Agentic RAG 方向参考，本轮不复制外部代码 |
| Root cause | 缺少持久化 evaluation run 和按 prompt version 聚合比较服务 |

Confidence: 0.95

## 实施步骤

1. 写 RED controller / migration 测试。
2. 新增 V14 migration。
3. 新增 `EvaluationRun` / `EvaluationRunMetric` 实体和 repository。
4. 新增 DTO。
5. 新增 `EvaluationRunService`，实现 run 记录和 comparison 聚合。
6. 新增 `EvaluationRunController`。
7. 更新 TODO、Evidence、Acceptance、Changelog、Memory。
8. 运行最小和扩展回归。

