# Orchestrator ANSWER_SUBMISSION 上下文收敛验收

## 验收结论

通过。

本轮完成 `ANSWER_SUBMISSION` 的 Orchestrator 最小闭环。答题提交现在可以通过统一 workflow 入口执行，assessment 业务记录复用 Orchestrator `traceId`，并且 GET workflow 能回放完整链路。

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| `workflowType=ANSWER_SUBMISSION` 可执行真实答题提交 | PASS | `OrchestratorWorkflowControllerTest.createsAnswerSubmissionWorkflowAndReusesWorkflowTraceContext` |
| 复用统一 `workflowId / agentTaskId / traceId` | PASS | 同上 |
| assessment 业务记录复用 Orchestrator traceId | PASS | 同上 |
| GET workflow 返回相同上下文和步骤 | PASS | `OrchestratorWorkflowControllerTest.returnsWorkflowStatusContextAfterAnswerSubmission` |
| same requestId + same payload replay 首次 workflow | PASS | `OrchestratorWorkflowControllerTest.replaysAnswerSubmissionWorkflowWithSameRequestIdWithoutDuplicatingRows` |
| replay 精确匹配 workflow envelope，避免同 requestId 误命中错误早期 workflow | PASS | `OrchestratorWorkflowControllerTest.replaysAnswerSubmissionWorkflowByExactEnvelopeInsteadOfFirstRequestIdMarker` |
| trace drift 时返回已有 winning workflow，并清理本次 transient task/trace | PASS | `OrchestratorWorkflowControllerTest.concurrentAnswerWorkflowTraceDriftReturnsWinnerWorkflowWithoutPersistingSecondTask` |
| same requestId + different payload 返回 409 | PASS | `OrchestratorWorkflowControllerTest.rejectsAnswerSubmissionWorkflowPayloadConflictWithoutNewRows` |
| invalid payload 不创建 `agent_task` | PASS | `OrchestratorWorkflowControllerTest.rejectsInvalidAnswerSubmissionPayloadBeforeCreatingWorkflowTask` |
| service 层拒绝空 `requestId` | PASS | `AssessmentServiceTest.submitAnswerWithTraceIdRejectsBlankRequestIdAtServiceLayer`；`AssessmentServiceTest.replayAnswerIfPresentRejectsBlankRequestIdAtServiceLayer` |
| service 层拒绝过长 `requestId` | PASS | `AssessmentServiceTest.submitAnswerWithTraceIdRejectsTooLongRequestIdAtServiceLayer`；`AssessmentServiceTest.replayAnswerIfPresentRejectsTooLongRequestIdAtServiceLayer` |
| 直接 assessment API 兼容 | PASS | `AssessmentControllerTest` |
| RAG timeout recovery 测试 fixture 兼容当前时间 | PASS | `mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test`，6 tests, 0 failures, 0 errors |
| 全量后端测试通过 | PASS | `mvn test`，117 tests, 0 failures, 0 errors |

## 约束确认

- 未新增数据库 migration。
- 未新增依赖。
- 未改 `/api/assessment/answers` 合同。
- 未改前端。
- 未声称完成题目级权限校验。
- 未声称完成全部后端 TODO；本验收只覆盖 P0-1 `ANSWER_SUBMISSION` 子切片。

## 后续工作

- P0-1：补每个 workflow 节点的失败策略、重试策略和 durable failed evidence。
- P0-3：补 workflow-level idempotency/recovery 的显式表或唯一键。
- P3：补题目/课程/路径级权限校验和审计数据 retention。
