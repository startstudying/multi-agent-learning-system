# PLAN - P3-4-V ResourceGeneration / Agent Trace detail roles-first RBAC

## 1. Traceability

- PRD: `docs/product/PRD-20260609-p3-4-v-resource-trace-detail-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-v-resource-trace-detail-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-v-resource-trace-detail-rbac.md`

## 2. Task Classification

| Field | Value |
|---|---|
| Task type | Bug fix / security hardening |
| Execution focus | Reproduce role-confusion, migrate smallest detail/search HTTP paths to roles-first, preserve owner-only/cancel semantics. |

## 3. Subagent Decision

| Field | Decision |
|---|---|
| Use Subagents | Yes |
| Reason | 任务涉及 Agent Trace、ResourceGeneration、RBAC 和测试矩阵；已按项目规则启用专家并收到报告。 |
| Parallelism Level | L1 parallel analysis |
| Selected Subagents | Architect, Security Reviewer, Test Engineer |
| Implementation Mode | Single Codex implementation after integration review |

## 4. Phases

| Phase | Description | Status |
|---|---|---|
| 1 | 读取 memory/spec/current code，收敛专家结论 | Done |
| 2 | 创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT 与 subagent reports | Done |
| 3 | 添加 RED tests 并运行 focused RED | Done |
| 4 | 实现 Controller roles-first 调用与 Service overload | Done |
| 5 | 运行 focused / adjacent / full verification | Done |
| 6 | 创建 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO | Done |

## 5. File Change Plan

| File | Action |
|---|---|
| `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java` | 修改 |
| `backend/src/test/java/com/learningos/agent/api/AgentTraceControllerTest.java` | 修改 |
| `backend/src/main/java/com/learningos/agent/api/ResourceGenerationController.java` | 修改 |
| `backend/src/main/java/com/learningos/agent/api/AgentTraceController.java` | 修改 |
| `backend/src/main/java/com/learningos/agent/api/AgentTraceGovernanceController.java` | 修改 |
| `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java` | 修改 |
| `backend/src/main/java/com/learningos/agent/application/AgentTraceGovernanceService.java` | 修改 |
| P3-4-V workflow/evidence/acceptance/retro docs | 新增 |
| `docs/changelog/CHANGELOG.md` | 更新 |
| `docs/memory/PROJECT_MEMORY.md` | 更新 |
| `docs/memory/BACKEND_MEMORY.md` | 更新 |
| `docs/memory/API_MEMORY.md` | 更新 |
| `docs/planning/backend-architecture-todolist.md` | 更新 |

## 6. Risk Assessment

| Risk | Impact | Mitigation |
|---|---|---|
| Legacy service overload 继续被 HTTP 调用 | role-confusion 未关闭 | Controller tests 覆盖 Bearer roles + spoofed header。 |
| learner-resources 被误开放给 admin | 学生资源泄露 | 保持 owner-only，仅让 explicit admin 影响 missing 语义。 |
| Trace search 误向 teacher/user 开放 | 治理数据泄露 | search 只接受 explicit `ADMIN`。 |
| cancel 语义被误改 | 用户任务控制回归 | 本切片不开放 admin cancel，adjacent tests 覆盖 existing cancel。 |

## 7. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 派生 auth facts，Service 执行授权。 |
| Frontend rules | PASS | 不改 frontend。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG/model runtime。 |
| Security | PASS | 目标是后端 roles-first 授权。 |
| API / Database | PASS | 无 API/DB 变更。 |

## 8. Final Status

Done。P3-4-V 已完成并验收，但 P3-4 和总 TODO 仍未完成。

## 9. Architecture Drift Post-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只提取 `UserContext` 与 role facts；Service 执行对象授权。 |
| Frontend rules | PASS | 未修改 frontend。 |
| Agent / RAG rules | PASS | 未改变 Agent execution、Trace 写入、RAG 或 model runtime。 |
| Security | PASS | HTTP 主路径不再依赖 `sub == "admin"` 推断 admin。 |
| API / Database | PASS | 未修改 API path/DTO/schema。 |
