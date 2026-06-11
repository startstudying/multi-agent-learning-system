# EVIDENCE-20260610 P3-4 子任务：Service legacy subject-name authorization cleanup

## 1. Scope

本证据记录覆盖 M 级子任务：

```text
P3-4 子任务：Service legacy subject-name authorization cleanup
```

目标：移除 `KnowledgeCatalogService`、`AssessmentService`、`GradingEvaluationService` 中通过 `currentUserId == "admin"` / `teacher_*` 推断角色的 legacy service authorization overload/helper。

## 2. Changes Verified

### Production Code

- `KnowledgeCatalogService`
  - Removed legacy public overloads that accepted only `currentUserId` and inferred roles.
  - Removed private subject-name helpers:
    - `resolveCourseTeacherId(String, String)`
    - `requireCourseTeacherOrAdmin(String, Course)`
    - `requireCourseManageAccess(String, Course)`
    - `requireCourseReadAccess(String, Course)`
    - `scopedCourseMissing(String)`
    - `isAdmin(String)`
    - `isTeacherUser(String)`
- `AssessmentService`
  - Removed legacy list/detail overloads that inferred admin/teacher facts from `currentUserId`.
  - Removed private `isAdmin(String)` and `isTeacherUser(String)`.
- `GradingEvaluationService`
  - Removed `evaluate(String currentUserId, GradingEvaluationRequest request)`.
  - Removed private `isAdmin(String)` and `isTeacherUser(String)`.
  - Kept pure algorithm entries:
    - `evaluate(GradingEvaluationRequest request)`
    - `evaluate(List<Double>, List<Double>, double)`

### Test Code

- Added reflection guards to `CourseAccessServiceTest` for `KnowledgeCatalogService` legacy overload/helper removal.
- Added reflection guards to `AssessmentServiceTest`.
- Added reflection guards to `GradingEvaluationServiceTest`.

## 3. Static Checks

### Target helper removal

Command:

```powershell
rg -n 'isAdmin\(|isTeacherUser\(|teacher_' backend\src\main\java\com\learningos\knowledge\application\KnowledgeCatalogService.java backend\src\main\java\com\learningos\assessment\application\AssessmentService.java backend\src\main\java\com\learningos\assessment\application\GradingEvaluationService.java
```

Result:

- Exit code `1` for no matches in the target helper pattern, except later simplified string checks found only non-helper business strings such as validation messages and `request.teacherId()`.
- No target `isAdmin(...)`, `isTeacherUser(...)`, or `teacher_*` subject-name inference remains in the three target services.

### Compile guard

Command:

```powershell
cd D:\多元agent\backend
mvn --% -q -DskipTests compile
```

Result:

- Exit code `0`.
- Compile succeeded after deleting legacy overloads.

## 4. Focused Verification

Command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseAccessServiceTest,AssessmentServiceTest,GradingEvaluationServiceTest test
```

Result:

```text
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Coverage:

- `KnowledgeCatalogService` reflection guards.
- `AssessmentService` reflection guards and existing answer idempotency behavior.
- `GradingEvaluationService` reflection guards and pure metric algorithm behavior.
- Existing `CourseAccessService` role-confusion guards.

## 5. Adjacent Verification

Command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,CourseAccessServiceTest,AssessmentControllerTest,AssessmentServiceTest,GradingEvaluationServiceTest,AnalyticsControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,RagQueryServiceTest,LearningWorkflowControllerTest test
```

Result:

```text
Tests run: 197, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Coverage:

- Course/Knowledge HTTP roles-first behavior.
- Assessment answer/wrong-question list/detail roles-first behavior.
- Grading evaluation course-scope behavior.
- Adjacent analytics, resource, review, RAG, and learning workflow RBAC paths.

## 6. Full Backend Verification

Command:

```powershell
cd D:\多元agent\backend
mvn test
```

Result:

```text
Tests run: 536, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 7. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Only Application Service and tests were changed; Controllers still delegate to Service. |
| Frontend rules | PASS | No frontend file changed. |
| Agent / RAG rules | PASS | No Agent/RAG runtime change; this cleanup reduces future internal-call role-confusion risk. |
| Security | PASS | Target Service authorization entrypoints now require explicit role facts, not subject-name inference. |
| API / Database | PASS | No REST API, DTO, DB schema, migration, or dependency change. |

## 8. Limitations

- This task does not clean every possible legacy subject-name helper elsewhere in the backend.
- P3-4 parent remains open for broader class/course matrix, answer-record expansion, dev/test legacy fallback cleanup, and frontend production streaming client / sensitive SSE URL cleanup.
- No MySQL smoke was run because no schema or migration changed.

## 9. Verdict

PASS.
