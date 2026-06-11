# TASK-20260608 P3-4-D Course Enrollment Scope

Status: Done

## Done Criteria

- [x] 新增 `course_enrollment` V19 migration。
- [x] 新增 `CourseEnrollment` entity 和 repository。
- [x] 新增 `CourseAccessService` 集中 course read/manage/enrollment 判断。
- [x] Student course list/detail/graph 只返回 active enrolled course。
- [x] 未 enrolled student 创建 course-bound learning path 返回 `FORBIDDEN`。
- [x] 未 enrolled student 创建 course-bound resource generation task 返回 `FORBIDDEN`。
- [x] Teacher class summary learner set 来自 active enrollment。
- [x] 非 course goal 行为保持兼容。
- [x] 不新增依赖、不改 frontend。
- [x] Focused/adjacent/backend 测试完成并写入 Evidence。
- [x] Acceptance / Changelog / Memory / TODO / Retro 更新。

## Allowed Files

- `backend/src/main/resources/db/migration/V19__course_enrollment_scope.sql`
- `backend/src/main/java/com/learningos/knowledge/domain/CourseEnrollment.java`
- `backend/src/main/java/com/learningos/knowledge/repository/CourseEnrollmentRepository.java`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- `docs/evidence/EVIDENCE-20260608-course-enrollment-scope.md`
- `docs/acceptance/ACCEPT-20260608-course-enrollment-scope.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/retrospectives/RETRO-20260608-course-enrollment-scope.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/SKILL_REGISTRY.md`

## Files Not Allowed To Modify

- `backend/pom.xml`
- `frontend/**`
- `docs/superpowers/**`
- model provider / parser / vector adapter implementation files

## Test Commands

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,SchemaConvergenceMigrationTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceReviewControllerTest test
mvn test
```

## Implementation Update

- Implemented V19 `course_enrollment` migration, `CourseEnrollment`, `CourseEnrollmentRepository`, and centralized `CourseAccessService`.
- Wired student course list/detail/knowledge-graph reads to active enrollment scope.
- Wired course-bound learning path and resource generation creation to active enrollment checks while keeping non-course template goals compatible.
- Wired teacher class summary learner set to active enrollment instead of legacy learning-path inference.
- Added focused controller and migration tests for active/dropped/missing enrollment behavior.
- Verification completed with focused, adjacent, and full backend Maven tests passing. Early `mvn --% ...` attempts hit `windows sandbox: setup refresh failed`, then equivalent quoted Maven arguments were used successfully.
