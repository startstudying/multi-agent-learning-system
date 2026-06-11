# Orchestrator ANSWER_SUBMISSION 上下文收敛计划

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 本任务是后端 Agent workflow 增强，必须走 PRD/REQ/SPEC/PLAN/TASK/Context。 |
| `ai-learning-agent-development` | 项目级规则，约束学习系统模块边界。 |
| `ai-learning-architecture` | 定义 assessment workflow、状态和 replay 策略。 |
| `spring-ai-agent-backend` | 维护 Spring Boot service/controller/trace 分层。 |
| `assessment-feedback-agent` | 涉及答题提交、评分、错因反馈、掌握度更新。 |
| `agent-trace-governance` | 需要统一 `agent_task`、`agent_trace` 和业务 traceId。 |
| `test-driven-development` | 必须先写失败测试再实现。 |
| `verification-before-completion` | 完成前必须运行聚焦测试和全量测试。 |

Missing skills: 无。

GitHub Research Needed: No。当前代码和已完成 `RAG_QA` / `RESOURCE_GENERATION` 模式足够支撑。

New Project-Specific Skill To Create: No。

## Subagent Decision

Use Subagents: Yes

Reason: 用户要求多 subagent 并行开发；本任务涉及 Orchestrator、assessment、trace governance、幂等和权限。

Parallelism Level: L1

Selected Subagents:

- Architect review: 已完成 `RUN-20260606-orchestrator-answer-submission-context-review.md`
- Test Engineer: 并行只读评审测试覆盖
- Security & Quality: 并行只读评审权限、幂等、审计风险

Implementation Mode: Single Codex。代码文件重叠度高，不做并行实现。

## Confidence Check

| Check | Status | Evidence |
|---|---|---|
| No duplicate implementation | PASS | `ANSWER_SUBMISSION` 枚举存在，但 Orchestrator 无执行分支。 |
| Architecture compliance | PASS | 复用现有 Orchestrator、AssessmentService、AgentRunRecorder。 |
| Official docs verified | N/A | 不新增框架 API 或外部依赖。 |
| OSS references | N/A | 不需要新外部模式，参考项目已进入上层 TODO。 |
| Root cause identified | PASS | workflow type 已开放但缺少 assessment branch 和 replay。 |

Confidence: 0.90。可以进入本仓库最小实现。

## Steps

1. 补齐工作流文档和 Context Pack。
2. 在 `OrchestratorWorkflowControllerTest` 增加 `ANSWER_SUBMISSION` create/get/invalid/replay/conflict 测试。
3. 在 assessment service 层增加显式 traceId 入口和 replay preflight。
4. 在 Orchestrator 中增加 payload adapter、replay 查询、assessment 子步骤和终态转换。
5. 复查并补齐 replay 精确匹配、trace drift 清理、`requestId` 服务层校验。
6. 运行聚焦 Maven 测试。
7. 修复全量测试暴露的 RAG timeout recovery 测试 fixture 时间漂移。
8. 运行全量 Maven 测试。
9. 更新 evidence、acceptance、TODO、memory、changelog、retrospective。

## Risks

| Risk | Decision |
|---|---|
| 没有 workflow 表导致 replay 查询依赖 `inputJson` | 本切片接受，后续 P0-3 处理。 |
| Assessment 冲突在 task 创建后才发现 | 通过 replay preflight 前置规避。 |
| 运行期失败证据可能随事务回滚 | 本切片不扩大失败策略，后续 workflow node failure strategy 处理。 |

## Implementation Status

Done.

- `ANSWER_SUBMISSION` 已接入 Orchestrator workflow context。
- `AssessmentService.submitAnswerWithTraceId(...)` 支持显式 traceId。
- `AssessmentService.replayAnswerIfPresent(...)` 支持 Orchestrator 在创建 task 前做 replay/conflict preflight。
- 同 `requestId` 相同 payload replay 首次 workflow，不新增业务行或 workflow task。
- Orchestrator replay 会解析候选 `agent_task.inputJson` envelope 并精确匹配 `workflowType/ownerUserId/learnerId/requestId/questionId/answerLength/traceId`。
- trace drift 时返回已有 winning workflow，并删除本次 transient loser `agent_task` / `agent_trace`。
- `requestId` 校验下沉到 service 层，空值和超过 120 字符的值都会返回 `VALIDATION_ERROR`。
- 同 `requestId` 不同 payload 返回 `409 CONFLICT`，不新增 workflow task。
- 本轮未做通用 workflow retry/recovery 和运行期失败独立事务 evidence。
