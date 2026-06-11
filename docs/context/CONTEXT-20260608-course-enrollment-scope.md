# CONTEXT-20260608 P3-4-D Course Enrollment Scope

## 1. Current TASK

`docs/tasks/TASK-20260608-course-enrollment-scope.md`

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260608-course-enrollment-scope.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/subagents/runs/RUN-20260608-backend-todolist-next-slice-integration.md`

## 3. Selected Skills

- feature-development-workflow
- object-scope-authorization
- java-security-review
- spring-ai-agent-backend
- assessment-feedback-agent
- test-driven-development
- verification-before-completion

## 4. Subagent Plan

已完成 L1 parallel analysis：

- P3-2 RAG parser expert report
- P3-3 model provider expert report
- P3-4 security expert report
- integration review report

本切片由 Main Codex 单线程实现，不并行改代码。

## 5. Files Allowed To Modify

同 `TASK-20260608-course-enrollment-scope.md` 的 Allowed Files。

## 6. Files Not Allowed To Modify

同 `TASK-20260608-course-enrollment-scope.md` 的 Files Not Allowed To Modify。

## 7. Test Commands

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,SchemaConvergenceMigrationTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceReviewControllerTest test
mvn test
```

## 8. Current Task Boundary

只实现 course enrollment scope。answer record 独立详情/list API、teacher answer summary、JWT/RBAC、RAG KB 与 course enrollment 联动不在本切片内。
