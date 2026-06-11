# PLAN - P3-4-T Orchestrator RESOURCE_GENERATION create roles-first RBAC

## 1. Traceability

- PRD: `docs/product/PRD-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-t-orchestrator-resource-create-rbac.md`

## 2. Task Classification

| Field | Value |
|---|---|
| Task type | Bug fix / security hardening / Agent workflow RBAC |
| Execution focus | Reproduce Orchestrator legacy role-confusion, smallest roles-first migration, regression tests |

## 3. Subagent Decision

| Field | Decision |
|---|---|
| Use Subagents | Yes |
| Reason | 用户要求专家 subagent；任务涉及 Orchestrator、ResourceGeneration、auth context、side effects、testing |
| Parallelism Level | L1 parallel analysis |
| Selected Subagents | Architect, Security Reviewer, Test Engineer |
| Implementation Mode | Single Codex implementation after integration review |

## 4. Phases

| Phase | Description | Status |
|---|---|---|
| 1 | 读取 memory/spec/current code，保存专家报告，形成 integration review | Done |
| 2 | 创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT | Done |
| 3 | 在 `OrchestratorWorkflowControllerTest` 新增 RED tests | Done |
| 4 | 运行 focused RED 并记录失败 | Done |
| 5 | 修改 Orchestrator controller/service roles-first create/retry | Done |
| 6 | 运行 focused / adjacent / full verification | Done |
| 7 | 创建 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO | Done |

## 5. File Change Plan

| File | Action |
|---|---|
| `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java` | 修改 |
| `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java` | 修改 |
| `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java` | 修改 |
| `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-*.md` | 新增 |
| P3-4-T PRD/REQ/SPEC/PLAN/TASK/CONTEXT/EVIDENCE/ACCEPT/RETRO | 新增 |
| `docs/changelog/CHANGELOG.md` | 更新 |
| `docs/memory/PROJECT_MEMORY.md` | 更新 |
| `docs/memory/BACKEND_MEMORY.md` | 更新 |
| `docs/memory/API_MEMORY.md` | 更新 |
| `docs/planning/backend-architecture-todolist.md` | 更新 |

## 6. Risk Assessment

| Risk | Impact | Mitigation |
|---|---|---|
| retry path 仍走 legacy create | failed workflow retry 可重新触发 bypass | retry overload 同步传 roles-first facts |
| forbidden 后留下业务副作用 | 权限失败仍造成资源、模型、成本污染 | 测试断言 ResourceGeneration/model/token/citation rows 为 0 |
| 误开放 admin 代创建 ResourceGeneration | 扩大权限范围 | SPEC 明确 owner-only；测试覆盖 Bearer admin other learner |
| 误标 P3-4 完成 | 计划状态失真 | TODO 只标 P3-4-T，保留 broader class/course 和 formal OAuth2 follow-up |

## 7. Rollback Strategy

代码回滚只涉及 Orchestrator controller/service/test。本切片不改 DB/API/依赖，回滚风险低。

## 8. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 提取角色事实，Service 编排 |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 保留 Orchestrator trace/failure evidence |
| Security | PASS | 无 secrets/dependency |
| API / Database | PASS | 无 contract/schema change |

## 9. Architecture Drift Post-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 提取 Bearer roles facts；Service 负责 Orchestrator 编排和授权事实传递 |
| Frontend rules | PASS | 未修改 frontend |
| Agent / RAG rules | PASS | 保留 Orchestrator trace/failure evidence，失败摘要脱敏 |
| Security | PASS | 未新增 secrets/dependency；权限仍由后端代码执行 |
| API / Database | PASS | 未修改 API path/DTO/schema |

## 10. Final Status

P3-4-T 已完成并验收。P3-4 broader class/course、formal OAuth2/JWK/Spring Security、broader penetration tests 仍保持未完成。
