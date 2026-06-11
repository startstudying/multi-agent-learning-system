# PLAN-20260608 P3-4-D Course Enrollment Scope

## 1. 实施步骤

1. [x] RED：新增 course enrollment 权限测试，证明学生未 enrollment 不能读/用 course。
2. [x] RED：新增 teacher class summary 测试，证明 legacy learning path 不再决定 learner set。
3. [x] GREEN：新增 V19 migration、`CourseEnrollment`、`CourseEnrollmentRepository`。
4. [x] GREEN：新增 `CourseAccessService` 并接入 `KnowledgeCatalogService`。
5. [x] GREEN：接入 `LearningWorkflowService.createPathForUser`。
6. [x] GREEN：接入 `ResourceGenerationService.createTask/createTaskInWorkflow`。
7. [x] GREEN：接入 `AnalyticsService.teacherClassSummary`。
8. [x] 更新 migration 静态测试与 MySQL smoke 最新版本常量。
9. [x] 运行 focused/adjacent/full backend 测试。
10. [x] 更新 Evidence / Acceptance / Changelog / Memory / TODO / Retro / Skill。

## 2. 风险与缓解

| Risk | Mitigation |
|---|---|
| 现有测试用 course goal 但没有 enrollment | 只对真实存在的 courseId 启用 enrollment 检查，并在相关测试中显式 seed enrollment。 |
| 业务需要非课程 goal | `goalId` 不存在于 `course` 表时保持原 template goal 行为。 |
| enrollment API 缺失导致无法线上管理 | 本切片只建立授权 source of truth；管理 API 单独后续切片。 |
| answer record 仍未完成矩阵 | 在 TODO/Acceptance 中明确保留下一切片，不误标完成。 |

## 3. 无新增依赖说明

不修改 `backend/pom.xml`，不需要 dependency review。

## 4. 测试命令

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,SchemaConvergenceMigrationTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceReviewControllerTest test
mvn test
```

MySQL smoke 若本机凭据可用：

```powershell
cd backend
mvn --% -Dtest=MysqlMigrationSmokeTest -Dlearningos.mysql.smoke=true test
```

## 5. Implementation Update

- Steps 3-8 have been implemented in code: V19 schema, entity/repository, `CourseAccessService`, and service integrations.
- Steps 1-2 RED tests were added. Early `mvn --% ...` execution was blocked by `windows sandbox: setup refresh failed`, then equivalent quoted Maven commands were used for GREEN verification.
- Step 9 completed: focused, adjacent, and full backend Maven verification passed.
- Step 10 completed: Evidence / Acceptance / Changelog / Memory / TODO / Retro / Skill documents were updated.
