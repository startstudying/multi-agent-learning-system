# PLAN - P3-4-X LearningPath Detail Roles-First RBAC

## 1. Traceability

- PRD: `docs/product/PRD-20260609-p3-4-x-learning-path-detail-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-x-learning-path-detail-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-x-learning-path-detail-rbac.md`

## 2. Task Classification

| Field | Value |
|---|---|
| Task type | Bug fix / security hardening |
| Execution focus | 复现并修复 LearningPath detail GET 的 subject-name admin role-confusion，保持最小改动。 |

## 3. Subagent Decision

| Field | Decision |
|---|---|
| Use Subagents | Yes |
| Reason | 用户要求专家 subagent 并行开发；本切片涉及 RBAC、IDOR、防枚举和测试矩阵。 |
| Parallelism Level | L1 parallel analysis |
| Selected Subagents | Architect, Security Reviewer, Test Engineer |
| Implementation Mode | Main Codex 单线实现，避免多个 agent 修改同一测试/服务文件。 |

## 4. Integrated Expert Result

- Architect：根因是 `LearningPathController.get(...)` 只传 `currentUserId`，service 用 `"admin"` subject-name 判断 admin。
- Security Reviewer：当前漏洞等级 HIGH；Bearer `USER sub=admin` 可获得 admin detail 语义，并破坏 missing/foreign 防枚举。
- Test Engineer：建议在 `LearningWorkflowControllerTest` 新增 HTTP 集成 RED 矩阵，覆盖 explicit admin、subject-admin role-confusion、owner、non-owner foreign/missing。

## 5. Phases

| Phase | Description | Status |
|---|---|---|
| 1 | 审计 TODO/memory/current code | Done |
| 2 | 启动并合并专家报告 | Done |
| 3 | 创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT | Done |
| 4 | 添加 RED tests 并观察失败 | Done |
| 5 | 实现 roles-first GET detail | Done |
| 6 | 运行 focused / adjacent / full verification | Done |
| 7 | Evidence / Acceptance / Retro / Changelog / Memory / TODO 更新 | Done |

## 6. File Change Plan

| File | Action |
|---|---|
| `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java` | 修改 |
| `backend/src/main/java/com/learningos/learning/api/LearningPathController.java` | 修改 |
| `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java` | 修改 |
| `docs/subagents/runs/RUN-20260609-p3-4-x-learning-path-detail-rbac-*.md` | 新增/更新 |
| P3-4-X workflow/evidence/acceptance/retro docs | 新增 |
| `docs/changelog/CHANGELOG.md` | 更新 |
| `docs/memory/PROJECT_MEMORY.md` | 更新 |
| `docs/memory/BACKEND_MEMORY.md` | 更新 |
| `docs/memory/API_MEMORY.md` | 更新 |
| `docs/planning/backend-architecture-todolist.md` | 更新 |

## 7. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| 改动扩大到 create path | 回归 P3-4-S 已验收行为 | 本切片只改 GET detail。 |
| Bearer admin missing/foreign 语义误收敛 | 运维 `NOT_FOUND` 语义丢失 | 明确测试 admin missing `NOT_FOUND`。 |
| non-admin missing/foreign 防枚举回退 | IDOR/object oracle | 明确测试 forbidden body 不含目标 id。 |

## 8. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只传当前用户事实；Service 仍做对象授权。 |
| Frontend rules | PASS | 不改 frontend。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG/model runtime。 |
| Security | PASS | 修复 subject-name role inference。 |
| API / Database | PASS | 无 path/DTO/schema/dependency 变更。 |

## 9. Architecture Drift Post-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只读取 `UserContext` 并传 explicit role fact；Service 仍负责对象授权与防枚举语义。 |
| Frontend rules | PASS | 未修改 frontend，未引入前端 LLM/API key 风险。 |
| Agent / RAG rules | PASS | 未修改 Agent、RAG、model provider、trace runtime。 |
| Security | PASS | `GET /api/learning-paths/{pathId}` 不再从 subject name 推断 admin；`USER sub=admin` role-confusion 被拒绝。 |
| API / Database | PASS | 无 REST path、DTO、schema、dependency 变更。 |

## 10. Final Status

Done。P3-4-X 已完成；P3-4 总项仍未全部完成，broader class/course、formal OAuth2/JWK/Spring Security、broader permission penetration tests 仍待后续扩展。
