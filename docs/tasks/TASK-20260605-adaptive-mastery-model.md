# 自适应掌握度模型任务

## 任务清单

- [x] 读取 assessment 与 mastery 当前实现。
- [x] 启动参考项目研究与本地切片分析子代理。
- [x] 用失败测试覆盖已有历史掌握度时的动态更新行为。
- [x] 实现 `AssessmentService` 读取最新 `mastery_record`。
- [x] 实现 `AssessmentFeedbackService` BKT-lite 更新策略。
- [x] 跑 assessment 聚焦测试。
- [x] 跑后端全量测试。
- [x] 更新 evidence、acceptance、memory、changelog。

## Done Criteria

- `AssessmentControllerTest` 覆盖历史掌握度读取。
- `AssessmentFeedbackServiceTest` 继续覆盖错因诊断和掌握度边界。
- `mvn "-Dtest=AssessmentFeedbackServiceTest,AssessmentControllerTest" test` 通过。
- 全量 `mvn test` 通过。
