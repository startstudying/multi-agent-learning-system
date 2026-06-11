# RUN-20260609 P3-4 broader class/course permission penetration tests - Test

## Role

Test engineer.

## Scope

Read-only review of test design and verification evidence for the S Fast Lane penetration-test slice.

## Verdict

CONDITIONAL before documentation closure; PASS for current S slice test adequacy after Evidence/Acceptance is added.

## Coverage Summary

`CourseKnowledgeControllerTest` now covers:

- `USER sub=teacher_1` cannot create a course through subject-name teacher inference.
- `USER sub=teacher_1` cannot create a knowledge point in a course whose `teacherId` equals the token subject.
- Related forbidden responses avoid leaking protected course/chapter/knowledge point details.

`AnalyticsControllerTest` now covers:

- Admin-only analytics endpoints reject `USER sub=admin`.
- `USER sub=teacher_1` cannot read class summary for a subject-owned course.
- Bearer `TEACHER` class summary ignores spoofed `X-User-Id`.
- Dropped and never-enrolled learners with learning-path, wrong-question, and resource-task signals are excluded from class analytics.

## Surefire Evidence Reviewed

- `AnalyticsControllerTest`: `Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`
- `CourseKnowledgeControllerTest`: `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`

## Required Closure Items

- Create combined Evidence/Acceptance.
- Mark TASK acceptance criteria complete.
- State explicitly that the current tests do not complete P3-4 as a whole.
