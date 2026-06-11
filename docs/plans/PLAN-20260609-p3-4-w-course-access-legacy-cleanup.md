# PLAN - P3-4-W CourseAccessService legacy overload cleanup

## 1. Traceability

- PRD: `docs/product/PRD-20260609-p3-4-w-course-access-legacy-cleanup.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-w-course-access-legacy-cleanup.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-w-course-access-legacy-cleanup.md`

## 2. Task Classification

| Field | Value |
|---|---|
| Task type | Refactor / security hardening |
| Execution focus | 删除已迁移完成后的 legacy public API 面，用测试和编译守卫防止回归。 |

## 3. Subagent Decision

| Field | Decision |
|---|---|
| Use Subagents | Yes |
| Reason | 用户要求专家 subagent 并行开发；本切片涉及 RBAC、安全边界和测试矩阵。 |
| Parallelism Level | L1 parallel analysis |
| Selected Subagents | Architect, Security Reviewer, Test Engineer |
| Implementation Mode | Main Codex 单线实现，避免多 agent 修改同一文件。 |

## 4. Integrated Expert Result

- Architect：当前源码未发现 `CourseAccessService` legacy overload 的真实调用残留；建议直接删除 4 个 legacy overload 和 3 个 subject-name helper。
- Security Reviewer：当前 HTTP 主路径基本已 roles-first；剩余风险是 legacy public API 被未来误用。
- Test Engineer：建议用服务层/编译守卫和现有 controller adjacent 矩阵验证，不扩大到全仓 RBAC 清理。

## 5. Phases

| Phase | Description | Status |
|---|---|---|
| 1 | 审计 memory/spec/TODO/current code 与调用残留 | Done |
| 2 | 启动并合并专家报告 | Done |
| 3 | 创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT | Done |
| 4 | 添加 RED tests 并观察失败 | Done |
| 5 | 删除 legacy overload/helper | Done |
| 6 | 运行 focused / adjacent / full verification | Done |
| 7 | Evidence / Acceptance / Retro / Changelog / Memory / TODO 更新 | Done |

## 6. File Change Plan

| File | Action |
|---|---|
| `backend/src/test/java/com/learningos/knowledge/application/CourseAccessServiceTest.java` | 新增 |
| `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java` | 修改 |
| `docs/subagents/runs/RUN-20260609-p3-4-w-course-access-legacy-cleanup-*.md` | 新增/补齐 |
| P3-4-W workflow/evidence/acceptance/retro docs | 新增 |
| `docs/changelog/CHANGELOG.md` | 更新 |
| `docs/memory/PROJECT_MEMORY.md` | 更新 |
| `docs/memory/BACKEND_MEMORY.md` | 更新 |
| `docs/memory/API_MEMORY.md` | 更新 |
| `docs/planning/backend-architecture-todolist.md` | 更新 |

## 7. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| 删除旧签名导致隐藏调用编译失败 | 编译失败 | 先运行 `rg`，删除后运行 `mvn -DskipTests compile`；若发现调用点，只改为 roles-first，不恢复旧推断。 |
| 切片扩大到其他业务 service legacy overload | 回归面扩大 | 明确排除 LearningPath/Assessment/KnowledgeCatalog 自身 legacy cleanup，另列 follow-up。 |
| 保留旧签名但改为 fail-closed | API 面仍可被误用 | 本切片选择删除，除非 compile 证据证明必须兼容。 |

## 8. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 只改 Service 授权 API 面与测试。 |
| Frontend rules | PASS | 不改 frontend。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG/model runtime。 |
| Security | PASS | 目标是删除 subject-name role inference。 |
| API / Database | PASS | 无 path/DTO/schema 变更。 |

## 9. Architecture Drift Post-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 只删除 `CourseAccessService` legacy authorization API 面并新增服务测试，仍保持 Controller -> Service -> Repository 分层。 |
| Frontend rules | PASS | 未修改 frontend，未让前端直接调用 LLM。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG/model runtime。 |
| Security | PASS | 删除 subject-name role inference 入口，课程授权公共入口收口为 explicit role facts。 |
| API / Database | PASS | 无 REST path、DTO、schema、migration 或 dependency 变更。 |

## 10. Final Status

Done。P3-4-W 完成后仍不代表 P3-4 全部完成；broader class/course authorization、formal OAuth2/JWK/Spring Security、broader penetration tests 仍待后续。
