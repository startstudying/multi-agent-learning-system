# TASK - P3-4-R Assessment / GradingEvaluation roles-first RBAC

## 1. Traceability

- PLAN: `docs/plans/PLAN-20260609-p3-4-r-assessment-grading-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md`

## 2. Goal

让 Assessment read paths 与 GradingEvaluation HTTP path 使用 explicit Bearer role facts，阻断 `USER sub=admin/teacher_1` subject-name role-confusion，同时保留现有 API/DB/DTO 行为。

## 3. Files Allowed To Modify

- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-backend.md`
- `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-security.md`
- `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-test.md`
- `docs/product/PRD-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- Formal OAuth2/JWK/Spring Security config
- `POST /api/assessment/answers` submit behavior

## 5. Implementation Steps

1. 创建工作流文档与 Context Pack。
2. 增加 RED 测试：
   - Bearer admin spoofed header 可读 Assessment detail / 可运行 grading。
   - Bearer teacher no-prefix 可读 own-course Assessment list/detail / 可运行 grading。
   - Bearer `USER sub=admin` 不获得 admin 权限。
   - Bearer `USER sub=teacher_1` 不获得 teacher 权限。
3. 运行 focused RED：`mvn --% -Dtest=AssessmentControllerTest test`。
4. 修改 `AssessmentController` 传 explicit role facts。
5. 修改 `AssessmentService` roles-first overload 和 CourseAccess 调用。
6. 修改 `GradingEvaluationService` roles-first overload 和 CourseAccess 调用。
7. 运行 focused / adjacent / full verification。
8. 更新 Evidence / Acceptance / Retro / Changelog / Memory / TODO。

## 6. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,EvaluationSetControllerTest,EvaluationRunControllerTest,AnalyticsControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 7. Done Criteria

- [x] PRD/REQ/SPEC/PLAN/TASK/CONTEXT 存在。
- [x] RED 失败已观察并记录。
- [x] Assessment read paths 使用 roles-first overload。
- [x] GradingEvaluation HTTP path 使用 roles-first overload。
- [x] `USER sub=admin/teacher_1` role-confusion 被阻断。
- [x] focused/adjacent/full tests 已运行或限制已说明。
- [x] Evidence / Acceptance / Retro 已创建。
- [x] Changelog / Memory / TODO 已更新。

## 8. Status

Done.
