# Agent Trace 治理看板后端最小切片 SPEC

## API

### `GET /api/agent/traces`

查询参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| `userId` | string | 过滤 `agent_task.ownerUserId` |
| `agentType` | string | 过滤 `agent_task.taskType` 或 trace 中的 `agentName` |
| `status` | string | 过滤 `agent_task.status` |
| `from` | ISO-8601 instant | 过滤 `agent_task.createdAt >= from` |
| `to` | ISO-8601 instant | 过滤 `agent_task.createdAt <= to` |
| `failureReason` | string | 过滤失败摘要或任务输出中的安全失败原因 |

响应：

```json
{
  "code": "OK",
  "message": "OK",
  "data": {
    "items": [
      {
        "taskId": "agt_x",
        "traceId": "trace_x",
        "userId": "alice",
        "agentType": "RESOURCE_GENERATION",
        "status": "FAILED",
        "failureReason": "MODEL_PROVIDER_ERROR",
        "latencyMs": 88,
        "createdAt": "2026-06-06T00:00:00Z",
        "updatedAt": "2026-06-06T00:00:01Z",
        "stepCount": 1,
        "toolCallCount": 1
      }
    ]
  }
}
```

### `GET /api/agent/tasks/{taskId}/trace`

新增字段：

```json
{
  "data": {
    "toolCalls": [
      {
        "toolCallId": "atc_x",
        "toolName": "CourseRagTool",
        "status": "DONE",
        "inputSummary": "{\"query\":\"[REDACTED]\"}",
        "outputSummary": "{\"matches\":3}",
        "errorMessage": null,
        "latencyMs": 35
      }
    ],
    "retentionPolicy": {
      "auditRetentionDays": 365,
      "textRetentionDays": 30,
      "longTermAuditFields": ["taskId", "traceId"],
      "cleanableTextFields": ["agent_tool_call.inputJson"]
    }
  }
}
```

## 数据模型

现有 `agent_tool_call` 表已在 V2 创建。V15 最小迁移只补齐治理查询需要的兼容列与索引：

- `trace_id varchar(120)`
- `input_summary varchar(2000)`
- `output_summary varchar(2000)`
- `retention_class varchar(40)`
- `idx_agent_tool_call_trace (trace_id)`
- `idx_agent_tool_call_task_status (agent_task_id, status)`

JPA 实体使用 camelCase 字段映射到 snake_case 列，服务层保存时同时填充 `inputJson/outputJson` 和 `inputSummary/outputSummary`，保证旧表结构与新展示字段兼容。

## 服务行为

- `AgentRunRecorder.recordToolCall(...)`
  - 校验 task 存在。
  - 校验 status 属于 trace status 集合。
  - 清理输入、输出、错误摘要。
  - 保存 `agent_tool_call`。

- `AgentTraceGovernanceService.searchTraces(...)`
  - 从 `AgentTaskRepository` 查询任务。
  - 在服务层做最小过滤和聚合，避免新增复杂依赖。
  - 非 admin/teacher 用户只允许查看自己的 trace；`admin` 和 `teacher*` 可按 `userId` 过滤。

- `AgentTraceGovernanceService.getTrace(...)`
  - 校验访问权限。
  - 聚合 `agent_trace` 与 `agent_tool_call`。
  - 返回 retention policy。

## 敏感字段清理

清理规则：

- 匹配 `password`、`secret`、`apiKey`、`token`、`authorization` 的字段值替换为 `[REDACTED]`。
- 含有 `privateDocument`、`rawDocument`、`fullText`、`courseDocument` 的字段值替换为 `[REDACTED_DOCUMENT]`。
- 摘要长度限制为 500 字符。
- 空输出保存为 `{}`。

## 架构漂移检查

- Controller 只处理 HTTP 和 current user 提取。
- 查询、权限、聚合、清理都在 service/recorder 层。
- 不让 Agent Tool 直接访问 repository。
- 不新增依赖。
