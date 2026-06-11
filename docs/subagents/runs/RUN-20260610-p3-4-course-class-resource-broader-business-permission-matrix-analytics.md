# RUN-20260610-p3-4-course-class-resource-broader-business-permission-matrix-analytics

## Role

Analytics authorization expert.

## Scope

Target file:

- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`

The reused expert thread was constrained by a previous read-only role and could not write files directly. It provided the authorization-path analysis; main Codex integrated the patch in the allowed file.

## Tests Added

- `bearerTeacherStudentSummaryRejectsDroppedLearnerInOwnCourseWithoutLeakingScope`

## Behavior Verified

- Bearer `TEACHER` can read a course-scoped student summary only when the target learner is actively enrolled in the teacher-owned course.
- A `DROPPED` learner in the teacher-owned course receives safe `FORBIDDEN`.
- The forbidden response does not leak course id, knowledge point id, learning path id, wrong-cause text, or resource task id.

## Production Defect

Read-only analysis found the expected production path in `AnalyticsService.requireStudentSummaryCourseAccess(...)`: teacher access checks `listActiveLearnerIds(courseId).contains(learnerId)`. No production defect was identified before integration.

## Verification

The expert could not run a meaningful post-patch verification because its role was read-only and it could not add the test. Main Codex must run:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest test
```

## Upgrade Needed

No upgrade expected unless integrated verification exposes a production defect.

