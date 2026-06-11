# Orchestrator 节点契约与策略显式化证据

## 1. 代码变更

- `OrchestratorWorkflowStepResponse` 增加 `inputDto`、`outputDto`、`failurePolicy`、`retryPolicy`、`retryable`。
- `OrchestratorWorkflowService` 基于 `workflowType + stepId` 输出节点契约。
- `FAILED RESOURCE_GENERATION` 的 `nextActions` 保持 `INSPECT_TRACE`、`RETRY_WORKFLOW`。
- `FAILED RAG_QA` 和 `FAILED ANSWER_SUBMISSION` 的 `nextActions` 改为 `INSPECT_TRACE`、`RESUBMIT_ORIGINAL_REQUEST`。
- `OrchestratorWorkflowControllerTest` 增加 RED/GREEN 断言，覆盖资源生成节点契约、失败步骤契约和 retry 策略。

## 2. TDD 证据

RED：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest#returnsExplicitNodeContractsForResourceGenerationWorkflow" test
```

结果：失败原因符合预期，`$.data.steps[0].inputDto` 不存在。

GREEN：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest#returnsExplicitNodeContractsForResourceGenerationWorkflow" test
```

结果：1 个测试通过。

## 3. 回归验证

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest" test
```

结果：25 个测试通过。

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest,RagQueryServiceTest,SchemaConvergenceMigrationTest" test
```

结果：70 个测试通过。

## 4. 剩余风险

- 未运行后端全量 `mvn test`；本次已运行 Orchestrator 和相关回归集合。
- 资源生成模型失败 summary 脱敏仍需单独切片处理。
- P0-3 “其他长任务 retry_count / next_retry_at / last_error / recoverable” 仍未完成。
