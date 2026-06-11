# TASK-20260608 P3-4-F Assessment Record List RBAC / Pagination

Status: Done

## Done Criteria

- [x] `GET /api/assessment/answers` 已新增。
- [x] `GET /api/assessment/wrong-questions` 已新增。
- [x] student 只能 list 自己的 answer / wrong question。
- [x] teacher list 必须绑定 own course，并只返回 active enrollment learner 的课程相关记录。
- [x] admin 可全局分页 list，并可按 learner/course 过滤。
- [x] 非法 page/size 返回 `VALIDATION_ERROR`。
- [x] 列表响应不包含原始 `answer`、`requestId`、`requestHash`、`responseJson`、`payloadJson`。
- [x] 不新增依赖、不新增 migration、不改 frontend。
- [x] Focused / adjacent / full backend 测试完成并写入 Evidence。
- [x] Acceptance / Changelog / Memory / TODO / Retro 更新。

## Allowed Files

- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/domain/AnswerRecord.java`
- `backend/src/main/java/com/learningos/assessment/domain/WrongQuestion.java`
- `backend/src/main/java/com/learningos/assessment/dto/AssessmentPageResponse.java`
- `backend/src/main/java/com/learningos/assessment/dto/AssessmentRecordSummaryResponse.java`
- `backend/src/main/java/com/learningos/assessment/dto/WrongQuestionSummaryResponse.java`
- `backend/src/main/java/com/learningos/assessment/repository/AnswerRecordRepository.java`
- `backend/src/main/java/com/learningos/assessment/repository/WrongQuestionRepository.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/application/AssessmentServiceTest.java`
- `docs/product/PRD-20260608-assessment-record-list-rbac.md`
- `docs/requirements/REQ-20260608-assessment-record-list-rbac.md`
- `docs/specs/SPEC-20260608-assessment-record-list-rbac.md`
- `docs/plans/PLAN-20260608-assessment-record-list-rbac.md`
- `docs/tasks/TASK-20260608-assessment-record-list-rbac.md`
- `docs/context/CONTEXT-20260608-assessment-record-list-rbac.md`
- `docs/evidence/EVIDENCE-20260608-assessment-record-list-rbac.md`
- `docs/acceptance/ACCEPT-20260608-assessment-record-list-rbac.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/retrospectives/RETRO-20260608-assessment-record-list-rbac.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-list-rbac-backend.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-list-rbac-security.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-list-rbac-integration.md`

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
