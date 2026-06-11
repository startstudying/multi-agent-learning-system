# TASK-20260608 后端 P3 生产化集成总控

## Current Task：P3-4-C 权限矩阵安全前置

Status: Current slice completed

### Done Criteria

- [x] 专家 subagent 报告落盘。
- [x] P3-4-C PRD/REQ/SPEC/PLAN/TASK/CONTEXT 存在。
- [x] `GET /api/courses` 对 `admin` 返回全部课程，对 teacher 返回自己课程，对普通 student 不返回 foreign course。
- [x] `GET /api/courses/{courseId}` 对 foreign teacher/student 与 missing course 的非 admin 响应同类 `FORBIDDEN`，且无 `data`。
- [x] `GET /api/courses/{courseId}/knowledge-graph` 使用同一读取授权。
- [x] `POST /api/assessment/grading-evaluations` 禁止 student，允许 teacher/admin。
- [x] 权限检查在 Service 层完成。
- [x] 聚焦测试通过。
- [x] 相邻回归通过或 Evidence 记录限制。
- [x] 后端全量 `mvn test` 通过。
- [x] Evidence / Acceptance / Memory / Changelog / TODO 更新。

### Allowed Files

- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/main/java/com/learningos/knowledge/repository/CourseRepository.java`
- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `docs/subagents/runs/RUN-20260608-backend-p3-productionization-*.md`
- `docs/product/PRD-20260608-*.md`
- `docs/requirements/REQ-20260608-*.md`
- `docs/specs/SPEC-20260608-*.md`
- `docs/plans/PLAN-20260608-*.md`
- `docs/tasks/TASK-20260608-*.md`
- `docs/context/CONTEXT-20260608-*.md`
- `docs/evidence/EVIDENCE-20260608-rbac-course-class-answer-matrix.md`
- `docs/acceptance/ACCEPT-20260608-rbac-course-class-answer-matrix.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`

### Files Not Allowed

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- RAG parser / model gateway implementation files in this task

### Test Commands

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test
mvn test
```

### Verification Results

| Command | Result |
|---|---|
| `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test` | PASS；19 tests，0 failures，0 errors |
| `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test` | PASS；71 tests，0 failures，0 errors |
| `mvn test` | PASS；302 tests，0 failures，0 errors，1 skipped |
