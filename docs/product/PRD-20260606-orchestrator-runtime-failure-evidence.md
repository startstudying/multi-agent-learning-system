# Orchestrator 运行期失败证据持久化 PRD

## 1. 背景

当前 Orchestrator 已经把 `RESOURCE_GENERATION`、`RAG_QA`、`ANSWER_SUBMISSION` 纳入统一 `workflowId / agentTaskId / traceId` 上下文，但仍存在一个关键缺口：当 workflow task 已创建后，下游服务发生运行期错误时，事务可能整体回滚，调用方只能看到 HTTP 错误，无法通过 workflow 查询失败节点和失败原因。

这会削弱 P0 主闭环的可诊断性：管理员、教师或开发人员无法从 `agent_task` 和 `agent_trace` 解释失败发生在哪个 Agent 节点，也无法基于 `nextActions` 判断是否可重试。

## 2. 用户故事

- 作为教师或管理员，我希望一次 RAG/答题/资源生成 workflow 失败后，仍能看到失败节点和失败摘要，而不是只看到接口报错。
- 作为开发者，我希望无权限、内容安全拦截、业务冲突等运行期失败能保留 trace 证据，便于定位。
- 作为学生，我希望接口仍返回原本的错误码，不因为记录失败证据而把失败伪装成成功。

## 3. 目标

- 对 task 创建后的运行期 `ApiException` 写入 `FAILED agent_task` 和失败 `agent_trace`。
- 失败后仍原样返回原错误码，例如 403、409、400。
- `GET /api/orchestrator/workflows/{workflowId}` 能查询到失败 workflow 的 `recentFailedStep`、`traceSummary.failedSteps` 和 `nextActions`。
- 失败摘要不记录完整学生答案、完整问题原文或敏感内容。

## 4. 非目标

- 不实现真正的 retry endpoint。
- 不新增独立 workflow 表。
- 不实现 RAG query replay / response snapshot。
- 不改变前置校验语义：无效 payload、learner 不匹配、空 requestId 等仍在创建 task 前失败。

## 5. 验收指标

| 指标 | 目标 |
|---|---|
| RAG_QA 运行期权限失败 | HTTP 403，同时保留 `FAILED` workflow 证据 |
| 失败 trace | 包含 `workflow_start` 和一个失败 step |
| 查询接口 | 能通过 `workflowId` 返回失败状态和失败摘要 |
| 副作用 | 权限失败不写入成功 query log 或 citation |
| 回归 | 现有 Orchestrator/RAG/Assessment/Review tests 不回退 |
