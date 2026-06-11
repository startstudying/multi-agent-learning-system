# 资源生成任务恢复状态 PRD

## 背景

P0-3 要求长任务具备幂等、重试和恢复能力。当前文档索引任务已经具备 `retry_count`、错误信息和恢复调度基础，但资源生成任务失败后只在 `agent_task` / `agent_trace` 中保存失败证据，`resource_generation_task` 自身缺少可查询的恢复状态。

## 目标

- 为资源生成长任务补齐可查询的恢复元数据。
- 让前端或 Orchestrator 能从任务详情判断是否可恢复、重试次数和下次可重试时间。
- 错误摘要使用安全错误码，不在任务恢复字段中暴露模型供应商原始错误全文。

## 非目标

- 不实现后台自动重试调度器。
- 不改变现有 `agent_task` / `agent_trace` 失败证据写入方式。
- 不新增模型供应商、依赖或外部服务。

## 用户价值

- 学生端或教师端能看到任务失败后的恢复状态。
- Orchestrator 后续可以基于统一字段做重试入口、失败筛选和恢复队列。
- 运维和审查人员可以通过业务任务表定位失败任务，而不必只依赖 trace 明细。

## 验收标准

- `resource_generation_task` 表具备 `retry_count`、`next_retry_at`、`last_error`、`recoverable` 字段。
- 模型调用失败后，资源生成任务状态为 `FAILED`，恢复字段被持久化。
- `GET /api/resources/generation-tasks/{taskId}` 返回恢复字段。
- 相关测试覆盖 migration 文本、失败持久化和任务详情响应。
