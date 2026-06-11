# EVIDENCE - P3-4-S LearningPath / ResourceGeneration course-bound create roles-first RBAC

## 1. Scope

本证据文档覆盖：

- `POST /api/learning-paths`
- `POST /api/resources/generation-tasks`
- `CourseAccessService.requireLearnerEnrolledForExistingCourse(...)` roles-first overload

不覆盖：

- Orchestrator `RESOURCE_GENERATION` workflow create 调用链
- ResourceGeneration detail / trace / cancel / review / learner resource RBAC
- broader class/course matrix
- formal OAuth2/JWK/Spring Security migration

## 2. Code Evidence

| Area | Evidence |
|---|---|
| LearningPath controller role facts | `backend/src/main/java/com/learningos/learning/api/LearningPathController.java` uses `CurrentUserService.currentUser()` and derives `ADMIN` / `TEACHER` facts from `UserContext.roles()`. |
| LearningPath service roles-first overload | `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java` exposes `createPathForUser(currentUserId, currentUserAdmin, currentUserTeacher, request)`. |
| ResourceGeneration controller role facts | `backend/src/main/java/com/learningos/agent/api/ResourceGenerationController.java` uses `CurrentUserService.currentUser()` and derives `ADMIN` / `TEACHER` facts from `UserContext.roles()`. |
| ResourceGeneration owner-only direct create | `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java` direct roles-first overload keeps `ensureLearnerOwner(...)` and passes `allowAdminEnrollmentBypass=false`. |
| Enrollment helper | `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java` adds `requireLearnerEnrolledForExistingCourse(currentUserId, currentUserAdmin, learnerId, courseId)`. |

## 3. RED Evidence

Command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test
```

Observed pre-fix result:

```text
Tests run: 32, Failures: 3, Errors: 0, Skipped: 0
BUILD FAILURE
```

Expected failures:

- Bearer `ADMIN sub=ops_admin` LearningPath admin create was rejected.
- Bearer `USER sub=admin` LearningPath role-confusion was incorrectly accepted.
- Bearer `USER sub=admin` ResourceGeneration enrollment bypass was incorrectly accepted and produced side effects.

## 4. GREEN Evidence

Focused command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test
```

Result:

```text
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Adjacent command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
```

Result:

```text
Tests run: 91, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full backend command:

```powershell
cd D:\多元agent\backend
mvn test
```

Result:

```text
Tests run: 446, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. Side-Effect Evidence

`ResourceGenerationControllerTest` asserts forbidden create requests leave no durable side effects:

- `resourceGenerationTaskRepository.count() == 0`
- `learningResourceRepository.count() == 0`
- `resourceReviewRepository.count() == 0`
- `agentTaskRepository.count() == 0`
- `agentTraceRepository.count() == 0`
- `modelCallLogRepository.count() == 0`
- `tokenUsageLogRepository.count() == 0`

## 6. Integration Review Evidence

Integration review file:

- `docs/subagents/runs/RUN-20260609-p3-4-s-learning-resource-integration-review.md`

Verdict:

- CONDITIONAL PASS for direct `POST /api/learning-paths` and `POST /api/resources/generation-tasks`.
- Orchestrator `RESOURCE_GENERATION` workflow create remains a follow-up because it still calls a legacy `createTaskInWorkflow(...)` path.

## 7. Architecture Drift Evidence

| Check | Result |
|---|---|
| API path / DTO | No change |
| Database schema | No change |
| Dependencies | No change |
| Frontend | No change |
| Backend layering | Controller derives role facts; Service performs authorization |
| Agent/RAG runtime | No runtime behavior change in this slice |
| Secrets | No secrets or credentials added |

## 8. Remaining Risk

P3-4-S closes only direct create APIs. The next authorization slice should migrate Orchestrator `RESOURCE_GENERATION` workflow create to roles-first facts before claiming full ResourceGeneration create coverage.
