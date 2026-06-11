# PRD - Token / Cost 预算治理

## 1. 问题陈述

后端已经记录 `model_call_log`、`token_usage_log` 和 `agent_task`，但 P2-4 需要把这些审计记录转化为可查询的成本治理视图：管理员需要按用户、课程、Agent 类型和时间窗口理解 Token 消耗，判断是否接近或超过预算，并识别高成本任务和异常模型调用。

本切片收敛的是 analytics 层的治理视图，不改变真实模型调用入口，也不新增调用前预算门禁。调用前拦截、真实 provider 成本计价和外部告警属于后续 P3 生产化切片。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 平台管理员 | admin | 查看 Token / 成本聚合、预算状态和异常调用 |
| 运维人员 | ops | 快速定位高成本任务和失败/慢调用 |
| 后端开发者 | engineer | 基于统一 API 验证成本治理数据闭环 |

## 3. 用户故事

- 作为平台管理员，我希望按时间窗口查看 Token 使用量和成本，以便判断当前预算风险。
- 作为平台管理员，我希望系统给出预算动作建议，例如降级到 deterministic/cached response 或要求人工确认，以便控制成本。
- 作为运维人员，我希望看到高成本任务和异常模型调用，以便定位失败、慢调用或异常成本来源。

## 4. MVP 范围

### 纳入范围

- 增加 `/api/analytics/token-budget/governance` 后端查询能力。
- 支持按用户、课程、Agent 类型、时间窗口聚合 Token 和成本。
- 返回预算状态、预算动作、降级策略和人工确认标记。
- 返回高成本任务告警和异常模型调用列表。
- 仅允许 `admin` 查询。

### 非目标

- 不接入真实模型 provider。
- 不在 `AiModelGateway` 调用前执行预算拦截。
- 不新增数据库表或迁移。
- 不新增前端页面。
- 不接入外部告警系统。

## 5. 成功指标

| 指标 | 目标值 | 衡量方式 |
|---|---|---|
| 聚合维度完整性 | 覆盖 user/course/agentType/timeWindow | MockMvc 断言响应字段 |
| 预算决策 | 支持 `WITHIN_BUDGET` / `NEAR_BUDGET` / `OVER_BUDGET` | Controller 测试 |
| 风险识别 | 返回高成本任务和异常模型调用 | Controller 测试 |
| 权限边界 | 非 admin 返回 `FORBIDDEN` | Service/API 测试 |

## 6. 用户流程

```text
管理员发起治理查询
-> 后端读取模型调用和 Token 使用日志
-> 按筛选窗口聚合成本
-> 计算预算状态和动作建议
-> 返回高成本任务与异常模型调用
```

## 7. 依赖关系

- 上游依赖：`agent_task`、`model_call_log`、`token_usage_log`、`resource_generation_task` 已有审计记录。
- 下游影响：analytics API 和后续运维看板可消费该治理视图。
- 阻塞：无。调用前预算门禁另行设计。

## 8. 待澄清问题

| 问题 | 负责人 | 状态 |
|---|---|---|
| 真实 provider 价格表如何维护 | 后续 P3 模型接入任务 | 暂不纳入 |
| 预算门禁应在 Orchestrator 还是 `AiModelGateway` 执行 | 后续 P3 成本治理任务 | 暂不纳入 |

## 9. 审批

| 角色 | 姓名 | 日期 | 状态 |
|---|---|---|---|
| Main Codex | Codex | 2026-06-06 | 已执行 |
