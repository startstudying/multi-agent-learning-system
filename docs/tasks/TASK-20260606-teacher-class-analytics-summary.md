# 教师端班级学习分析任务拆解

## Task 1 文档和测试

- [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
- [x] 添加教师查询自己课程 summary 的 RED 测试。
- [x] 添加非课程教师 forbidden 测试。
- [x] 添加 admin 可查询测试。
- [x] 添加空课程数据测试。

## Task 2 后端实现

- [x] `AnalyticsController` 新增 class summary endpoint。
- [x] `AnalyticsService` 注入课程、知识点、路径、资源任务、资源、review repository。
- [x] 实现权限校验。
- [x] 实现弱知识点、错因分布、资源完成率、待审核资源聚合。

## Task 3 验证和交付

- [x] 运行 `cd backend; mvn "-Dtest=AnalyticsControllerTest" test`。
- [x] 创建 evidence 文档。
- [x] 创建 acceptance 文档。
- [x] 更新 changelog。
- [x] 更新 memory。
- [x] 更新 backend TODO。

## Done Criteria

- 新 API 返回稳定 envelope。
- 权限测试覆盖教师、其他教师、学生、admin。
- 空数据不 500。
- 测试通过或限制被记录。
- 文档和 memory 更新。
