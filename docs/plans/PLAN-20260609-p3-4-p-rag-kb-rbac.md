# PLAN - P3-4-P RAG KB management roles-first RBAC

## 1. 追踪

- PRD: `docs/product/PRD-20260609-p3-4-p-rag-kb-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-p-rag-kb-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md`

## 2. Subagent Decision

| 字段 | 结论 |
|---|---|
| Use Subagents | Yes |
| Reason | RAG management + backend auth + object scope + tests，涉及 RAG/security/testing 多视角 |
| Parallelism Level | L1 parallel analysis |
| Selected Subagents | Backend/Architect、Security、Test Engineer |
| Implementation Mode | Single Codex implementation after integration review |

已完成并采纳：

- `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-backend.md`
- `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-security.md`
- `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-test.md`
- `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-integration-review.md`

## 3. 实施阶段

| 阶段 | 说明 | 关联任务 | 状态 |
|---|---|---|---|
| 1 | 补齐 PRD/REQ/SPEC/PLAN/TASK/CONTEXT | TASK-01 | 已完成 |
| 2 | 新增 RED 权限回归测试 | TASK-02 | 已完成 |
| 3 | Controller 传递 `UserContext.roles()` role facts | TASK-03 | 已完成 |
| 4 | Service/Permission role-aware overload 与授权迁移 | TASK-04 | 已完成 |
| 5 | 运行 focused/adjacent/full 验证 | TASK-05 | 已完成 |
| 6 | Evidence/Acceptance/Changelog/Memory/Retro | TASK-06 | 已完成 |

## 4. 文件变更清单

| 文件 | 操作 | 阶段 | 负责人 |
|---|---|---|---|
| `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java` | 修改 | 2 | Codex |
| `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java` | 修改 | 2 | Codex |
| `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java` | 可选修改 | 2/4 | Codex |
| `backend/src/main/java/com/learningos/rag/api/KnowledgeBaseController.java` | 修改 | 3 | Codex |
| `backend/src/main/java/com/learningos/rag/api/DocumentController.java` | 修改 | 3 | Codex |
| `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java` | 修改 | 4 | Codex |
| `backend/src/main/java/com/learningos/rag/application/DocumentService.java` | 修改 | 4 | Codex |
| `backend/src/main/java/com/learningos/rag/application/PermissionService.java` | 修改 | 4 | Codex |
| `docs/evidence/EVIDENCE-20260609-p3-4-p-rag-kb-rbac.md` | 新增 | 6 | Codex |
| `docs/acceptance/ACCEPT-20260609-p3-4-p-rag-kb-rbac.md` | 新增 | 6 | Codex |
| `docs/retrospectives/RETRO-20260609-p3-4-p-rag-kb-rbac.md` | 新增 | 6 | Codex |
| `docs/changelog/CHANGELOG.md` | 更新 | 6 | Codex |
| `docs/memory/PROJECT_MEMORY.md` | 更新 | 6 | Codex |
| `docs/memory/BACKEND_MEMORY.md` | 更新 | 6 | Codex |
| `docs/memory/AGENT_RAG_MEMORY.md` | 更新 | 6 | Codex |
| `docs/memory/API_MEMORY.md` | 更新 | 6 | Codex |
| `docs/planning/backend-architecture-todolist.md` | 更新 | 6 | Codex |
| 本切片 PRD/REQ/SPEC/PLAN/TASK/CONTEXT | 新增/更新 | 1/6 | Codex |

## 5. 依赖

- 前置条件：
  - P3-4-I real auth context 已完成。
  - P3-4-M CourseAccessService role-aware overload 已完成。
  - P3-4-O Evaluation roles-first RBAC 已完成。
- 新增依赖：无。

## 6. 风险评估

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| Controller 使用 `CurrentUserService.isAdmin()` / `isTeacherUser()` | dev/test legacy inference 污染 Bearer role tests | Controller 只从 `UserContext.roles()` 计算 role facts |
| Admin list 全局 KB 改动影响 RAG query | 查询权限语义漂移 | 新增 role-aware overload，旧签名保留给旧路径 |
| Course metadata 校验仍走 legacy overload | `USER sub=teacher_1` 被错误放行 | `DocumentService` 显式调用 role-aware `CourseAccessService` |
| Missing response 仍按 userId 判断 | `USER sub=admin` 得到 `NOT_FOUND` | `scopedMissing` 改为 explicit admin fact |
| 个人 KB 创建被误收紧 | 学生端现有能力破坏 | SPEC 明确保留 personal KB 创建 |

## 7. 回滚策略

- 回滚本切片代码文件到上一版本。
- 保留文档并标记任务未完成。
- 无 DB migration，无数据回滚。

## 8. 测试策略

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest test
mvn --% -Dtest=PermissionServiceTest test
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,ChatControllerTest,RagEvaluationControllerTest test
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
mvn test
```

## 9. 架构漂移预检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只提取身份；Service 执行权限 |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime；RAG query 旧权限路径保留 |
| Security | PASS | 无 secrets；无 dependency |
| API / Database | PASS | 无 API/DB contract change |

## 10. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-09 | Approved for implementation after Context Pack |

## 11. 完成证据

| 项 | 结果 |
|---|---|
| RED | `mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest test` 首次观察到 `26 run, 6 failures`，命中预期权限缺口。 |
| Focused | `KnowledgeBaseControllerTest,DocumentControllerTest` 通过：`26 run, 0 failures, 0 errors`。 |
| Service | `PermissionServiceTest` 通过：`4 run, 0 failures, 0 errors`。 |
| Adjacent RAG | `KnowledgeBaseControllerTest,DocumentControllerTest,ChatControllerTest,RagEvaluationControllerTest` 通过：`30 run, 0 failures, 0 errors`。 |
| Adjacent Auth/Course | `DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest` 通过：`34 run, 0 failures, 0 errors`。 |
| Full backend | `mvn test` 通过：`426 run, 0 failures, 0 errors, 1 skipped`。 |
