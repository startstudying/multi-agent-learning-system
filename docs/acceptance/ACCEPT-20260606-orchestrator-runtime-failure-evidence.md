# Orchestrator 运行期失败证据持久化验收

## 验收结论

通过。

本轮 P0-1 最小切片已完成：`RAG_QA` 在 workflow task 创建后发生运行期权限失败时，接口仍返回 `FORBIDDEN`，同时后台保留 `FAILED agent_task`、`workflow_start` 和脱敏失败 step；查询接口可返回失败上下文。

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| RAG_QA 未授权 KB 返回原 403 | PASS | `OrchestratorWorkflowControllerTest.persistsRagQaRuntimeFailureEvidenceWithoutWritingQueryArtifacts` |
| 运行期失败后 `agent_task.status = FAILED` | PASS | 同上 |
| trace 包含 `workflow_start` 和 `step_runtime_failure` | PASS | 同上 |
| 失败摘要不包含完整 question 原文 | PASS | 同上 |
| 权限失败不写 query log / source citation | PASS | 同上 |
| `GET /api/orchestrator/workflows/{workflowId}` 可查询失败上下文 | PASS | 同上 |
| 无效 payload 仍不创建 task | PASS | `rejectsInvalidRagQaPayloadBeforeCreatingWorkflowTask` 等既有测试 |
| Orchestrator 聚焦测试通过 | PASS | `mvn "-Dtest=OrchestratorWorkflowControllerTest" test`，17 tests |
| 交叉回归通过 | PASS | 41 tests |
| 全量后端测试通过 | PASS | `mvn test`，122 tests |

## 未纳入本轮的后续项

- 未实现真正的 retry endpoint。
- 未实现 RAG query replay / response snapshot。
- 未实现文档上传幂等。
- 未实现教师/管理员权限模型。
- `model_call_log` 的 provider 错误脱敏仍需单独治理，本轮只避免 runtime `ApiException` 写入 model-call log。
