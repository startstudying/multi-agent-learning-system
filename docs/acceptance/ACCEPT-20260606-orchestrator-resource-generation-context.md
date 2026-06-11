# Orchestrator Resource Generation 上下文统一验收

## 验收结论

通过。

本轮完成 `RESOURCE_GENERATION` 的 Orchestrator 上下文统一最小切片。通过 Orchestrator 发起资源生成后，资源生成任务、Agent Task、Agent Trace、Model Call、Token Usage 使用同一个 `agentTaskId/traceId`；`GET /api/orchestrator/workflows/{workflowId}` 可以看到 `workflow_start` 和资源生成 Agent 步骤。

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| Orchestrator 创建 `RESOURCE_GENERATION` 后返回 `WAITING_REVIEW` | PASS | `OrchestratorWorkflowControllerTest.createsResourceGenerationWorkflowAndReusesWorkflowAgentContext` |
| `resource_generation_task.agentTaskId` 等于 workflow `agentTaskId` | PASS | 同上 |
| `resource_generation_task.traceId` 等于 workflow `traceId` | PASS | 同上 |
| trace steps 包含 `workflow_start + 7` 个资源生成步骤 | PASS | 同上 |
| trace `sequenceNo` 连续为 `1..8` | PASS | 同上；`AgentRunRecorderTest.appendsTraceStepsAfterExistingStepsWithContinuousSequenceNumbers` |
| workflow 查询返回同一上下文和完整 steps | PASS | `OrchestratorWorkflowControllerTest.returnsWorkflowStatusContextAfterResourceGeneration` |
| 模型失败后保留 workflow、失败 task、失败 trace 和 model call | PASS | `OrchestratorWorkflowControllerTest.persistsFailedResourceGenerationWorkflowEvidenceWhenModelFails` |
| 非法资源生成 payload 不创建半成品 resource task | PASS | `OrchestratorWorkflowControllerTest.rejectsInvalidResourceGenerationPayloadBeforeCreatingWorkflowTask` |
| 直接资源生成接口不回退 | PASS | `ResourceGenerationControllerTest` |
| 全量后端测试通过 | PASS | `cd backend && mvn test`，94 tests，0 failures |

## 未纳入本轮的后续项

- `RAG_QA` workflow 接入同一 `workflowId/agentTaskId/traceId`。
- `ANSWER_SUBMISSION` workflow 接入同一 `workflowId/agentTaskId/traceId`。
- Orchestrator 级幂等、retry、recovery 策略。
- 独立 workflow 状态表或索引字段。
