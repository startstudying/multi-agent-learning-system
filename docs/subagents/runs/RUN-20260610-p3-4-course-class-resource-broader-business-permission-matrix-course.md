# RUN-20260610-p3-4-course-class-resource-broader-business-permission-matrix-course

## Role

Course / Knowledge permission matrix expert.

## Scope

Target file:

- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`

The reused expert thread was constrained by a previous read-only role and could not write files directly. It provided the coverage analysis and verification recommendation; main Codex integrated the patch in the allowed file.

## Tests Added

- `courseListBearerTeacherIgnoresSpoofedAdminHeaderAndReturnsOnlyOwnedCourses`
- `courseListBearerStudentWithSpoofedTeacherHeaderReturnsOnlyActiveEnrollments`

## Behavior Verified

- Bearer `TEACHER` list-course requests ignore spoofed `X-User-Id: admin` and return only courses owned by the JWT subject.
- Bearer `STUDENT` list-course requests ignore spoofed `X-User-Id: teacher_*` and return only `ACTIVE` enrollment courses.
- `DROPPED` enrollment courses are not included in the student course list response.

## Production Defect

No production defect was identified during read-only analysis. The task should stay S unless the integrated tests fail.

## Verification

The expert ran the pre-patch class verification:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest test
```

Result:

```text
25 run, 0 failures, 0 errors, 0 skipped
```

Main Codex must re-run focused and adjacent verification after integration.

## Upgrade Needed

No upgrade expected unless integrated verification exposes a production defect.

