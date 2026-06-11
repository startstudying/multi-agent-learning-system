# Evaluation Set 管理任务

## TASK-1 文档和范围

- [x] 明确本轮只做 set/sample 管理。
- [x] 明确不做 run/compare。
- [x] 记录子任务只读分析。

## TASK-2 RED 测试

- [x] 新增 Service 测试。
- [x] 新增 Controller 测试。
- [x] 新增 V13 migration 文本测试。
- [x] 运行并确认 RED。

## TASK-3 GREEN 实现

- [x] 新增 EvaluationSet / EvaluationSample 实体和 Repository。
- [x] 新增 DTO、Service、Controller。
- [x] 新增 V13 migration。
- [x] 保持 Controller -> Service -> Repository 分层。

## TASK-4 验证和收尾

- [x] 运行 focused 测试。
- [x] 运行相关回归测试。
- [x] 更新 TODO、evidence、acceptance、memory、changelog、retro。

## Done Criteria

- TODO 第 142 行勾选。
- 三类评估集样本可持久化和查询。
- 学生无法访问评估集管理 API。
- focused 和相关回归测试通过。
