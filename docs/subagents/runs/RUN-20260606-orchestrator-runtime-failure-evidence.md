# Subagent Run: Orchestrator 运行期失败证据持久化

## Subagent Decision

Use Subagents: Yes。

Reason: 本任务涉及 Orchestrator、Agent Trace、RAG 权限失败、安全隐私和测试边界，且用户要求多 subagent 并行开发。

Parallelism Level: L1。

Implementation Mode: Single Codex implementation with parallel analysis。

## 已收到结论

### Test Strategy

结论：最小 RED 应放在 `OrchestratorWorkflowControllerTest`，因为缺口不是 RAG/Assessment 单独服务，而是 Orchestrator 在下游运行期异常后没有把 workflow 收敛为可查询失败证据。

本轮处理：

- 新增 `persistsRagQaRuntimeFailureEvidenceWithoutWritingQueryArtifacts`。
- RED 暴露 task 停在 `RUNNING`。
- GREEN 后断言 HTTP 403、`FAILED` task、失败 step、无 query log/citation、GET workflow 可查失败上下文。

### Security / Privacy

结论：运行期失败 evidence 不能保存完整 question、answer、document excerpt 或 provider 请求体；`ApiException` 错误码不能被吞；权限失败不能伪造成功 citation/query 证据。

本轮处理：

- 持久化失败只保存 `ErrorCode` 和 safe summary。
- 原 `ApiException` 继续向外抛出。
- 测试断言失败摘要不包含完整 question。
- 测试断言 query log 和 citation 为 0。

### Architecture

结论：最小实现应限制在 `createWorkflow` 下游执行区间，前置校验仍应在创建 task 前失败；不做 retry endpoint 和 workflow 表。

本轮处理：

- `createWorkflow` 增加 `noRollbackFor = ApiException.class`。
- 只捕获 startRun 和 `workflow_start` 之后的下游 `ApiException`。
- 前置 invalid payload 既有测试继续通过。

## 集成决策

- 本轮完成 P0-1 运行期失败证据持久化。
- 下一个 P0 候选：`P0-3 RAG_QA query replay / response snapshot`。
- 另一个后续：真正 `RETRY_WORKFLOW` endpoint/contract。
