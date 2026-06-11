# CONTEXT-20260608 P3-4-E Assessment Record RBAC Matrix

## 1. Current TASK

`docs/tasks/TASK-20260608-assessment-record-rbac.md`

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260608-rbac-course-class-answer-matrix.md`
- `docs/specs/SPEC-20260608-course-enrollment-scope.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-rbac-security.md`

## 3. Selected Skills

- feature-development-workflow
- object-scope-authorization
- assessment-feedback-agent
- java-security-review
- test-driven-development
- verification-before-completion

## 4. Subagent Plan

L1 parallel analysis:

- Security & Quality: assessment record RBAC matrix and leakage risk.
- Backend Expert: implementation landing points and teacher authorization feasibility.

Implementation remains single Codex in the main workspace.

## 5. Files Allowed To Modify

See `TASK-20260608-assessment-record-rbac.md`.

## 6. Files Not Allowed To Modify

See `TASK-20260608-assessment-record-rbac.md`.

## 7. Test Commands

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest test
mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test
mvn test
```

## 8. Current Task Boundary

Only implement answer / wrong-question detail RBAC. Do not implement list APIs, pagination, JWT/RBAC, DB migration, courseId denormalization, frontend pages, or grading evaluation course scope.
