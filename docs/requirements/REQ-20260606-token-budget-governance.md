# REQ - Token / Cost 预算治理

## 1. 追踪

- PRD：`docs/product/PRD-20260606-token-budget-governance.md`
- 需求编号：REQ-20260606-token-budget-governance

## 2. 功能需求

| 编号 | 需求描述 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | 系统必须提供 Token / 成本治理查询 API。 | 必须 | `GET /api/analytics/token-budget/governance` 返回统一 envelope。 |
| FR-02 | 查询必须支持 `from` / `to` 时间窗口。 | 必须 | 窗口外日志不参与聚合，`from > to` 返回 `VALIDATION_ERROR`。 |
| FR-03 | 结果必须按用户聚合 Token 和成本。 | 必须 | 响应包含 `costStats.byUser[]`。 |
| FR-04 | 结果必须按课程聚合 Token 和成本。 | 必须 | 响应包含 `costStats.byCourse[]`，未知课程归入 `UNKNOWN_COURSE`。 |
| FR-05 | 结果必须按 Agent 类型聚合 Token 和成本。 | 必须 | 响应包含 `costStats.byAgentType[]`。 |
| FR-06 | 结果必须返回预算决策。 | 必须 | 响应包含 `budgetDecision.status`、`action`、`fallbackStrategy`、`requiresManualConfirmation`。 |
| FR-07 | 超过降级阈值时必须建议降级到 deterministic/cached response。 | 必须 | `NEAR_BUDGET` 返回 `DOWNGRADE_TO_DETERMINISTIC_OR_CACHED_RESPONSE`。 |
| FR-08 | 超过人工确认阈值时必须要求人工确认。 | 必须 | `OVER_BUDGET` 返回 `REQUIRE_MANUAL_CONFIRMATION`。 |
| FR-09 | 结果必须识别高成本任务。 | 必须 | 响应包含 `highCostTasks[]` 和原因码。 |
| FR-10 | 结果必须识别异常模型调用。 | 必须 | 响应包含失败、高延迟或高成本模型调用。 |

## 3. 非功能需求

| 编号 | 需求描述 | 优先级 |
|---|---|---|
| NFR-01 | 仅 `admin` 可以访问治理 API。 | 必须 |
| NFR-02 | 不暴露 prompt 原文、原始模型输出或 provider 敏感错误。 | 必须 |
| NFR-03 | 不新增外部依赖。 | 必须 |
| NFR-04 | 不新增数据库迁移。 | 必须 |
| NFR-05 | 保持现有 analytics API 兼容。 | 必须 |

## 4. 用户流程

```text
admin
-> GET /api/analytics/token-budget/governance
-> 后端读取审计记录
-> 过滤时间窗口
-> 聚合成本维度
-> 计算预算决策
-> 返回治理摘要
```

## 5. 输入 / 输出

### 输入

| 字段 | 类型 | 必填 | 校验规则 |
|---|---|---|---|
| `from` | Instant | 否 | ISO date-time |
| `to` | Instant | 否 | ISO date-time，若同时存在必须不早于 `from` |
| `tokenBudget` | Long | 否 | 正数，否则使用默认值 |
| `degradeThreshold` | Double | 否 | 正数，且不大于 `manualConfirmationThreshold` |
| `manualConfirmationThreshold` | Double | 否 | 正数 |
| `highCostTokenThreshold` | Long | 否 | 正数 |
| `highCostUsdThreshold` | Double | 否 | 正数 |
| `anomalyLatencyMsThreshold` | Long | 否 | 正数 |

### 输出

| 字段 | 类型 | 说明 |
|---|---|---|
| `costStats` | Object | 总 Token、总成本、模型调用数和维度聚合 |
| `budgetDecision` | Object | 预算状态和动作建议 |
| `highCostTasks` | Array | 高成本任务告警 |
| `abnormalModelCalls` | Array | 异常模型调用列表 |

## 6. 边界情况

| 场景 | 预期行为 |
|---|---|
| 非 admin 查询 | 返回 `FORBIDDEN` |
| `from` 晚于 `to` | 返回 `VALIDATION_ERROR` |
| 无数据 | 返回空聚合和默认预算状态 |
| 阈值参数缺失或非法 | 使用默认正数阈值 |
| 降级阈值大于人工确认阈值 | 返回 `VALIDATION_ERROR` |

## 7. 依赖关系

- 上游依赖：`AgentTaskRepository`、`ModelCallLogRepository`、`TokenUsageLogRepository`、`ResourceGenerationTaskRepository`。
- 下游影响：管理员运维视图和后续预算门禁设计。

## 8. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-06 | 已执行 |
