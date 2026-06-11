# CONTEXT-20260608 P3-4-G Grading Evaluation Course Scope

## 1. Current TASK

`docs/tasks/TASK-20260608-grading-evaluation-course-scope.md`

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260606-grading-quality-evaluation.md`
- `docs/specs/SPEC-20260608-rbac-course-class-answer-matrix.md`
- `docs/specs/SPEC-20260608-assessment-record-list-rbac.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-list-rbac-backend.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-list-rbac-security.md`
- `docs/subagents/runs/RUN-20260608-assessment-record-list-rbac-integration.md`
- `docs/subagents/runs/RUN-20260608-grading-evaluation-course-scope-backend.md`
- `docs/subagents/runs/RUN-20260608-grading-evaluation-course-scope-security.md`
- `docs/subagents/runs/RUN-20260608-grading-evaluation-course-scope-integration.md`

## 3. Selected Skills

- feature-development-workflow
- object-scope-authorization
- assessment-feedback-agent
- java-security-review / security-review
- multi-agent-coder
- test-driven-development
- verification-before-completion
- Confidence Check

## 4. Subagent Plan

L1 parallel analysis:

- Backend Expert: current implementation and minimal code boundary.
- Security & Quality: course scope matrix and anti-enumeration semantics.
- Integration Reviewer: final merged boundary and test checklist.

Implementation remains single Codex in the main workspace.

## 5. Files Allowed To Modify

See `TASK-20260608-grading-evaluation-course-scope.md`.

## 6. Files Not Allowed To Modify

See `TASK-20260608-grading-evaluation-course-scope.md`.

## 7. Test Commands

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest test
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
mvn test
```

## 8. Current Task Boundary

Only implement course-scoped grading evaluation. Do not implement JWT/RBAC, evaluation set runner, frontend changes, DB migration, new dependencies, or broader course/class matrix.
