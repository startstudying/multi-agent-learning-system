# TASK-20260608 P3-4-G Grading Evaluation Course Scope

Status: Done

## Done Criteria

- [x] `POST /api/assessment/grading-evaluations` 请求必须包含 `courseId`。
- [x] student 请求始终 `FORBIDDEN`，且无 `data`。
- [x] teacher 只能运行 own-course grading evaluation。
- [x] teacher foreign/missing course 返回 `FORBIDDEN`，且无 `data`。
- [x] admin 可运行任意 existing course，missing course 返回 `NOT_FOUND`。
- [x] `samples[].knowledgePointId` 非空时必须属于 `courseId`。
- [x] legacy score array 模式也必须绑定 `courseId`。
- [x] 不新增依赖、不新增 migration、不改 frontend。
- [x] Focused / adjacent / full backend 测试完成并写入 Evidence。
- [x] Acceptance / Changelog / Memory / TODO / Retro / Skill 更新。

## Allowed Files

- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `backend/src/main/java/com/learningos/assessment/dto/GradingEvaluationRequest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/application/GradingEvaluationServiceTest.java`
- `docs/product/PRD-20260608-grading-evaluation-course-scope.md`
- `docs/requirements/REQ-20260608-grading-evaluation-course-scope.md`
- `docs/specs/SPEC-20260608-grading-evaluation-course-scope.md`
- `docs/plans/PLAN-20260608-grading-evaluation-course-scope.md`
- `docs/tasks/TASK-20260608-grading-evaluation-course-scope.md`
- `docs/context/CONTEXT-20260608-grading-evaluation-course-scope.md`
- `docs/evidence/EVIDENCE-20260608-grading-evaluation-course-scope.md`
- `docs/acceptance/ACCEPT-20260608-grading-evaluation-course-scope.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/retrospectives/RETRO-20260608-grading-evaluation-course-scope.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/runs/RUN-20260608-grading-evaluation-course-scope-backend.md`
- `docs/subagents/runs/RUN-20260608-grading-evaluation-course-scope-security.md`
- `docs/subagents/runs/RUN-20260608-grading-evaluation-course-scope-integration.md`

## Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- RAG parser/vector/model provider files
- Assessment answer submit / wrong-question persistence flow outside required tests

## Test Commands

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest test
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
mvn test
```
