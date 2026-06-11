# Orchestrator 运行期失败证据持久化任务

## 1. 追溯

- PLAN：`docs/plans/PLAN-20260606-orchestrator-runtime-failure-evidence.md`
- SPEC：`docs/specs/SPEC-20260606-orchestrator-runtime-failure-evidence.md`
- 任务编号：TASK-20260606-orchestrator-runtime-failure-evidence

## 2. 目标

让 task 已创建后的 Orchestrator 运行期 `ApiException` 保留 `FAILED` task 和 trace 证据，同时接口仍返回原错误码。

## 3. 范围

### 纳入范围

- RAG_QA 权限失败回归测试。
- Orchestrator 运行期失败记录。
- 失败摘要脱敏。
- 查询失败 workflow 的验收。

### 排除范围

- retry endpoint。
- workflow 独立表。
- RAG query replay。
- 文档上传幂等。
- 真实认证/角色权限。

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- 本任务对应的 `docs/**`、Memory、Changelog、TODO。

## 5. 完成标准

- [x] RED 测试能证明当前运行期失败证据丢失。
- [x] RAG_QA 未授权 KB 返回 403，但持久化 `FAILED agent_task`。
- [x] trace 包含 `workflow_start` 和脱敏失败 step。
- [x] `GET /api/orchestrator/workflows/{workflowId}` 可查询失败上下文。
- [x] 权限失败不写入 query log / source citation。
- [x] 聚焦、回归、全量测试通过。
- [x] Evidence / Acceptance / Memory / Changelog 已更新。

## 6. 状态

| 字段 | 值 |
|---|---|
| 状态 | 已完成 |
| 负责人 | Codex |
| 开始日期 | 2026-06-06 |
| 完成日期 | 2026-06-06 |
