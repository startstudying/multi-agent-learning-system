# RUN-20260609 P3-4 broader class/course permission penetration tests - Security

## Role

Security reviewer / broken-access-control reviewer.

## Scope

Read-only review of class/course permission penetration tests and the analytics authorization boundary.

## Verdict

CONDITIONAL before documentation closure; PASS for the named high-risk cases after Evidence/Acceptance is added.

## Covered Risks

- Bearer `USER sub=admin` and `USER sub=teacher_1` do not receive admin/teacher business semantics from subject strings.
- Bearer `TEACHER` identity is not overwritten by spoofed `X-User-Id`.
- Class analytics aggregates only active enrolled learners and does not infer membership from legacy learning path, wrong-question, or resource-task signals.
- Non-admin denied paths continue to avoid leaking protected object identifiers in response bodies.

## Evidence Reviewed

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- Existing surefire reports for `AnalyticsControllerTest` and `CourseKnowledgeControllerTest`.

## Remaining Risks

- P3-4 broader class/course authorization matrix is not fully complete.
- Formal OAuth2/JWK/Spring Security migration remains future work.
- Class analytics still has some in-memory filtering paths; future hardening should prefer repository-level scoped queries where practical.

## Recommendation

Keep these tests as the minimum regression gate when the formal Spring Security/OAuth2 layer is introduced.
