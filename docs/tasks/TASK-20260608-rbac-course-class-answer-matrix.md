# TASK-20260608 P3-4-C 权限矩阵安全前置

Status: Done

## Done Criteria

- [x] `GET /api/courses` 对 admin/teacher/student 返回符合 SPEC 的 scoped list。
- [x] `GET /api/courses/{courseId}` 对非 admin missing/foreign 返回 `FORBIDDEN` 且无 `data`。
- [x] `GET /api/courses/{courseId}/knowledge-graph` 复用同一读取授权。
- [x] `POST /api/assessment/grading-evaluations` 对 student 返回 `FORBIDDEN`。
- [x] Controller 不直接判断课程归属。
- [x] 不新增依赖、不改 schema、不改前端。
- [x] 聚焦测试通过。
- [x] 相邻回归通过或记录限制。
- [x] 后端全量 `mvn test` 通过。
- [x] Evidence / Acceptance 已创建。

## Allowed Files

- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/main/java/com/learningos/knowledge/repository/CourseRepository.java`
- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `docs/evidence/EVIDENCE-20260608-rbac-course-class-answer-matrix.md`
- `docs/acceptance/ACCEPT-20260608-rbac-course-class-answer-matrix.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`

## Test Commands

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test
mvn test
```

## Verification Results

| Command | Result |
|---|---|
| `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test` | PASS；19 tests，0 failures，0 errors |
| `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test` | PASS；71 tests，0 failures，0 errors |
| `mvn test` | PASS；302 tests，0 failures，0 errors，1 skipped |
