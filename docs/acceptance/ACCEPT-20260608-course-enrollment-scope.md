# ACCEPT-20260608 P3-4-D Course Enrollment Scope

## 1. 验收结论

当前状态：**通过自动化验收**。

说明：早期 `mvn --% ...` 命令触发过工具层 `windows sandbox: setup refresh failed`，随后改用等价 Maven 参数格式完成 focused、adjacent、full backend 验证。

## 2. 验收项检查

| 验收项 | 当前状态 | 证据 |
|---|---|---|
| 新增 `course_enrollment` V19 migration | PASS | `backend/src/main/resources/db/migration/V19__course_enrollment_scope.sql` |
| 新增 `CourseEnrollment` entity / repository | PASS | `CourseEnrollment.java` / `CourseEnrollmentRepository.java` |
| 新增 `CourseAccessService` 集中授权 | PASS | `CourseAccessService.java` |
| Student course list/detail/graph 只允许 active enrolled course | PASS | `KnowledgeCatalogService.java` + `CourseKnowledgeControllerTest.java` |
| course-bound learning path 未报名返回 `FORBIDDEN` | PASS | `LearningWorkflowService.java` + `LearningWorkflowControllerTest.java` |
| course-bound resource generation 未报名返回 `FORBIDDEN` | PASS | `ResourceGenerationService.java` + `ResourceGenerationControllerTest.java` |
| Teacher class summary learner set 来自 active enrollment | PASS | `AnalyticsService.java` + `AnalyticsControllerTest.java` |
| 非 course goal 保持兼容 | PASS | Learning path / resource generation tests |
| 不新增依赖、不改 frontend | 已遵守 | 未修改 `backend/pom.xml` / `frontend/**` |
| Evidence / Acceptance / Memory / Changelog / Retro 更新 | PASS | 本文件及相关文档 |

## 3. 已执行验证命令

```powershell
cd backend
mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,SchemaConvergenceMigrationTest" test
mvn "-Dtest=ResourceReviewControllerTest" test
mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceReviewControllerTest" test
mvn test
```

结果：

- Focused: `Tests run: 74, Failures: 0, Errors: 0, Skipped: 0`
- Resource review adjacent fix: `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`
- Adjacent: `Tests run: 87, Failures: 0, Errors: 0, Skipped: 0`
- Full backend: `Tests run: 312, Failures: 0, Errors: 0, Skipped: 1`

如本地 MySQL 凭据可用，再补跑：

```powershell
cd backend
mvn --% -Dtest=MysqlMigrationSmokeTest -Dlearningos.mysql.smoke=true test
```

## 4. 残留风险

- MySQL smoke 可能继续受本地 3306 `root` 凭据限制影响；若失败，应区分环境凭据失败与 V19 migration 失败。
- 本切片未完成 answer record 详情/list RBAC 矩阵，不应把 P3-4 整体标为 Done。
