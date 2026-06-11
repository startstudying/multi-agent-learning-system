# RUN - P3-4-T Orchestrator RESOURCE_GENERATION Integration Review

## 决策

采用最小 roles-first 迁移方案，不做 API/DTO/DB/依赖变更，不改 ResourceGeneration direct create 语义。

## 专家输出整合

| 议题 | 决策 |
|---|---|
| Controller 是否传 roles | 采纳：必须从 `UserContext.roles()` 派生 `ADMIN` / `TEACHER` facts |
| Service 是否新增 overload | 采纳：新增 `createWorkflow(ownerUserId, admin, teacher, request)` 与 `retryWorkflow(ownerUserId, admin, teacher, workflowId)` |
| ResourceGeneration 是否允许 admin 代创建 | 不允许，保持 owner-only |
| Forbidden enrollment failure 是否可留下 workflow evidence | 可以，沿用当前 Orchestrator runtime failure evidence；但不得留下 ResourceGeneration/model/token/citation 业务副作用 |
| 是否硬化/删除 legacy `ResourceGenerationService.createTaskInWorkflow` | 本切片不做；迁移 Orchestrator 后该 legacy path 无当前生产调用，后续在 broader RBAC 清理中处理 |
| 是否做 retry path | 采纳：同步传 roles-first facts，避免 retry 回到 legacy create |

## Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只提取 auth facts；Service 执行业务编排和授权传递 |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | Orchestrator 仍写 trace；权限失败只保留脱敏 failed evidence |
| Security | PASS | 不新增 dependency/secrets；权限由后端代码执行 |
| API / Database | PASS | 不改 API path/DTO/schema |

## 实施边界

只允许修改 Orchestrator controller/service/test 以及本切片 workflow 文档、记忆、证据和 TODO。P3-4 broader class/course、formal OAuth2/JWK/Spring Security、ResourceGeneration detail/trace/cancel/review RBAC 继续保持未完成。

