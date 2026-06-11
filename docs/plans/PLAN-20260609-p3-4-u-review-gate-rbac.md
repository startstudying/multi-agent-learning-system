# PLAN - P3-4-U Review Gate ResourceReview roles-first RBAC

## 1. Traceability

- PRD: `docs/product/PRD-20260609-p3-4-u-review-gate-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-u-review-gate-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-u-review-gate-rbac.md`

## 2. Task Classification

| Field | Value |
|---|---|
| Task type | Bug fix / security hardening |
| Execution focus | Reproduce Review Gate role-confusion, migrate smallest HTTP path to roles-first, add regression tests. |

## 3. Subagent Decision

| Field | Decision |
|---|---|
| Use Subagents | Yes |
| Reason | 用户要求专家 subagent；任务涉及 security/RBAC/testing/architecture。 |
| Parallelism Level | L1 parallel analysis |
| Selected Subagents | Architect, Security Reviewer, Test Engineer |
| Implementation Mode | Single Codex implementation after integration review |

## 4. Phases

| Phase | Description | Status |
|---|---|---|
| 1 | 读取 memory/spec/current code，保存专家报告，形成 integration review | Done |
| 2 | 创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT | Done |
| 3 | 在 `ResourceReviewControllerTest` 新增 RED tests | Done |
| 4 | 运行 focused RED 并记录失败 | Done |
| 5 | 修改 Review Gate controller/service roles-first overload | Done |
| 6 | 运行 focused / adjacent / full verification | Done |
| 7 | 创建 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO | Done |

## 5. File Change Plan

| File | Action |
|---|---|
| `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java` | 修改 |
| `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java` | 修改 |
| `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java` | 修改 |
| `docs/subagents/runs/RUN-20260609-p3-4-u-review-gate-rbac-*.md` | 新增 |
| P3-4-U PRD/REQ/SPEC/PLAN/TASK/CONTEXT/EVIDENCE/ACCEPT/RETRO | 新增 |
| `docs/changelog/CHANGELOG.md` | 更新 |
| `docs/memory/PROJECT_MEMORY.md` | 更新 |
| `docs/memory/BACKEND_MEMORY.md` | 更新 |
| `docs/memory/API_MEMORY.md` | 更新 |
| `docs/planning/backend-architecture-todolist.md` | 更新 |

## 6. Risk Assessment

| Risk | Impact | Mitigation |
|---|---|---|
| HTTP path 继续调用 legacy service | Bearer subject-name role-confusion 未关闭 | Controller tests 覆盖 `USER sub=admin/teacher_1`。 |
| Teacher no-prefix 被误拒 | 合法教师无法审核 | Bearer `TEACHER sub=instructor_1` own-course 测试。 |
| missing/foreign oracle 回归 | IDOR 探测 | Bearer teacher missing/foreign safe forbidden 测试。 |
| 误改审核发布状态机 | 资源发布回归 | 保持 decision 核心逻辑不变，运行 ReviewGovernance adjacent tests。 |

## 7. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只派生角色事实，Service 授权和状态流转。 |
| Frontend rules | PASS | 不改 frontend。 |
| Agent / RAG rules | PASS | 保留 Review Gate 和 Agent task 发布语义。 |
| Security | PASS | 无 secrets/dependency；权限由后端代码执行。 |
| API / Database | PASS | 无 API/DB 变更。 |

## 8. Architecture Drift Post-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 提取 `UserContext` 和 role facts；Service 执行对象授权和审核状态流转。 |
| Frontend rules | PASS | 未修改 frontend。 |
| Agent / RAG rules | PASS | 未改变 Agent trace、Review Gate 发布语义或 RAG。 |
| Security | PASS | 未新增 secrets/dependency；Review Gate HTTP 主路径不再依赖 subject-name role inference。 |
| API / Database | PASS | 未修改 API path/DTO/schema。 |

## 9. Final Status

P3-4-U 已完成并验收。P3-4 broader class/course、formal OAuth2/JWK/Spring Security、ResourceGeneration/Agent Trace detail roles-first、CourseAccessService legacy cleanup 仍未完成。
