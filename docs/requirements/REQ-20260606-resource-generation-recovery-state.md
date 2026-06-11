# 资源生成任务恢复状态需求

## 功能需求

1. 资源生成任务创建成功时，默认恢复状态为：
   - `retryCount = 0`
   - `recoverable = false`
   - `lastError = null`
   - `nextRetryAt = null`
2. 资源生成模型调用失败时，任务必须持久化：
   - `status = FAILED`
   - `progressPercent = 0`
   - `retryCount = 1`
   - `recoverable = true`
   - `lastError = MODEL_CALL_FAILED`
   - `nextRetryAt` 为未来时间
3. 任务详情响应必须返回上述恢复字段。
4. 数据库 migration 必须对已有 `resource_generation_task` 表补列，并保持可重复执行风格。

## 安全需求

- `lastError` 只保存稳定错误码，不保存供应商原始错误全文。
- 原始错误仍只允许留在已有 trace / model call log 调试链路中。

## 兼容需求

- 不改变现有创建任务、查询任务、幂等 replay、review gate 的行为。
- 成功任务新增字段应为默认值，不影响旧 JSON 消费方。

## 测试需求

- 先写 RED 测试验证字段缺失。
- GREEN 后运行资源生成控制器测试和 schema migration 测试。
