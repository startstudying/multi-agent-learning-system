# RUN-20260610 P3-4 course/class/resource broader business permission matrix - resource

## Role

Resource permission matrix subagent.

## Scope

Only updated:

- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`

No production code, API contract, database schema, dependency, frontend, or shared documentation was changed.

## Tests Added

- `courseBoundResourceGenerationCreateRejectsBearerOwnerWithDroppedEnrollmentWithoutSideEffects`
- `courseBoundResourceGenerationCreateRejectsBearerOwnerWithNoEnrollmentWithoutSideEffects`

Both tests verify that a Bearer owner cannot create a course-bound resource generation task unless the target learner has `ACTIVE` enrollment for the course.

## Behavior Verified

- `DROPPED` enrollment returns `FORBIDDEN`.
- Missing enrollment returns `FORBIDDEN`.
- Spoofed `X-User-Id: admin` does not bypass Bearer owner enrollment checks.
- Rejected requests do not create `ResourceGenerationTask`, `LearningResource`, `ResourceReview`, `AgentTask`, `AgentTrace`, `ModelCallLog`, `TokenUsageLog`, or `SourceCitation` rows.

## Production Defect

No production defect was found. The new tests passed without production code changes.

## Verification

Command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest test
```

Result:

```text
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Upgrade Needed

No. This remains a test-only slice for the resource-generation part of the broader business permission matrix.
