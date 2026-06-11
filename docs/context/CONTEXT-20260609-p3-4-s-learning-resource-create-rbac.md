# CONTEXT - P3-4-S LearningPath / ResourceGeneration course-bound create roles-first RBAC

## 1. Current Task Boundary

本切片只处理：

- `POST /api/learning-paths`
- `POST /api/resources/generation-tasks`
- course-bound create enrollment helper 的 roles-first overload

不处理 ResourceGeneration detail/trace/cancel/review/learner resource 读写权限。

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
- `docs/specs/SPEC-20260608-course-enrollment-scope.md`
- `docs/specs/SPEC-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-s-learning-resource-create-rbac.md`

## 3. Selected Skills

- `feature-development-workflow`
- `auth-context-boundary`
- `object-scope-authorization`
- `test-driven-development`
- `security-review`
- `test-generator`
- `architecture-drift-check`
- `verification-before-completion`

## 4. Subagent Plan

| Role | Report |
|---|---|
| Backend/Architect | `docs/subagents/runs/RUN-20260609-p3-4-s-learning-resource-backend.md` |
| Security & Quality | `docs/subagents/runs/RUN-20260609-p3-4-s-learning-resource-security.md` |
| Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-s-learning-resource-test.md` |
| Integration Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-s-learning-resource-integration-review.md` |

Implementation mode：Main Codex 单线程实现，避免多个 agent 修改同一文件。

## 5. Files Allowed To Modify

- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/java/com/learningos/agent/api/ResourceGenerationController.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
- `docs/subagents/runs/RUN-20260609-p3-4-s-learning-resource-*.md`
- `docs/product/PRD-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 6. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- formal OAuth2/JWK/Spring Security config
- Agent/RAG/model provider/review gate runtime
- ResourceGeneration detail/trace/cancel/review RBAC

## 7. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 8. Boundary Notes

- LearningPath 允许 explicit `ADMIN` role 代创建，延续 P3-4-D 管理员语义。
- ResourceGeneration 不允许 admin/teacher 代创建；该能力如需开放，必须另写 PRD/SPEC。
- 不声明 P3-4 或 `backend-architecture-todolist.md` 整体完成。

