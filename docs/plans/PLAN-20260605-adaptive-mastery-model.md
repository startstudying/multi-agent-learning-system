# 自适应掌握度模型计划

## 执行顺序

1. 阅读 assessment、learning、mastery repository 当前实现。
2. 对比参考项目，确定最小切片为“答题反馈闭环使用真实历史掌握度”。
3. 先写失败测试，证明旧实现固定返回 `0.42 -> 0.58`。
4. 在 `AssessmentService` 中读取最新 `mastery_record`。
5. 在 `AssessmentFeedbackService` 中增加知识点推导、默认初始值、BKT-lite 更新策略。
6. 调整测试期望并跑聚焦测试。
7. 更新 TODO、evidence、acceptance、memory、changelog。
8. 跑全量后端测试。

## 风险

- 当前还没有 `Question` JPA 实体，知识点只能从 `questionId` 规则推导。
- BKT-lite 是确定性规则，不等同完整 BKT/IRT。
- 答题提交仍未实现 `requestId` 幂等，重复提交仍可能产生多条记录。

## 回滚方式

只涉及 assessment 服务和测试，无数据库迁移。若需要回滚，可恢复 `AssessmentFeedbackService.evaluate` 的原固定掌握度逻辑，并移除 `AssessmentService` 中读取历史 mastery 的代码。
