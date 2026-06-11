# CONTEXT-20260608 P3-4-F Assessment Record List RBAC / Pagination

## 1. Current TASK

`docs/tasks/TASK-20260608-assessment-record-list-rbac.md`

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260608-assessment-record-rbac.md`
- `docs/specs/SPEC-20260608-course-enrollment-scope.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-rbac-security.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-list-rbac-backend.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-list-rbac-security.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-list-rbac-integration.md`

## 3. Selected Skills

- feature-development-workflow
- object-scope-authorization
- assessment-feedback-agent
- java-security-review
- test-driven-development
- verification-before-completion

## 4. Subagent Plan

L1 parallel analysis:

- Backend / Spec Architect: scoped list query and API contract.
- Security & Quality: IDOR/list enumeration and sensitive field review.
- Integration Reviewer: documentation and verification plan.

Implementation remains single Codex in the main workspace.

## 5. Files Allowed To Modify

See `TASK-20260608-assessment-record-list-rbac.md`.

## 6. Files Not Allowed To Modify

See `TASK-20260608-assessment-record-list-rbac.md`.

## 7. Test Commands

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest test
mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test
mvn test
```

## 8. Current Task Boundary

Only implement paginated answer / wrong-question list RBAC. Do not implement frontend pages, JWT/RBAC, DB migration, answer export, grading evaluation course scope, or assessment `courseId` denormalization.
