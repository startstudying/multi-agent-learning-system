# PLAN - P3-4-Q Analytics student summary roles-first RBAC

## 1. 追踪

- PRD: `docs/product/PRD-20260609-p3-4-q-analytics-student-summary-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-q-analytics-student-summary-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-q-analytics-student-summary-rbac.md`

## 2. Subagent Decision

| 字段 | 结论 |
|---|---|
| Use Subagents | Yes |
| Reason | 用户要求专家 subagent 并行；本切片涉及 backend auth、object scope 与测试矩阵 |
| Parallelism Level | L1 parallel analysis |
| Selected Subagents | Backend/Architect、Security & Quality、Test Engineer |
| Implementation Mode | Single Codex implementation after analysis integration |

已完成并采纳：

- `docs/subagents/runs/RUN-20260609-p3-4-q-analytics-student-summary-backend.md`
- `docs/subagents/runs/RUN-20260609-p3-4-q-analytics-student-summary-security.md`
- `docs/subagents/runs/RUN-20260609-p3-4-q-analytics-student-summary-test.md`

集成决策：

- P3-4-Q 只处理 Analytics student summary 的 `CourseAccessService` legacy caller。
- Assessment / GradingEvaluation 另起 P3-4-R，避免跨矩阵扩大风险。

## 3. 实施阶段

| 阶段 | 说明 | 关联任务 | 状态 |
|---|---|---|---|
| 1 | 补齐 PRD/REQ/SPEC/PLAN/TASK/CONTEXT | TASK-01 | 已完成 |
| 2 | 新增 RED 权限回归测试 | TASK-02 | 已完成 |
| 3 | 修改 `AnalyticsService` 使用 role-aware `CourseAccessService` overload | TASK-03 | 已完成 |
| 4 | 运行 focused/adjacent/full 验证 | TASK-04 | 已完成 |
| 5 | Evidence/Acceptance/Changelog/Memory/Retro/TODO 更新 | TASK-05 | 已完成 |

## 4. 文件变更清单

| 文件 | 操作 | 阶段 | 负责人 |
|---|---|---|---|
| `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java` | 修改 | 2 | Codex |
| `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java` | 修改 | 3 | Codex |
| 本切片 PRD/REQ/SPEC/PLAN/TASK/CONTEXT | 新增/更新 | 1/5 | Codex |
| `docs/evidence/EVIDENCE-20260609-p3-4-q-analytics-student-summary-rbac.md` | 新增 | 5 | Codex |
| `docs/acceptance/ACCEPT-20260609-p3-4-q-analytics-student-summary-rbac.md` | 新增 | 5 | Codex |
| `docs/retrospectives/RETRO-20260609-p3-4-q-analytics-student-summary-rbac.md` | 新增 | 5 | Codex |
| `docs/changelog/CHANGELOG.md` | 更新 | 5 | Codex |
| `docs/memory/PROJECT_MEMORY.md` | 更新 | 5 | Codex |
| `docs/memory/BACKEND_MEMORY.md` | 更新 | 5 | Codex |
| `docs/memory/API_MEMORY.md` | 更新 | 5 | Codex |
| `docs/planning/backend-architecture-todolist.md` | 更新 | 5 | Codex |

## 5. 风险评估

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| 测试只覆盖 header fallback，不覆盖 Bearer roles | 无法证明 roles-first 修复 | 新增 Bearer JWT tests |
| `USER sub=teacher_1` 仍被 legacy caller 提权 | Broken Access Control | RED 测试覆盖未 enrollment self summary 场景 |
| 缺失课程响应泄漏存在性 | IDOR/oracle 风险 | missing/foreign 对非 admin 均断言 `FORBIDDEN` 且无 `data` |
| 一次改 Assessment/GradingEvaluation | blast radius 过大 | 明确排除，后续 P3-4-R |

## 6. 测试策略

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 7. 架构漂移预检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 已提供 role facts；Service 执行授权 |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime |
| Security | PASS | 无 secrets；无 dependency |
| API / Database | PASS | 无 API/DB contract change |

## 8. 完成证据

| 项 | 结果 |
|---|---|
| RED | `mvn --% -Dtest=AnalyticsControllerTest test` 首次观察到 `34 run, 3 failures`，命中 admin/teacher role facts 被 legacy caller 丢失。 |
| Focused | `AnalyticsControllerTest` 通过：`34 run, 0 failures, 0 errors`。 |
| Adjacent | `AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest` 通过：`68 run, 0 failures, 0 errors`。 |
| Full backend | `mvn test` 通过：`431 run, 0 failures, 0 errors, 1 skipped`。 |
