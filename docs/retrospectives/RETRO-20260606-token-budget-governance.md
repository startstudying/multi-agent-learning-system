# RETRO - Token / Cost 预算治理

## 1. 结果

P2-4 analytics 层治理视图已经由现有后端代码实现，本轮补齐 workflow 文档、测试证据和验收报告。聚合维度、预算决策、高成本任务和异常模型调用均由 `AnalyticsControllerTest` 覆盖。

## 2. 做得好的地方

- 沿用现有 `AnalyticsController` / `AnalyticsService` 边界，没有扩散模型治理逻辑。
- 没有新增依赖和数据库迁移。
- 响应不暴露 prompt 原文或模型原始输出。
- 通过专家子代理识别了“治理视图”和“调用前门禁”的边界差异。

## 3. 问题

- 代码实现先于 workflow 文档完成，导致 TODO 状态和项目记忆滞后。
- 当前实现使用内存聚合，不适合长期生产数据量。
- 预算规则目前只能查询和提示，不能阻止高成本模型调用。

## 4. 后续动作

| 动作 | 负责人 | 去向 |
|---|---|---|
| 在 P3 中设计 `BudgetDecisionService` 或等价调用前门禁 | Backend Expert | 后续 P3 成本治理 |
| 将 analytics 聚合改为 repository / SQL 聚合 | Backend Expert | 后续 P3 性能优化 |
| 将高成本/异常调用接入 Micrometer 或告警事件 | Security & Quality / Backend Expert | P3-5 |

## 5. Skill Extraction

本轮不新增项目专用 skill。可复用模式是“先识别已实现但文档滞后的计划项，再用测试证据回填 workflow closure”，暂不沉淀为独立 skill。
