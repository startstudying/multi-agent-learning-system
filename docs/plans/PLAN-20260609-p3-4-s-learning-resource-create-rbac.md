# PLAN - P3-4-S LearningPath / ResourceGeneration course-bound create roles-first RBAC

## 1. Traceability

- PRD: `docs/product/PRD-20260609-p3-4-s-learning-resource-create-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-s-learning-resource-create-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-s-learning-resource-create-rbac.md`

## 2. Task Classification

| Field | Value |
|---|---|
| Task type | Bug fix / security hardening |
| Execution focus | Reproduce role-confusion with RED tests, identify root cause, smallest authorization fix, avoid broad refactor |

## 3. Subagent Decision

| 字段 | 结论 |
|---|---|
| Use Subagents | Yes |
| Reason | 用户要求专家 subagent 并行；本切片涉及 backend auth、course enrollment scope、Agent resource generation side effects、testing |
| Parallelism Level | L1 parallel analysis |
| Selected Subagents | Backend/Architect, Security & Quality, Test Engineer, Integration Reviewer |
| Implementation Mode | Single Codex implementation after analysis integration |

冲突决议：

- 采纳 Architect/Security：ResourceGeneration 保持 owner-only，不扩展 admin/teacher 代创建能力。
- 采纳 Test Engineer 的 role-confusion 与 side-effect 测试方向，但将 “ResourceGeneration admin 可代创建” 用例改为 “admin 仍不能为其他 learner 创建”。
- LearningPath 保留 P3-4-D admin 代创建语义，仅由 explicit `ADMIN` role 驱动。

## 4. Phases

| Phase | Description | Status |
|---|---|---|
| 1 | 保存专家报告，创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT | Done |
| 2 | 在 LearningWorkflowControllerTest / ResourceGenerationControllerTest 增加 RED tests | Done |
| 3 | 运行 focused RED 并记录失败 | Done |
| 4 | 实现 Controller + Service + CourseAccess roles-first overload | Done |
| 5 | 运行 focused / adjacent / full verification | Done |
| 6 | 创建 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO | Done |

## 5. File Change Plan

| File | Action |
|---|---|
| `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java` | 修改 |
| `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java` | 修改 |
| `backend/src/main/java/com/learningos/learning/api/LearningPathController.java` | 修改 |
| `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java` | 修改 |
| `backend/src/main/java/com/learningos/agent/api/ResourceGenerationController.java` | 修改 |
| `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java` | 修改 |
| `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java` | 修改 |
| 本切片 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/EVIDENCE/ACCEPT/RETRO | 新增/更新 |
| `docs/subagents/runs/RUN-20260609-p3-4-s-learning-resource-*.md` | 新增 |
| `docs/changelog/CHANGELOG.md` | 更新 |
| `docs/memory/PROJECT_MEMORY.md` | 更新 |
| `docs/memory/BACKEND_MEMORY.md` | 更新 |
| `docs/memory/API_MEMORY.md` | 更新 |
| `docs/planning/backend-architecture-todolist.md` | 更新 |

## 6. Risk Assessment

| Risk | Impact | Mitigation |
|---|---|---|
| ResourceGeneration 被误扩权为 admin 代创建 | AI resource side effects blast radius 扩大 | SPEC 明确 owner-only，RED 测试覆盖 admin 代创建拒绝 |
| `USER sub=admin` 仍绕过 enrollment | Broken Access Control | Controller 只从 roles 派生 facts；ResourceGeneration enrollment bypass 传 `false` |
| LearningPath admin 语义丢失 | 管理员无法代建 path | explicit `ADMIN` role 驱动 admin bypass |
| side effects 前置检查顺序错误 | Forbidden 请求留下 task/resource/review/trace/model logs | 测试断言相关 repository count |
| 范围膨胀 | 改动 detail/trace/review 等未计划路径 | Context Pack 限定允许文件与禁止范围 |
| Orchestrator `RESOURCE_GENERATION` workflow create 仍走 legacy `createTaskInWorkflow(...)` | 如果把所有 ResourceGeneration create 入口都纳入验收，会留下 role-confusion 旁路风险 | 本切片只验收 direct `POST /api/resources/generation-tasks`；后续单独开 P3-4-T 迁移 Orchestrator roles-first 调用 |

## 7. Test Strategy

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 8. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 提取 role facts；Service 授权 |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime；仅确保 create 授权在 Agent side effects 前 |
| Security | PASS | 无 secrets；无 dependency |
| API / Database | PASS | 无 API/DB contract change |

## 9. Verification Evidence

| Command | Result |
|---|---|
| `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test` | RED observed before fix: `32 run, 3 failures`; GREEN after fix: `32 run, 0 failures, 0 errors` |
| `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test` | `91 run, 0 failures, 0 errors` |
| `mvn test` | `446 run, 0 failures, 0 errors, 1 skipped` |

## 10. Final Status

Done for direct HTTP API scope.

P3-4 remains open for broader class/course authorization, Orchestrator `RESOURCE_GENERATION` roles-first create, formal OAuth2/JWK/Spring Security, and broader penetration testing.
