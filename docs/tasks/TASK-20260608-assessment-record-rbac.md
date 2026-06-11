# TASK-20260608 P3-4-E Assessment Record RBAC Matrix

Status: Done

## Done Criteria

- [x] `GET /api/assessment/answers/{answerId}` 已新增。
- [x] `GET /api/assessment/wrong-questions/{wrongQuestionId}` 已新增。
- [x] student 只能读取自己的 answer / wrong question。
- [x] teacher 只能读取自己课程 active enrollment learner 的课程相关记录。
- [x] admin 可读取任意已存在记录，missing 返回 `NOT_FOUND`。
- [x] 非 admin missing/foreign 返回同形 `FORBIDDEN` 且无 `data`。
- [x] 响应不包含 `requestId`、`requestHash`、`responseJson`、`payloadJson`。
- [x] 不新增依赖、不新增 migration、不改 frontend。
- [x] Focused / adjacent / full backend 测试完成并写入 Evidence。
- [x] Acceptance / Changelog / Memory / TODO / Retro 更新。

## Verification Result

- Focused：`mvn --% -Dtest=AssessmentControllerTest test`，13 tests，0 failures，0 errors，0 skipped。
- Adjacent：`mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test`，53 tests，0 failures，0 errors，0 skipped。
- Full backend：`mvn test`，316 tests，0 failures，0 errors，1 skipped。

## Allowed Files

- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/domain/AnswerRecord.java`
- `backend/src/main/java/com/learningos/assessment/domain/GradingResult.java`
- `backend/src/main/java/com/learningos/assessment/domain/WrongQuestion.java`
- `backend/src/main/java/com/learningos/assessment/dto/AssessmentRecordDetailResponse.java`
- `backend/src/main/java/com/learningos/assessment/dto/WrongQuestionDetailResponse.java`
- `backend/src/main/java/com/learningos/assessment/repository/GradingResultRepository.java`
- `backend/src/main/java/com/learningos/assessment/repository/WrongQuestionRepository.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/application/AssessmentServiceTest.java`
- `docs/product/PRD-20260608-assessment-record-rbac.md`
- `docs/requirements/REQ-20260608-assessment-record-rbac.md`
- `docs/specs/SPEC-20260608-assessment-record-rbac.md`
- `docs/plans/PLAN-20260608-assessment-record-rbac.md`
- `docs/tasks/TASK-20260608-assessment-record-rbac.md`
- `docs/context/CONTEXT-20260608-assessment-record-rbac.md`
- `docs/evidence/EVIDENCE-20260608-assessment-record-rbac.md`
- `docs/acceptance/ACCEPT-20260608-assessment-record-rbac.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/retrospectives/RETRO-20260608-assessment-record-rbac.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-rbac-security.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-rbac-backend.md`

## Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- model provider / RAG parser / vector adapter files

## Test Commands

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest test
mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test
mvn test
```
