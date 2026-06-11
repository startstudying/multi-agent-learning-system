# REQ - Orchestrator Workflow 查询与状态上下文收敛

## 1. 追踪

- PRD：`docs/product/PRD-20260605-orchestrator-workflow-query.md`
- 需求编号：REQ-20260605-orchestrator-workflow-query

## 2. 功能需求

| 编号 | 需求描述 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | 创建 workflow 的响应必须包含 `workflowId`、`workflowType`、`agentTaskId`、`traceId`、`status`、`steps`、`traceSummary`、`recentFailedStep`、`nextActions` | 必须 | POST 测试断言上述字段存在且语义正确 |
| FR-02 | 新增 `GET /api/orchestrator/workflows/{workflowId}` 查询 workflow 当前状态上下文 | 必须 | create 后用返回的 `workflowId` 查询成功 |
| FR-03 | 查询结果返回最近失败步骤 | 必须 | 没有失败时为 `null`；存在失败 trace 时取最后一个 `FAILED` 步骤 |
| FR-04 | 查询结果返回 trace 摘要 | 必须 | 返回总步骤数、失败步骤数、最后步骤、最后状态 |
| FR-05 | 查询结果返回可继续动作 | 必须 | 根据当前 `status` 返回稳定的动作标识 |
| FR-06 | 不存在或不可访问的 `workflowId` 返回统一错误响应 | 必须 | HTTP 404，`code=NOT_FOUND`，`data=null` |

## 3. 非功能需求

| 编号 | 需求描述 | 优先级 |
|---|---|---|
| NFR-01 | Controller 只处理 HTTP 映射和当前用户获取 | 必须 |
| NFR-02 | 不新增数据库表、迁移或依赖 | 必须 |
| NFR-03 | 只修改 Context Pack 允许文件 | 必须 |

## 4. 用户流程

### 流程 1：创建后查询

```text
调用方 -> POST /api/orchestrator/workflows
系统 -> 创建 agent_task 和 workflow_start trace
调用方 -> GET /api/orchestrator/workflows/{workflowId}
系统 -> 返回同一个 workflow 的状态上下文
```

### 流程 2：查询不存在 workflow

```text
调用方 -> GET /api/orchestrator/workflows/wf_missing
系统 -> 抛出 ApiException(NOT_FOUND)
统一异常处理 -> 返回 404 envelope
```

## 5. 输入 / 输出

### 输入

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| workflowId | path string | 是 | 非空；不存在时返回 `NOT_FOUND` |

### 输出

| 字段 | 类型 | 说明 |
|---|---|---|
| workflowId | string | workflow 标识 |
| workflowType | string | 工作流类型 |
| agentTaskId | string | 关联 Agent 任务 |
| traceId | string | 请求/Agent trace 标识 |
| status | string | 当前任务状态 |
| steps | array | trace 步骤快照 |
| recentFailedStep | object/null | 最近失败步骤 |
| traceSummary | object | trace 摘要 |
| nextActions | array | 可继续动作 |

## 6. 边界情况

| 场景 | 预期行为 |
|---|---|
| workflow 不存在 | 404 + `NOT_FOUND` |
| workflow 属于其他用户 | 404 + `NOT_FOUND` |
| workflow 无 trace 步骤 | `steps=[]`，trace 摘要计数为 0 |
| workflow 当前为 `FAILED` | `recentFailedStep` 返回最近失败步骤，`nextActions` 包含 `RETRY_WORKFLOW` |

## 7. 依赖关系

- 上游依赖：`TraceFilter` 提供 `X-Trace-Id`，`CurrentUserService` 提供当前用户。
- 下游影响：前端和运维可用 `workflowId` 查询状态上下文。

