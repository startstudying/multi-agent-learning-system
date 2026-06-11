# Orchestrator 运行期失败证据持久化规格

## 1. 概述

本切片补齐 P0-1 Orchestrator Workflow 的运行期失败证据。目标是在 workflow task 已创建后，下游 Agent/RAG/Assessment 服务出现 `ApiException` 时，系统仍能持久化失败 task 和 trace，并保持原 HTTP 错误码。

## 2. 追溯

- PRD：`docs/product/PRD-20260606-orchestrator-runtime-failure-evidence.md`
- REQ：`docs/requirements/REQ-20260606-orchestrator-runtime-failure-evidence.md`

## 3. 关键设计

### 3.1 事务边界

`OrchestratorWorkflowService.createWorkflow(...)` 增加 `noRollbackFor = ApiException.class`，避免运行期业务异常导致已创建 task 和 trace 回滚。

### 3.2 捕获范围

只在以下步骤之后捕获运行期异常：

```text
startRun
record workflow_start
execute downstream workflow
```

前置 DTO 解析和必填字段校验仍在 `startRun` 之前执行，失败时不创建 task。

### 3.3 失败记录

新增或复用服务方法记录：

```text
agent_task.status = FAILED
agent_task.outputJson = {"status":"FAILED","summary": "...", "errorMessage":"...", "recoverable": true}
agent_trace.stepId = step_runtime_failure
agent_trace.agentName = Orchestrator
agent_trace.status = FAILED
```

失败 summary 使用脱敏文本：

```text
Workflow RAG_QA failed with FORBIDDEN.
```

不写入完整 question、answer、document excerpt。

## 4. API 行为

### 4.1 创建接口

```http
POST /api/orchestrator/workflows
```

当运行期权限失败：

```json
{
  "code": "FORBIDDEN",
  "message": "No accessible knowledge bases for this query"
}
```

同时后台保留失败 workflow 证据。

### 4.2 查询接口

```http
GET /api/orchestrator/workflows/{workflowId}
```

返回：

```json
{
  "status": "FAILED",
  "recentFailedStep": {
    "stepId": "step_runtime_failure",
    "status": "FAILED"
  },
  "traceSummary": {
    "failedSteps": 1,
    "lastStatus": "FAILED"
  },
  "nextActions": ["INSPECT_TRACE", "RETRY_WORKFLOW"]
}
```

## 5. 测试策略

- `OrchestratorWorkflowControllerTest`
  - 新增 RAG_QA 运行期 KB 权限失败测试。
  - 断言 HTTP 403。
  - 断言 `agent_task` 为 `FAILED`。
  - 断言 trace 包含 `workflow_start` 和 `step_runtime_failure`。
  - 断言 query log / citation 未写入。
  - 通过提取 `workflowId` 调用 GET 查询失败上下文。

## 6. 验收清单

- [x] RAG_QA task 创建后的权限失败可持久化。
- [x] 失败 workflow 可查询。
- [x] 错误码保持原样。
- [x] 失败摘要脱敏。
- [x] 无效 payload 仍不创建 task。
- [x] 聚焦和全量测试通过。
