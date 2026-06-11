# Orchestrator 节点契约与策略显式化验收

## 1. 验收结论

通过。

## 2. 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| `steps[]` 返回节点输入 DTO | 通过 | `OrchestratorWorkflowControllerTest#returnsExplicitNodeContractsForResourceGenerationWorkflow` |
| `steps[]` 返回节点输出 DTO | 通过 | 同上 |
| `steps[]` 返回失败策略和重试策略 | 通过 | 同上 |
| `recentFailedStep` 返回业务 DTO 和失败证据输出 DTO | 通过 | RAG_QA / ANSWER_SUBMISSION runtime failure 测试 |
| 资源生成失败 workflow 可提示 endpoint retry | 通过 | 资源生成失败与 retry 测试 |
| RAG/答题失败 workflow 不再提示不可用 retry | 通过 | `nextActions[1] = RESUBMIT_ORIGINAL_REQUEST` |
| 不新增 DB schema | 通过 | 未新增 migration |

## 3. 验证命令

- `mvn "-Dtest=OrchestratorWorkflowControllerTest" test`
- `mvn "-Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest,RagQueryServiceTest,SchemaConvergenceMigrationTest" test`

## 4. 未完成事项

- 后台自动重试、退避、retry 次数限制属于 P0-3/P3 后续任务。
- `LEARNING_GOAL_CREATION` 仍未接入完整 workflow。
- 资源生成模型失败 summary 需要后续脱敏加固。
