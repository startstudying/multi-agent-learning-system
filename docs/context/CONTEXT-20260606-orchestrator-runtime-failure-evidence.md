# Orchestrator 运行期失败证据持久化 Context Pack

## 1. 当前任务

`docs/tasks/TASK-20260606-orchestrator-runtime-failure-evidence.md`

## 2. 允许修改

- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- 本任务对应 `docs/**` 交付文档、Memory、Changelog、TODO。

## 3. 禁止修改

- 前端文件。
- RAG query replay / document upload idempotency 生产逻辑。
- Review Gate 生产逻辑。
- 认证/角色权限模型。
- API 路径。

## 4. 关键现状

- `createWorkflow(...)` 当前只对 `AiModelGateway.ModelCallFailedException` 设置 noRollback。
- `RESOURCE_GENERATION` 模型失败已有持久化失败证据。
- `RAG_QA` 和 `ANSWER_SUBMISSION` task 创建后的 `ApiException` 可能导致事务回滚。
- `nextActions(FAILED)` 已返回 `INSPECT_TRACE` 和 `RETRY_WORKFLOW`。

## 5. 验收重点

- 只捕获 task 创建后的 runtime failure。
- 前置 invalid payload 仍不创建 task。
- 失败摘要不包含完整问题或答案。
- 错误码原样返回。
- 不产生 query log / citation 成功伪证据。
