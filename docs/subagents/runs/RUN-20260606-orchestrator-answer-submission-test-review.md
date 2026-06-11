# Orchestrator ANSWER_SUBMISSION 测试评审

## Summary

当前 `ANSWER_SUBMISSION` 已被 `OrchestratorWorkflowType` 接受，但 `OrchestratorWorkflowService` 只执行 `RESOURCE_GENERATION` 和 `RAG_QA`。因此最小 RED 测试应先证明：`ANSWER_SUBMISSION` 不能只创建 `workflow_start` 后停在 `RUNNING`，必须执行 assessment 子流程并转为 `DONE`。

## Must Test

1. `createsAnswerSubmissionWorkflowAndReusesWorkflowTraceContext`
   - `POST /api/orchestrator/workflows`
   - `workflowType=ANSWER_SUBMISSION`
   - header `X-Trace-Id=trc_orchestrator_answer`
   - 断言 response `status=DONE`、`traceId` 复用 header、关键 step 存在。
   - 断言 `answer_record`、`grading_result`、`mastery_record`、`wrong_question`、`learning_event` 复用同一 traceId。

2. `returnsWorkflowStatusContextAfterAnswerSubmission`
   - 创建后 GET workflow。
   - GET 返回创建时 traceId，不使用 GET 请求的新 traceId。

3. `replaysAnswerSubmissionWorkflowWithSameRequestIdWithoutDuplicatingBusinessRows`
   - 同 learner + requestId + payload 调用两次 Orchestrator。
   - 推荐断言返回首次 `workflowId / agentTaskId / traceId`，且业务行和 `agent_task` 都不增加。

4. `rejectsAnswerSubmissionWorkflowPayloadConflictWithoutNewRows`
   - 同 requestId 不同 answer 返回 `409 CONFLICT`。
   - 不新增业务行和 workflow task。

5. `rejectsInvalidAnswerSubmissionPayloadBeforeCreatingWorkflowTask`
   - 缺 `questionId`、缺 `answer` 或缺外层 `requestId` 返回 `400 VALIDATION_ERROR`。
   - `agent_task` 和业务表计数不变。

## Optional Later

- `ANSWER_SUBMIT` alias 兼容。
- Orchestrator 层跨用户答题拒绝。
- Assessment 运行期失败 durable evidence。
- 并发 Orchestrator answer submission。

## Brittle Assertions To Avoid

- 不建议对中间 step summary 做完整文案断言。
- 可断言 step id 顺序和 traceId；不要把实现细节如生成资源数量迁移到答题流。
- `steps.length()` 可以在 SPEC 明确固定 step 后锁定；本轮固定 6 个 step 是可接受的最小规格。

## Recommended RED Order

1. create 成功链路。
2. GET workflow 状态。
3. replay 不重复业务行和 workflow task。
4. payload conflict。
5. invalid payload。
6. assessment service replay/preflight 分支。

