# REQ - P3-4-T Orchestrator RESOURCE_GENERATION create roles-first RBAC

## 1. 功能需求

| ID | Requirement |
|---|---|
| FR-1 | `OrchestratorWorkflowController.create(...)` 必须读取 `CurrentUserService.currentUser()`，并从 `UserContext.roles()` 派生 admin/teacher facts。 |
| FR-2 | `OrchestratorWorkflowService.createWorkflow(...)` 必须提供 roles-first overload，并在 `RESOURCE_GENERATION` 分支传给 `ResourceGenerationService.createTaskInWorkflow(...)` roles-first overload。 |
| FR-3 | `OrchestratorWorkflowController.retry(...)` 和 `OrchestratorWorkflowService.retryWorkflow(...)` 必须同步使用 roles-first facts。 |
| FR-4 | Bearer `USER sub=admin` + existing course + no active enrollment 必须返回 `FORBIDDEN`。 |
| FR-5 | Bearer `ADMIN` / `TEACHER` 不得获得 ResourceGeneration 代创建其他 learner workflow 的能力。 |
| FR-6 | Bearer token 必须覆盖 spoofed `X-User-Id`。 |
| FR-7 | owner + active enrollment 的 course-bound `RESOURCE_GENERATION` workflow 必须继续成功。 |
| FR-8 | template goal 不存在对应 course 时必须保持兼容，不要求 enrollment。 |

## 2. 非功能需求

- 不新增依赖和 schema。
- 不暴露 course id、requestId、payload 原文到 forbidden response。
- 权限检查在后端代码中完成，不能依赖 Prompt。
- Orchestrator failed evidence 必须脱敏。

## 3. Edge Cases

| Case | Expected |
|---|---|
| owner mismatch | `FORBIDDEN` before workflow envelope / task / trace |
| payload invalid | `VALIDATION_ERROR` before workflow envelope / task / trace |
| course enrollment denied after workflow start | HTTP `FORBIDDEN`; allowed failed workflow evidence; no ResourceGeneration/model/token/citation side effects |
| retry failed workflow | 新 workflow 使用 retry caller 的 roles-first facts |

## 4. Traceability

- PRD: `docs/product/PRD-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- Parent follow-up: `docs/acceptance/ACCEPT-20260609-p3-4-s-learning-resource-create-rbac.md`

