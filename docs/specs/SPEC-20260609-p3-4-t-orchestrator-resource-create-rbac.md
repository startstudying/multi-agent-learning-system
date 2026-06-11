# SPEC - P3-4-T Orchestrator RESOURCE_GENERATION create roles-first RBAC

## 1. Traceability

- PRD: `docs/product/PRD-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- Subagent reports:
  - `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-backend.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-security.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-test.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-integration-review.md`

## 2. Current State

`OrchestratorWorkflowController.create(...)` 当前只传 `currentUserService.currentUserId()`。

`OrchestratorWorkflowService.createWorkflow(ownerUserId, request)` 的 `RESOURCE_GENERATION` 分支调用：

```java
resourceGenerationService.createTaskInWorkflow(ownerUserId, resourceGenerationRequest, context);
```

该 legacy overload 会通过 `isAdmin(userId)` 推断 admin，造成 Bearer `USER sub=admin` role-confusion 风险。

## 3. API Contract

不修改 API path、请求 DTO、响应 DTO。

| API | Contract Change | Authorization Change |
|---|---|---|
| `POST /api/orchestrator/workflows` | 无 | `RESOURCE_GENERATION` create 使用 roles-first facts |
| `POST /api/orchestrator/workflows/{workflowId}/retry` | 无 | retry 创建新 workflow 时使用 roles-first facts |

## 4. Authorization Semantics

| Scenario | Expected |
|---|---|
| Bearer `USER sub=admin` + `learnerId=admin` + existing course + no enrollment | `FORBIDDEN`; no ResourceGeneration/model/token/citation side effects |
| Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id=alice` + `learnerId=alice` | `FORBIDDEN` before workflow task; admin 不可代创建 ResourceGeneration |
| Bearer `STUDENT sub=alice` + spoofed `X-User-Id=admin` + active enrollment | `OK`; owner/task/resource learner 均为 `alice` |
| template goal not existing course | owner 可继续创建 |
| retry failed `RESOURCE_GENERATION` workflow | 新 workflow 使用 retry caller roles-first facts |

## 5. Service Contract

新增 roles-first overload：

```java
public OrchestratorWorkflowResponse createWorkflow(
        String ownerUserId,
        boolean currentUserAdmin,
        boolean currentUserTeacher,
        CreateWorkflowRequest request
)
```

旧 `createWorkflow(ownerUserId, request)` 保留兼容。

`RESOURCE_GENERATION` 分支必须调用：

```java
resourceGenerationService.createTaskInWorkflow(
        ownerUserId,
        currentUserAdmin,
        currentUserTeacher,
        resourceGenerationRequest,
        context
);
```

新增 roles-first retry overload：

```java
public OrchestratorWorkflowResponse retryWorkflow(
        String ownerUserId,
        boolean currentUserAdmin,
        boolean currentUserTeacher,
        String workflowId
)
```

旧 retry signature 保留兼容，但 HTTP controller 不再调用旧签名。

## 6. Side Effects Policy

| Failure Layer | Allowed Persistence | Forbidden Persistence |
|---|---|---|
| owner mismatch / invalid payload | none | `agent_task`, `agent_trace`, ResourceGeneration rows |
| course enrollment denied after workflow start | `FAILED agent_task`, `workflow_start`, `step_runtime_failure` | `resource_generation_task`, `learning_resource`, `resource_review`, `source_citation`, `model_call_log`, `token_usage_log` |

## 7. Persistence / Dependency / Frontend

- DB migration：无。
- 新依赖：无。
- Frontend：无。
- API DTO：无。

## 8. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 提取 auth facts；Service 编排业务和权限传递 |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | Orchestrator 仍写 trace；失败 evidence 脱敏 |
| Security | PASS | 不新增 secrets/dependencies |
| API / Database | PASS | 无 API/DB contract change |

## 9. Testing Strategy

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest test
mvn --% -Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,LearningWorkflowControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

