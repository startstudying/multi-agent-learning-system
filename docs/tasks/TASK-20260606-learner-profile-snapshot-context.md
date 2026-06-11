# Learner Profile 维度与快照任务拆解

## Task 1 文档和并行分析

- [x] 读取 P1-1 TODO 和现有 profile/path/resource 代码。
- [x] 启动只读子 Agent 做 schema/API 和测试设计分析。
- [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / subagent run。

## Task 2 TDD RED

- [x] 在 `LearningWorkflowControllerTest` 增加 profile 维度和 path snapshot 断言。
- [x] 在 `ResourceGenerationControllerTest` 增加 resource generation snapshot 断言。
- [x] 在 `SchemaConvergenceMigrationTest` 增加 V10 migration 断言。
- [x] 运行最小测试并确认 RED。

## Task 3 后端实现

- [x] 扩展 `ProfileDimension.lastEvidenceId`。
- [x] 扩展 `ProfileStructuredFields` 七个维度。
- [x] 扩展 `LearningPath` / `LearningPathResponse.profileSnapshot`。
- [x] 扩展 `ResourceGenerationTask` / `ResourceGenerationResponse.profileSnapshot`。
- [x] 新增 V10 migration。
- [x] 在 path/resource 服务中保存画像快照。

## Task 4 验证和交付

- [x] 运行 profile/path/resource/migration 相关测试。
- [x] 更新 evidence / acceptance。
- [x] 更新 changelog / PROJECT/BACKEND/API memory。
- [x] 更新 backend TODO。
- [x] 创建 retrospective。

## Done Criteria

- 新画像维度包含七个明确字段。
- 每个新画像维度包含 `confidence`、`sourceType`、`lastEvidenceId`。
- `LearningPathResponse.profileSnapshot` 新创建和查询都存在。
- `ResourceGenerationResponse.profileSnapshot` 新创建、查询和 replay 都存在。
- V10 migration 存在且被测试覆盖。
