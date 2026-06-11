# Orchestrator ANSWER_SUBMISSION 上下文收敛评审

## Summary

`ANSWER_SUBMISSION` 建议接入统一 Orchestrator workflow，但不能直接在 `createWorkflow` 中简单调用 `AssessmentService.submitAnswer(...)`。当前 Orchestrator 已声明 `ANSWER_SUBMISSION` workflow type，却没有执行 assessment 子流程；如果直接放行，会只创建停留在 `RUNNING` 的 workflow task。

## Root Cause

根因是 Orchestrator 的类型枚举领先于执行编排：`ANSWER_SUBMISSION` 已被允许进入统一入口，但缺少 payload adapter、Assessment Service 调用、trace step 追加、task 终态转换和 workflow replay 策略。

Assessment 子流程自身已经有 `(learnerId, requestId)` 幂等和响应快照。如果 Orchestrator 每次重复请求都创建新 `agent_task`，再由 assessment replay 首次 answer response，就会出现新 workflow traceId 与旧 answer traceId 不一致的审计断链。

## Recommendations

1. 新增 workflow-aware assessment 入口，例如 `submitAnswerWithTraceId(...)`，由 Orchestrator 显式传入统一 `traceId`。
2. `ANSWER_SUBMISSION` payload 必须在 `agent_task` 创建前校验：`learnerId`、外层 `requestId`、`questionId`、`answer` 都必须有效。
3. 同 learner + requestId + same payload 的 Orchestrator replay 应返回首次 workflow status context，不应创建新 workflow。
4. 同 learner + requestId + different payload 应在创建 workflow 前返回 `409 CONFLICT`。
5. 成功 workflow 至少写入 `workflow_start`、`step_assessment_safety`、`step_assessment_grading`、`step_assessment_feedback`、`step_assessment_mastery`、`step_assessment_replan`。

## Architectural Status

`WATCH`

可以进入最小实现，但合并前必须验证 workflow replay、traceId 注入和业务记录 trace 对齐。暂不建议新增 workflow 表或数据库 migration；这属于后续 workflow-level idempotency/recovery 强化。

## Trade-offs

| Option | Pros | Cons |
|---|---|---|
| 最小接入并 replay 首次 workflow | 不改 schema，符合 P0-1，交付快 | 依赖 `agent_task.inputJson` 查询 workflow envelope |
| 新增 workflow 表和唯一键 | 幂等、查询、恢复最清晰 | 需要 migration，超出本切片 |
| 直接调用现有 `submitAnswer(...)` | 改动最少 | 会产生重复 workflow 和 trace 漂移，不建议 |

## References

- `docs/planning/backend-architecture-todolist.md`：P0-1 `ANSWER_SUBMISSION` 尚未统一到 workflow context。
- `docs/memory/BACKEND_MEMORY.md`：Orchestrator context convergence 仍为 partial。
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowType.java`：`ANSWER_SUBMISSION` 类型已存在。
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`：当前只执行 `RESOURCE_GENERATION` 和 `RAG_QA`。
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`：answer submission 已有业务幂等和 trace 持久化基础。

