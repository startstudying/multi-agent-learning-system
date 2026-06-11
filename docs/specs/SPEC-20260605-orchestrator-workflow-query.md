# SPEC - Orchestrator Workflow 查询与状态上下文收敛

## 1. 概述

本规格为 TODO P0-1 的最小实现：在现有 Orchestrator 统一入口基础上，扩展 create 响应并新增 workflow 查询接口。实现复用已有 `agent_task` 与 `agent_trace`，不新增独立 workflow 表。

## 2. 追踪

- PRD：`docs/product/PRD-20260605-orchestrator-workflow-query.md`
- REQ：`docs/requirements/REQ-20260605-orchestrator-workflow-query.md`

## 3. 领域模型

- workflow 上下文暂存在 `agent_task.inputJson` 的 envelope 中，包含 `workflowId`、`workflowType`、`ownerUserId`、`learnerId`、`requestId`、`payload`。
- workflow 当前状态来自 `agent_task.status`。
- workflow 步骤来自 `agent_trace`，按 `sequenceNo` 升序返回。
- 最近失败步骤为 `agent_trace.status == "FAILED"` 的最后一条。

## 4. API 契约

### 创建 workflow

```http
POST /api/orchestrator/workflows
```

响应新增上下文字段：

```json
{
  "code": "OK",
  "message": "OK",
  "data": {
    "workflowId": "wf_xxx",
    "workflowType": "RESOURCE_GENERATION",
    "agentTaskId": "agt_xxx",
    "traceId": "trc_xxx",
    "status": "RUNNING",
    "steps": [
      {
        "stepId": "workflow_start",
        "agentName": "Orchestrator",
        "status": "RUNNING",
        "summary": "Workflow wf_xxx started for RESOURCE_GENERATION.",
        "sequenceNo": 1
      }
    ],
    "recentFailedStep": null,
    "traceSummary": {
      "traceId": "trc_xxx",
      "agentTaskId": "agt_xxx",
      "totalSteps": 1,
      "failedSteps": 0,
      "lastStepId": "workflow_start",
      "lastStatus": "RUNNING"
    },
    "nextActions": ["CHECK_STATUS"]
  }
}
```

### 查询 workflow

```http
GET /api/orchestrator/workflows/{workflowId}
```

响应同创建 workflow 的 `data` 结构。

### 错误码

| 错误码 | 说明 | 触发条件 |
|---|---|---|
| `NOT_FOUND` | Workflow not found | `workflowId` 不存在或不属于当前用户 |
| `VALIDATION_ERROR` | Validation failed | 请求 JSON 或 `payloadJson` 非法 |

## 5. 前端交互

前端创建 workflow 后可以保存 `workflowId`，通过 GET 查询接口轮询或恢复页面状态。`nextActions` 是稳定动作标识，前端可映射到按钮或状态提示。

## 6. 后端流程

```text
Controller
-> CurrentUserService.currentUserId()
-> OrchestratorWorkflowService.createWorkflow/getWorkflow
-> AgentRunRecorder/AgentTaskRepository/AgentTraceRepository
-> DTO 聚合
-> ApiResponse.success
```

## 7. Agent 工作流

```text
workflow_start
-> 后续 Agent 子流程（本任务不实现）
-> DONE / FAILED / WAITING_REVIEW
```

## 8. RAG 工作流

本任务不改 RAG 执行链路。`RAG_QA` workflow type 仅作为 Orchestrator 类型保留。

## 9. 数据库变更

无数据库变更。

## 10. 状态流转

```text
RUNNING / PENDING / WAITING / WAITING_REVIEW / DONE / FAILED / CANCELLED
```

`nextActions` 规则：

| status | nextActions |
|---|---|
| `RUNNING`, `PENDING`, `WAITING` | `CHECK_STATUS` |
| `WAITING_REVIEW` | `OPEN_REVIEW_QUEUE`, `CHECK_STATUS` |
| `FAILED` | `INSPECT_TRACE`, `RETRY_WORKFLOW` |
| `DONE` | `VIEW_RESULT` |
| `CANCELLED` | `START_NEW_WORKFLOW` |

## 11. 错误处理

Service 抛出 `ApiException(ErrorCode.NOT_FOUND, "Workflow not found")`，由 `GlobalExceptionHandler` 返回统一 envelope。Controller 不捕获业务异常。

## 12. 权限规则

查询 workflow 时必须限定当前用户。不存在和无权访问统一返回 `NOT_FOUND`。

## 13. Trace / 日志

创建 workflow 时保留现有 `workflow_start` trace。查询接口不新增 trace 记录，只读取并聚合已有 trace。

## 14. 测试策略

- 更新 `OrchestratorWorkflowControllerTest`：
  - create 后立即 GET，断言同一 `workflowId`、`agentTaskId`、`traceId`、`status`、`steps`、`traceSummary`、`nextActions`。
  - missing workflow 返回 404 + `NOT_FOUND` envelope。
- 按 TDD 先运行失败，再实现。

## 15. 验收清单

- [ ] FR-01 创建响应上下文字段已返回
- [ ] FR-02 GET 查询接口可用
- [ ] FR-03 最近失败步骤字段存在
- [ ] FR-04 trace 摘要字段存在
- [ ] FR-05 可继续动作字段存在
- [ ] FR-06 missing workflow 统一错误响应
- [ ] 目标测试通过

