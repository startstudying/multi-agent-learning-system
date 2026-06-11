# SPEC - Token / Cost 预算治理

## 1. 概述

本规格定义 P2-4 的 analytics 层 Token / 成本预算治理能力。系统基于已有审计表读取 `agent_task`、`model_call_log`、`token_usage_log` 和 `resource_generation_task`，提供管理员查询 API，输出成本聚合、预算决策、高成本任务和异常模型调用。

本规格不定义调用前预算拦截，不定义真实模型 provider 价格表，也不新增数据库结构。

## 2. 追踪

- PRD：`docs/product/PRD-20260606-token-budget-governance.md`
- REQ：`docs/requirements/REQ-20260606-token-budget-governance.md`
- TODO：`docs/planning/backend-architecture-todolist.md` P2-4

## 3. 领域模型

### 输入审计模型

- `AgentTask`：提供 `ownerUserId`、`taskType`、`status`、`traceId`、`latencyMs`。
- `ModelCallLog`：提供模型名、状态、延迟、错误码、估算成本、创建时间。
- `TokenUsageLog`：提供 prompt/completion/total token 和估算成本。
- `ResourceGenerationTask`：用于从 `agentTaskId` 关联课程或学习目标。

### 输出模型

- `TokenBudgetGovernanceSummary`
- `CostStats`
- `BudgetDecision`
- `HighCostTaskWarning`
- `AbnormalModelCall`

## 4. API 契约

### 端点

```http
GET /api/analytics/token-budget/governance
```

### Query 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `from` | Instant | 否 | 统计窗口开始 |
| `to` | Instant | 否 | 统计窗口结束 |
| `tokenBudget` | Long | 否 | 预算 Token 数 |
| `degradeThreshold` | Double | 否 | 降级阈值比例 |
| `manualConfirmationThreshold` | Double | 否 | 人工确认阈值比例 |
| `highCostTokenThreshold` | Long | 否 | 高成本任务 Token 阈值 |
| `highCostUsdThreshold` | Double | 否 | 高成本任务美元阈值 |
| `anomalyLatencyMsThreshold` | Long | 否 | 慢模型调用阈值 |

### 响应

```json
{
  "code": "OK",
  "data": {
    "costStats": {
      "promptTokens": 5300,
      "completionTokens": 3200,
      "totalTokens": 8500,
      "estimatedCost": 0.08,
      "modelCallCount": 2,
      "byUser": [],
      "byCourse": [],
      "byAgentType": [],
      "byTimeWindow": []
    },
    "budgetDecision": {
      "budgetTokens": 10000,
      "usedTokens": 8500,
      "remainingTokens": 1500,
      "status": "NEAR_BUDGET",
      "action": "DOWNGRADE_TO_DETERMINISTIC_OR_CACHED_RESPONSE",
      "requiresManualConfirmation": false,
      "fallbackStrategy": "DETERMINISTIC_OR_CACHED_RESPONSE",
      "reasonCodes": ["DEGRADE_THRESHOLD_EXCEEDED"]
    },
    "highCostTasks": [],
    "abnormalModelCalls": []
  },
  "message": ""
}
```

### 错误码

| 错误码 | 说明 | 触发条件 |
|---|---|---|
| `FORBIDDEN` | 无权限 | 当前用户不是 `admin` |
| `VALIDATION_ERROR` | 参数非法 | `from > to` 或降级阈值大于人工确认阈值 |

## 5. 前端交互

本切片不新增前端页面。后续运营看板可直接消费该 API。

## 6. 后端流程

```text
Controller 读取 query 参数和 currentUserId
-> AnalyticsService 校验 admin 权限和窗口参数
-> 读取 AgentTask / ModelCallLog / TokenUsageLog / ResourceGenerationTask
-> 按时间窗口过滤日志
-> 聚合 total / byUser / byCourse / byAgentType / byTimeWindow
-> 根据 BudgetRule 生成 BudgetDecision
-> 生成 HighCostTaskWarning 和 AbnormalModelCall
-> 返回 ApiResponse
```

## 7. Agent 工作流

本切片不新增 Agent 工作流。它消费 Agent 执行后的审计记录。

## 8. RAG 工作流

本切片不改变 RAG 检索或回答流程。`RAG_QA` 类型任务的 Token 日志可参与聚合。

## 9. 数据库变更

无新增数据库迁移。使用已有表：

- `agent_task`
- `model_call_log`
- `token_usage_log`
- `resource_generation_task`

## 10. 状态流转

预算决策状态：

```text
WITHIN_BUDGET
-> NEAR_BUDGET
-> OVER_BUDGET
```

动作映射：

| 状态 | action | fallbackStrategy |
|---|---|---|
| `WITHIN_BUDGET` | `ALLOW_MODEL_GATEWAY` | `ALLOW_MODEL_GATEWAY` |
| `NEAR_BUDGET` | `DOWNGRADE_TO_DETERMINISTIC_OR_CACHED_RESPONSE` | `DETERMINISTIC_OR_CACHED_RESPONSE` |
| `OVER_BUDGET` | `REQUIRE_MANUAL_CONFIRMATION` | `MANUAL_CONFIRMATION_REQUIRED` |

## 11. 错误处理

- 权限失败使用统一 `ApiException(ErrorCode.FORBIDDEN)`。
- 参数冲突使用 `ApiException(ErrorCode.VALIDATION_ERROR)`。
- 未关联课程或 Agent 类型时使用 `UNKNOWN_COURSE` / `UNKNOWN_AGENT_TYPE`，不抛 500。

## 12. 权限规则

- 仅 `admin` 可以调用治理 API。
- 响应不返回 prompt 原文、模型原始输出、原始 provider 错误详情。

## 13. Trace / 日志

- 本切片复用现有 `X-Trace-Id` filter。
- 不新增结构化日志字段。
- 任务级 trace/model/token 证据来自已有 Agent 记录。

## 14. 测试策略

- `AnalyticsControllerTest` 覆盖聚合维度、预算决策、高成本告警、异常模型调用。
- 重点断言：
  - `byUser`
  - `byCourse`
  - `byAgentType`
  - `byTimeWindow`
  - `NEAR_BUDGET`
  - `OVER_BUDGET`
  - `HIGH_TOKEN_USAGE`
  - `MODEL_CALL_FAILED`
  - `MODEL_CALL_HIGH_LATENCY`

## 15. 验收清单

- [x] 功能需求 FR-01 至 FR-10 已满足。
- [x] API 契约已验证。
- [x] 权限规则已落地。
- [x] 不新增数据库迁移。
- [x] 测试通过。
- [x] P2-4 TODO 可标记为 analytics 层完成。
