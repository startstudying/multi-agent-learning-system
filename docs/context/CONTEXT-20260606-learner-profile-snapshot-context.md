# Learner Profile 维度与快照 Context Pack

## 当前任务边界

补齐 P1-1 中画像维度、维度证据字段，以及路径规划和资源生成的 `profile_snapshot`。

## 允许修改文件

- `backend/src/main/java/com/learningos/learning/dto/ProfileDimension.java`
- `backend/src/main/java/com/learningos/learning/dto/ProfileStructuredFields.java`
- `backend/src/main/java/com/learningos/learning/dto/LearningPathResponse.java`
- `backend/src/main/java/com/learningos/learning/domain/LearningPath.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/java/com/learningos/agent/dto/ResourceGenerationResponse.java`
- `backend/src/main/java/com/learningos/agent/domain/ResourceGenerationTask.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/resources/db/migration/V10__profile_snapshot_context.sql`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- 本切片相关 docs 文件
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 禁止修改

- 不改前端。
- 不新增依赖。
- 不新增独立画像维度表。
- 不改真实模型调用或 prompt 版本。
- 不改 Assessment 掌握度算法。

## 验证命令

```powershell
cd backend
mvn "-Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,SchemaConvergenceMigrationTest" test
```
