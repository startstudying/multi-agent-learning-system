# Agent 状态机收敛规格

## 状态集合

### Agent Task

```text
PENDING
RUNNING
WAITING_REVIEW
DONE
FAILED
CANCELLED
```

### Agent Trace

```text
PENDING
RUNNING
WAITING
WAITING_REVIEW
DONE
FAILED
CANCELLED
```

## 状态流转

```text
PENDING -> RUNNING
PENDING -> CANCELLED
RUNNING -> WAITING_REVIEW
RUNNING -> DONE
RUNNING -> FAILED
RUNNING -> CANCELLED
WAITING_REVIEW -> DONE
WAITING_REVIEW -> FAILED
WAITING_REVIEW -> CANCELLED
DONE -> terminal
FAILED -> terminal
CANCELLED -> terminal
```

## API

新增：

```http
POST /api/agent/tasks/{taskId}/cancel
```

请求：

```json
{
  "reason": "User cancelled duplicated resource generation."
}
```

响应复用 `AgentTraceResponse`：

```json
{
  "taskId": "agt_xxx",
  "status": "CANCELLED",
  "steps": [],
  "traceId": "trc_xxx"
}
```

## 服务行为

- `AgentRunRecorder.startRun` 校验任务初始状态。
- `AgentRunRecorder.recordTraceSteps` 校验 trace step 状态。
- `AgentRunRecorder.recordFailure` 使用统一失败输出 JSON：

```json
{
  "status": "FAILED",
  "summary": "...",
  "errorMessage": "...",
  "recoverable": true
}
```

- `AgentRunRecorder.cancelTask`：
  - 校验任务存在。
  - 校验 owner。
  - 校验当前状态可取消。
  - 更新 `agent_task.status = CANCELLED`。
  - 写入 `outputJson`，包含 reason。
  - 追加一条 `CANCELLED` trace。
- `ResourceGenerationService.createTask`：
  - 生成草稿成功后，`agent_task.status` 和 `resource_generation_task.status` 进入 `WAITING_REVIEW`。
  - 全部资源审核通过后，`resource_generation_task.status` 与关联 `agent_task.status` 进入 `DONE`。
  - 模型结构化生成重试后仍失败时，`agent_task.status` 与 `resource_generation_task.status` 都必须持久化为 `FAILED`。
  - 对 `ModelCallFailedException` 不回滚失败记录，保证失败 trace、失败 model call 和业务任务状态可查询。

## 验收标准

- 非法 task 状态无法通过 `startRun` 写入。
- 非法 trace 状态无法通过 `recordTraceSteps` 写入。
- 失败记录包含 `FAILED` task、失败 trace、失败 model call 和 `recoverable` 字段。
- 可取消任务返回 `CANCELLED` 并追加 trace。
- 终态任务再次取消返回 `409 CONFLICT`。
- 资源生成模型失败后，不能留下对后台恢复任务有误导性的 `RUNNING` 业务任务。
