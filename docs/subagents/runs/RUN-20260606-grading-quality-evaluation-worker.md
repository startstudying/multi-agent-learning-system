# 自动批改质量评估后端 worker 记录

## Worker 定位

当前执行者是并行后端 worker，负责 TODO P2-3「自动批改质量评估」最小可验收后端切片。

## 输入约束

- 不修改 rag/agent 相关模块。
- 不修改 `evaluation` 包和 V14 migration。
- 不修改共享收口文件：`docs/changelog/CHANGELOG.md`、`docs/memory/*`、`docs/planning/backend-architecture-todolist.md`。
- 尽量不改 `AssessmentService` 答题提交流程。

## 分析结论

- 现有 `GradingEvaluationService` 仅支持 `humanScores` / `aiScores` 两组分数和阈值一致率。
- P2-3 缺口集中在请求样本结构、等级一致率、错因一致率和三类分组分析。
- 最小切片可完全在 assessment grading evaluation 相关 DTO / service / controller 内完成，不需要数据库和新依赖。

## 集成建议

- 保留旧字段兼容，避免影响已有测试和调用方。
- `agreementRate` 保留为旧响应字段，值与新版 `gradeAgreementRate` 对齐。
- 后续主协调者如需完整 Done Definition，可统一更新共享 memory/changelog/todolist。

## 状态

- 文档：已创建。
- RED/GREEN：待执行。
- Evidence/Acceptance：待测试后补齐。
