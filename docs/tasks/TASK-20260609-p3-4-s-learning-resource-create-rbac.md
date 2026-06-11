# TASK - P3-4-S LearningPath / ResourceGeneration course-bound create roles-first RBAC

## 1. Traceability

- PLAN: `docs/plans/PLAN-20260609-p3-4-s-learning-resource-create-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-s-learning-resource-create-rbac.md`

## 2. Goal

让 LearningPath 与 ResourceGeneration course-bound create 主路径使用 explicit Bearer role facts，阻断 `USER sub=admin` subject-name role-confusion，同时保持 ResourceGeneration owner-only 和现有 API/DB/DTO 行为。

## 3. Files Allowed To Modify

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

## 4. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- formal OAuth2/JWK/Spring Security config
- Agent/RAG/model provider/review gate runtime
- ResourceGeneration detail/trace/cancel/review RBAC

## 5. Implementation Steps

1. 创建工作流文档与 Context Pack。
2. 增加 RED tests：
   - LearningPath Bearer admin spoofed header 可代建。
   - LearningPath `USER sub=admin` 不可提权且无 path/node/event。
   - ResourceGeneration Bearer admin 不能为其他 learner 创建且无副作用。
   - ResourceGeneration `USER sub=admin` 不可绕过 enrollment 且无副作用。
3. 运行 focused RED。
4. 修改 `LearningPathController` 传 explicit role facts。
5. 修改 `LearningWorkflowService` roles-first create overload。
6. 修改 `ResourceGenerationController` 传 explicit role facts。
7. 修改 `ResourceGenerationService` roles-first create overload，并保持 owner-only。
8. 修改 `CourseAccessService` roles-first enrollment overload。
9. 运行 focused / adjacent / full verification。
10. 更新 Evidence / Acceptance / Retro / Changelog / Memory / TODO。

## 6. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 7. Done Criteria

- [x] PRD/REQ/SPEC/PLAN/TASK/CONTEXT 存在。
- [x] RED 失败已观察并记录。
- [x] LearningPath create 使用 roles-first overload。
- [x] ResourceGeneration create 使用 roles-first overload。
- [x] `USER sub=admin` role-confusion 被阻断。
- [x] ResourceGeneration forbidden create 无 durable side effects。
- [x] focused/adjacent/full tests 已运行或限制已说明。
- [x] Evidence / Acceptance / Retro 已创建。
- [x] Changelog / Memory / TODO 已更新。

## 8. Status

Done for direct `POST /api/learning-paths` and `POST /api/resources/generation-tasks`.

Follow-up: Orchestrator `RESOURCE_GENERATION` workflow create still needs roles-first migration in a separate task.
