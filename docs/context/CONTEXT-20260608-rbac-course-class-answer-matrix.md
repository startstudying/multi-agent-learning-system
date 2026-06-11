# CONTEXT-20260608 P3-4-C 权限矩阵安全前置

## 1. Current TASK

`TASK-20260608-rbac-course-class-answer-matrix.md`

## 2. Related Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260608-rbac-course-class-answer-matrix.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/subagents/runs/RUN-20260608-backend-p3-productionization-security-quality.md`
- `docs/subagents/runs/RUN-20260608-backend-p3-productionization-integration-review.md`

## 3. Selected Skills

- feature-development-workflow
- object-scope-authorization
- spring-boot-architecture
- api-contract-design
- security-review
- test-driven-development
- verification-before-completion

## 4. Files Allowed To Modify

- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/main/java/com/learningos/knowledge/repository/CourseRepository.java`
- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `docs/evidence/EVIDENCE-20260608-rbac-course-class-answer-matrix.md`
- `docs/acceptance/ACCEPT-20260608-rbac-course-class-answer-matrix.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/retrospectives/RETRO-20260608-rbac-course-class-answer-matrix.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/SKILL_REGISTRY.md`

## 5. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- RAG parser/model gateway/vector implementation files

## 6. Test Commands

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test
```

## 7. Boundary

只收口 course read/graph 和 grading evaluation 权限；不实现完整 JWT、class/enrollment、answer detail API 或 schema 变更。
