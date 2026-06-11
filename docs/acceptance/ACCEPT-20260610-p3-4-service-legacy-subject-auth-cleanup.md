# ACCEPT-20260610 P3-4 子任务：Service legacy subject-name authorization cleanup

## 1. Acceptance Verdict

Accepted.

## 2. Acceptance Criteria

| Criteria | Result | Evidence |
|---|---|---|
| `KnowledgeCatalogService` no longer exposes target legacy overloads | PASS | Reflection guards in `CourseAccessServiceTest`; focused tests passed. |
| `KnowledgeCatalogService` no longer keeps target subject-name helpers | PASS | Reflection guards in `CourseAccessServiceTest`; static `rg` check. |
| `AssessmentService` no longer exposes target legacy list/detail overloads | PASS | Reflection guards in `AssessmentServiceTest`; focused tests passed. |
| `AssessmentService` no longer keeps `isAdmin(String)` / `isTeacherUser(String)` | PASS | Reflection guards in `AssessmentServiceTest`; static `rg` check. |
| `GradingEvaluationService.evaluate(String, GradingEvaluationRequest)` removed | PASS | Reflection guard in `GradingEvaluationServiceTest`. |
| `GradingEvaluationService` pure algorithm entries remain | PASS | Existing metric tests passed. |
| Controllers continue to compile and call roles-first signatures | PASS | Compile guard, adjacent HTTP tests, and full backend tests passed. |
| No API/DTO/DB/dependency/frontend change | PASS | Diff scope and architecture drift check. |
| P3-4 parent remains open | PASS | TODO updated without marking parent done. |

## 3. Verification Summary

- Compile guard: `mvn --% -q -DskipTests compile` passed.
- Focused: `22 run, 0 failures, 0 errors, 0 skipped`.
- Adjacent: `197 run, 0 failures, 0 errors, 0 skipped`.
- Full backend: `536 run, 0 failures, 0 errors, 1 skipped`.

## 4. Risk Decision

Risk after implementation: Low for this child task.

Remaining risk belongs to P3-4 parent follow-ups:

- broader class/course matrix expansion
- answer-record expansion
- dev/test legacy fallback cleanup
- frontend production streaming client / sensitive SSE URL cleanup

## 5. Final Status

Done for this semantic child task.
