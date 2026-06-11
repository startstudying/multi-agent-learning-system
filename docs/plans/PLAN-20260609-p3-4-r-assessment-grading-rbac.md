# PLAN - P3-4-R Assessment / GradingEvaluation roles-first RBAC

## 1. Traceability

- PRD: `docs/product/PRD-20260609-p3-4-r-assessment-grading-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-r-assessment-grading-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md`

## 2. Subagent Decision

| 字段 | 结论 |
|---|---|
| Use Subagents | Yes |
| Reason | 用户要求专家 subagent 并行；本切片涉及 backend auth、object scope、security、testing |
| Parallelism Level | L1 parallel analysis |
| Selected Subagents | Backend/Architect, Security & Quality, Test Engineer |
| Implementation Mode | Single Codex implementation after analysis integration |

集成决策：

- 采纳 Backend/Architect 建议，把 P3-4-R 范围扩为 Assessment read paths + GradingEvaluation HTTP path。
- 不纳入 `POST /api/assessment/answers` 写入链路，不改 DB/API/frontend/dependency。

## 3. Phases

| Phase | Description | Status |
|---|---|---|
| 1 | 保存专家报告，创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT | Done |
| 2 | 在 `AssessmentControllerTest` 增加 Bearer roles-first RED 测试 | Done |
| 3 | 运行 focused RED 并记录失败 | Done |
| 4 | 实现 Controller + Service roles-first overload | Done |
| 5 | 运行 focused / adjacent / full verification | Done |
| 6 | 创建 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO | Done |

## 4. File Change Plan

| File | Action |
|---|---|
| `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java` | 修改 |
| `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java` | 修改 |
| `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java` | 修改 |
| `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java` | 修改 |
| `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-*.md` | 新增 |
| 本切片 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/EVIDENCE/ACCEPT/RETRO | 新增/更新 |
| `docs/changelog/CHANGELOG.md` | 更新 |
| `docs/memory/PROJECT_MEMORY.md` | 更新 |
| `docs/memory/BACKEND_MEMORY.md` | 更新 |
| `docs/memory/API_MEMORY.md` | 更新 |
| `docs/planning/backend-architecture-todolist.md` | 更新 |

## 5. Risk Assessment

| Risk | Impact | Mitigation |
|---|---|---|
| legacy `X-User-Id` tests 回归 | 影响现有 dev/test 兼容 | `DevAuthFilter` 会派生 roles；保留旧 overload |
| admin missing 语义改变 | 运维排障能力下降 | missing 分支使用 explicit `currentUserAdmin` |
| `USER sub=admin/teacher_1` 仍提权 | Broken Access Control | RED 覆盖并禁止 HTTP 主路径 subject 推断 |
| 范围扩大 | blast radius 增大 | 仅限 Assessment read paths + GradingEvaluation，禁止 DB/API/frontend/dependency |

## 6. Test Strategy

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,EvaluationSetControllerTest,EvaluationRunControllerTest,AnalyticsControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 7. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 提取 role facts；Service 授权 |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime |
| Security | PASS | 无 secrets；无 dependency |
| API / Database | PASS | 无 API/DB contract change |

## 8. Verification Evidence

| Item | Result |
|---|---|
| RED | `mvn --% -Dtest=AssessmentControllerTest test` observed `37 run, 11 failures, 0 errors`; failures matched Bearer role facts lost and `USER sub=admin/teacher_1` role-confusion |
| Focused | `AssessmentControllerTest` passed: `37 run, 0 failures, 0 errors` |
| Adjacent | `AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,EvaluationSetControllerTest,EvaluationRunControllerTest,AnalyticsControllerTest,DevAuthFilterTest,CurrentUserServiceTest` passed: `123 run, 0 failures, 0 errors` |
| Full backend | `mvn test` passed: `442 run, 0 failures, 0 errors, 1 skipped` |
