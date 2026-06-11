# 证据文档 - Token / Cost 预算治理

## 1. 追踪

- TASK：`docs/tasks/TASK-20260606-token-budget-governance.md`
- SPEC：`docs/specs/SPEC-20260606-token-budget-governance.md`
- 日期：2026-06-06

## 2. 实现内容

当前代码已经实现 P2-4 analytics 层 Token / 成本预算治理能力：

- `GET /api/analytics/token-budget/governance`
- 按用户、课程、Agent 类型、时间窗口聚合 Token 和成本
- 预算状态和动作建议
- 高成本任务告警
- 异常模型调用识别
- `admin` 权限限制

本轮补齐缺失的 workflow 文档和验收证据，不新增生产代码。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `docs/product/PRD-20260606-token-budget-governance.md` | 新增 | 产品目标和范围 |
| `docs/requirements/REQ-20260606-token-budget-governance.md` | 新增 | 功能与非功能需求 |
| `docs/specs/SPEC-20260606-token-budget-governance.md` | 新增 | API、状态和测试规格 |
| `docs/plans/PLAN-20260606-token-budget-governance.md` | 新增 | 实施计划 |
| `docs/tasks/TASK-20260606-token-budget-governance.md` | 新增 | 执行任务 |
| `docs/context/CONTEXT-20260606-token-budget-governance.md` | 更新 | Context Pack |
| `docs/acceptance/ACCEPT-20260606-token-budget-governance.md` | 新增 | 验收报告 |
| `docs/retrospectives/RETRO-20260606-token-budget-governance.md` | 新增 | 复盘 |

## 4. 测试结果

### 执行的命令

```bash
cd backend; mvn "-Dtest=AnalyticsControllerTest" test
```

### 结果

| 命令 | 结果 | 备注 |
|---|---|---|
| `cd backend; mvn "-Dtest=AnalyticsControllerTest" test` | 通过 | Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 |

关键输出：

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 5. 覆盖的验收点

- `tokenBudgetGovernanceGroupsCostStatsByUserCourseAgentTypeAndTimeWindow`
  - 覆盖 user/course/agentType/timeWindow 聚合。
  - 覆盖 `NEAR_BUDGET` 和降级建议。
- `tokenBudgetGovernanceRequiresManualConfirmationWhenBudgetIsExceeded`
  - 覆盖 `OVER_BUDGET` 和人工确认动作。
- `tokenBudgetGovernanceFlagsHighCostTasksAndAbnormalModelCalls`
  - 覆盖高成本任务、模型调用失败和高延迟识别。

## 6. 架构漂移检查

- [x] Controller 仅处理 HTTP 参数和 current-user，委托 Service。
- [x] Service 包含业务聚合与预算决策。
- [x] 不新增依赖。
- [x] 不新增数据库迁移。
- [x] 不暴露 prompt 原文或 provider 敏感错误。
- [x] 不让前端或业务服务直接调用 LLM API。

## 7. 已知限制

- 当前治理能力位于 analytics 查询层，不是调用前预算门禁。
- 当前聚合使用 repository `findAll()` 后内存过滤，生产大数据量下需要 P3 优化为数据库聚合。
- 当前项目根目录没有 `.git`，无法使用 git diff 作为变更证据；本证据基于当前文件和新鲜测试输出。
- 测试输出包含 Mockito 动态 agent 的 JDK 未来兼容警告，不影响本次测试结果。

## 8. 评审备注

Backend / Spec Architect 子代理确认：P2-4 的治理视图已经实现，但 TODO 和 workflow 文档滞后；调用前预算门禁应作为后续 P3 任务处理。
