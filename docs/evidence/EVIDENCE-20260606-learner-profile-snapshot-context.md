# Learner Profile 维度与快照证据

## 1. 追踪

- PRD：`docs/product/PRD-20260606-learner-profile-snapshot-context.md`
- REQ：`docs/requirements/REQ-20260606-learner-profile-snapshot-context.md`
- SPEC：`docs/specs/SPEC-20260606-learner-profile-snapshot-context.md`
- TASK：`docs/tasks/TASK-20260606-learner-profile-snapshot-context.md`
- 日期：2026-06-06

## 2. 实现内容

- `ProfileDimension` 增加 `lastEvidenceId`。
- 学习画像结构化字段扩展为七个产品维度：`baseline_level`、`learning_goal`、`weak_point`、`preference`、`pace_and_feedback`、`recent_error_pattern`、`teacher_note`。
- `LearningPath` / `LearningPathResponse` 增加 `profileSnapshot`，创建和查询路径时返回同一份快照。
- `ResourceGenerationTask` / `ResourceGenerationResponse` 增加 `profileSnapshot`，创建、查询和 requestId replay 时保持快照一致。
- `ResourceGenerationService` 将 `profileSnapshot` 放入模型请求 payload，资源生成显式引用画像上下文。
- 新增 `V10__profile_snapshot_context.sql`，为 `learning_path` 和 `resource_generation_task` 增加 `profile_snapshot`。
- 快照读取兼容历史字段别名：`knowledge_base` -> `baseline_level`，`resource_preference` -> `preference`，`error_pattern` -> `recent_error_pattern`；同时合并 `weakPointsJson` 和 `preferencesJson`。

## 3. TDD 证据

### RED 1：新增字段和快照缺失

命令：

```powershell
cd backend
mvn "-Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,SchemaConvergenceMigrationTest" test
```

失败摘要：

```text
ResourceGenerationTask missing getProfileSnapshot()
LearningPathResponse / ResourceGenerationResponse missing profileSnapshot expectations
V10__profile_snapshot_context.sql resource was missing
```

### GREEN 1：P1-1 主链路通过

命令：

```powershell
cd backend
mvn "-Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,SchemaConvergenceMigrationTest" test
```

结果：

```text
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### RED 2：历史画像字段别名兼容

命令：

```powershell
cd backend
mvn "-Dtest=LearningWorkflowControllerTest#learningPathSnapshotKeepsHistoricalProfileFieldAliases,ResourceGenerationControllerTest#resourceGenerationSnapshotKeepsHistoricalProfileFieldAliases" test
```

失败摘要：

```text
profileSnapshot did not contain Legacy Java basics / Legacy SQL weak point from historical profile fields.
```

### GREEN 2：别名和 profile 列 fallback 通过

命令：

```powershell
cd backend
mvn "-Dtest=LearningWorkflowControllerTest#learningPathSnapshotKeepsHistoricalProfileFieldAliases,ResourceGenerationControllerTest#resourceGenerationSnapshotKeepsHistoricalProfileFieldAliases" test
```

结果：

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 相关回归

命令：

```powershell
cd backend
mvn "-Dtest=LearningWorkflowControllerTest,LearningWorkflowServiceTest,ResourceGenerationControllerTest,SchemaConvergenceMigrationTest,OrchestratorWorkflowControllerTest" test
```

结果：

```text
Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 4. 架构检查

- [x] Controller 保持薄层。
- [x] 画像快照生成、持久化和读取在 Service 层完成。
- [x] 未新增依赖。
- [x] 数据库变更有 V10 migration。
- [x] 资源生成模型请求 payload 显式包含画像快照。
- [x] 无真实画像时返回稳定空快照，避免路径规划和资源生成失败。

## 5. 已知限制

- `profileSnapshot` 当前是 JSON 字符串，后续如需高频分析查询，应考虑结构化快照表或 JSON 索引。
- `teacher_note` 在没有教师输入时会保留低置信默认值，后续教师端增强时可改为显式空状态。
- V10 migration 目前通过文本测试覆盖，真实 MySQL 8 Flyway smoke 仍属于 P3。
