# 资源生成任务恢复状态 SPEC

## 数据模型

表：`resource_generation_task`

新增字段：

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `retry_count` | `int` | `not null default 0` | 当前任务已经进入恢复候选的失败次数 |
| `next_retry_at` | `timestamp` | nullable | 下次建议重试时间 |
| `last_error` | `varchar(120)` | nullable | 安全错误码 |
| `recoverable` | `boolean` | `not null default false` | 是否允许恢复或人工重试 |

## API 合同

`ResourceGenerationResponse` 新增字段：

```json
{
  "retryCount": 0,
  "nextRetryAt": null,
  "lastError": null,
  "recoverable": false
}
```

失败任务示例：

```json
{
  "status": "FAILED",
  "progressPercent": 0,
  "retryCount": 1,
  "nextRetryAt": "2026-06-06T10:05:00Z",
  "lastError": "MODEL_CALL_FAILED",
  "recoverable": true
}
```

## 服务行为

- `ResourceGenerationService.createTaskWithContext(...)` 创建任务时写入默认恢复状态。
- `AiModelGateway.ModelCallFailedException` 捕获分支中：
  - 将任务标记为 `FAILED`
  - `progressPercent` 置为 `0`
  - `retryCount` 至少递增为 `1`
  - `recoverable` 置为 `true`
  - `lastError` 置为 `MODEL_CALL_FAILED`
  - `nextRetryAt` 设置为当前时间后 5 分钟
- 不新增自动 retry worker。

## 失败策略

`lastError` 使用稳定错误码，不包含 provider 原始 message。后续如果要展示详细错误，应通过受权限保护的 trace/detail API 读取。

## 影响范围

- Backend entity / DTO / service
- Flyway migration
- Controller integration test
- Migration text test
