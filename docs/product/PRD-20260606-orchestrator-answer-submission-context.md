# Orchestrator ANSWER_SUBMISSION 上下文收敛 PRD

## 背景

当前 Orchestrator 已经统一了 `RESOURCE_GENERATION` 和 `RAG_QA` 的 `workflowId / agentTaskId / traceId`。答题提交仍主要通过 `/api/assessment/answers` 独立执行，虽然 assessment 业务记录已经保存 `traceId`，但还没有纳入统一 workflow 视图。

P0-1 的目标是把学习闭环中的答题反馈也接入 Orchestrator，使学生提交答案后，评分、错因诊断、掌握度更新、路径重规划可以通过同一个 workflow 查询。

## 目标

通过 `POST /api/orchestrator/workflows` 创建 `workflowType=ANSWER_SUBMISSION` 时：

- Orchestrator 创建或 replay 一个统一 workflow。
- Assessment 子流程复用 Orchestrator 的 `traceId`。
- `answer_record`、`grading_result`、`mastery_record`、`wrong_question`、`learning_event` 使用同一个 `traceId`。
- `GET /api/orchestrator/workflows/{workflowId}` 可以看到完整 assessment 子步骤。

## 非目标

- 不新增 workflow 表。
- 不新增数据库 migration。
- 不改变 `/api/assessment/answers` 的现有兼容行为。
- 不实现通用 workflow retry/recovery。
- 不接入真实 LLM rubric grading。

## 用户价值

- 学生端可以看到答题反馈已完成，而不是只拿到孤立 response。
- 教师和管理员可以通过 workflow trace 解释一次答题反馈的处理链路。
- 后续学习分析、错题本、路径重规划可以基于同一 trace 追踪。

## 成功指标

- `ANSWER_SUBMISSION` workflow 成功返回 `DONE`。
- 重复同一 `requestId` 和相同 payload 不新增业务记录，也不新增 workflow task。
- 重复同一 `requestId` 但不同 payload 返回 `409 CONFLICT`。
- 无效 payload 在创建 `agent_task` 前返回 `400 VALIDATION_ERROR`。

