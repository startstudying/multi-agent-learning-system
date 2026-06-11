# Orchestrator 失败与重试策略验收

## 1. 验收结论

通过。

本切片已补齐通用 `RuntimeException` 失败证据和最小 retry endpoint。失败 workflow 可通过 GET 查询到安全失败上下文；`FAILED RESOURCE_GENERATION` workflow 可由 owner 通过 retry endpoint 创建新 workflow。

## 2. 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| 通用 RuntimeException 后 `agent_task.status = FAILED` | PASS | `persistsGenericRuntimeFailureEvidenceWithoutLeakingSensitiveInput` |
| 失败 trace 包含 `workflow_start + step_runtime_failure` | PASS | 同上 |
| 失败 summary 不泄露原始 answer 或异常 message | PASS | 同上 |
| GET workflow 返回 `recentFailedStep` 和 `RETRY_WORKFLOW` | PASS | 同上 |
| retry endpoint 存在 | PASS | `retriesFailedResourceGenerationWorkflowAsNewWorkflowForOwner` |
| FAILED resource workflow retry 生成新 workflow | PASS | 同上 |
| retry 记录 `retryOfWorkflowId` | PASS | 同上 |
| 非 owner retry 返回 `NOT_FOUND` | PASS | 同上 |
| 非 FAILED workflow retry 返回 409 | PASS | `rejectsRetryForNonFailedWorkflow` |
| Orchestrator controller 回归通过 | PASS | `mvn "-Dtest=OrchestratorWorkflowControllerTest" test`：24 tests |

## 3. 测试结果

```text
mvn "-Dtest=OrchestratorWorkflowControllerTest#persistsGenericRuntimeFailureEvidenceWithoutLeakingSensitiveInput+retriesFailedResourceGenerationWorkflowAsNewWorkflowForOwner+rejectsRetryForNonFailedWorkflow" test
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0

mvn "-Dtest=OrchestratorWorkflowControllerTest" test
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
```

## 4. 风险与后续

- `RAG_QA` 和 `ANSWER_SUBMISSION` 不自动 retry，因为现有 envelope 脱敏后不能还原原始输入。
- retry 当前是同步最小实现，不含后台恢复、队列、退避或 retry attempt 表。
- 未执行全量后端测试；本切片只运行最小相关测试。
