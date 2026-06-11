# TASK - P3-4-T Orchestrator RESOURCE_GENERATION create roles-first RBAC

## 1. Traceability

- PLAN: `docs/plans/PLAN-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-t-orchestrator-resource-create-rbac.md`

## 2. Goal

关闭 Orchestrator `RESOURCE_GENERATION` create/retry 调用链中的 legacy subject-name role-confusion，确保 Bearer roles-first facts 传入 ResourceGeneration workflow create。

## 3. Files Allowed To Modify

- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-*.md`
- `docs/product/PRD-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- formal OAuth2/JWK/Spring Security config
- ResourceGeneration direct create behavior
- ResourceGeneration detail/trace/cancel/review RBAC
- RAG/Assessment behavior except regression tests that run unchanged

## 5. Implementation Steps

1. 新增 Orchestrator Bearer JWT helper、course/enrollment fixture、side-effect helper。
2. 写 RED tests：
   - `USER sub=admin` course-bound no enrollment 被拒绝且无 ResourceGeneration/model/token/citation副作用。
   - Bearer admin 不能为其他 learner 创建 ResourceGeneration workflow，且 workflow envelope 不创建。
   - Bearer student owner + active enrollment + spoofed header 仍成功。
3. 运行 focused RED。
4. 修改 `OrchestratorWorkflowController` create/retry 读取 `UserContext` 并传 roles-first facts。
5. 修改 `OrchestratorWorkflowService` create/retry roles-first overload，`RESOURCE_GENERATION` 调用 roles-first ResourceGeneration workflow API。
6. 运行 focused、adjacent、full verification。
7. 更新 Evidence / Acceptance / Retro / Changelog / Memory / TODO。

## 6. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest test
mvn --% -Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,LearningWorkflowControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 7. Done Criteria

- [x] PRD/REQ/SPEC/PLAN/TASK/CONTEXT 存在。
- [x] RED 失败已观察并记录。
- [x] Orchestrator create 使用 roles-first overload。
- [x] Orchestrator retry 使用 roles-first overload。
- [x] `USER sub=admin` role-confusion 被阻断。
- [x] ResourceGeneration forbidden workflow create 无业务/model/token/citation副作用。
- [x] focused/adjacent/full tests 已运行或限制已说明。
- [x] Evidence / Acceptance / Retro 已创建。
- [x] Changelog / Memory / TODO 已更新。

## 8. Status

Done。
