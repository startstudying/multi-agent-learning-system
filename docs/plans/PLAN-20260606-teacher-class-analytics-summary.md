# 教师端班级学习分析开发计划

## 执行方式

单 Codex 执行。此前 subagent 只读分析已收口，不再开启新的后台 agent。

## 步骤

1. 补齐 PRD、REQ、SPEC、PLAN、TASK、CONTEXT。
2. 在 `AnalyticsControllerTest` 中先写教师 class summary 失败测试。
3. 运行 `mvn "-Dtest=AnalyticsControllerTest" test` 确认 RED。
4. 在 `AnalyticsController` 暴露 `GET /api/analytics/classes/{courseId}/summary`。
5. 在 `AnalyticsService` 增加 `teacherClassSummary(courseId, currentUserId)`。
6. 注入所需 repository，按现有实体做内存聚合。
7. 运行相关测试，修复编译或断言问题。
8. 写 evidence、acceptance、changelog、memory，并更新 TODO。

## 风险

- 当前没有真实班级成员表，MVP 只能从 `LearningPath.goalId == courseId` 推断学习者。
- 当前 repository 查询粒度偏粗，MVP 可接受，后续大数据量需要补 query 方法。
- 临时 `teacher/admin` 权限模型不是完整 RBAC，后续 P3 权限任务需要替换。

## 架构漂移结果

预检查：通过。该切片只扩展 analytics 后端，不新增 Agent/RAG 执行链路，不修改数据库，不新增依赖。

后检查：通过。Controller 只新增路由并传递当前用户，Service 负责权限和聚合；待审核资源只返回元数据，不返回 `markdownContent`。
