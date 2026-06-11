# TASK - P3-4-P RAG KB management roles-first RBAC

## 1. 追踪

- PLAN: `docs/plans/PLAN-20260609-p3-4-p-rag-kb-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md`
- 任务编号: TASK-20260609-p3-4-p

## 2. 目标

将 RAG KB management 主路径迁移到 roles-first RBAC，补齐 Bearer spoof、subject-name role-confusion、missing-vs-foreign oracle 回归测试。

## 3. 范围

### 纳入范围

- `KnowledgeBaseController` / `DocumentController` role facts 传递。
- `KnowledgeBaseService` / `DocumentService` / `PermissionService` role-aware overload。
- Document course metadata scope 使用 role-aware `CourseAccessService` overload。
- Controller tests 和必要 service tests。
- Evidence / Acceptance / Memory / Changelog / Retro。

### 排除范围

- Formal OAuth2/JWK/Spring Security。
- KB-course binding schema。
- `/api/rag/query` retrieval runtime。
- Parser/vector/index worker/storage/model provider。
- Frontend。
- DB migration / dependency。

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/rag/api/KnowledgeBaseController.java`
- `backend/src/main/java/com/learningos/rag/api/DocumentController.java`
- `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java`
- `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java`
- `docs/product/PRD-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. 禁止修改的文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- Parser/vector/index worker/storage/provider/model runtime。
- 非 RAG KB management / auth-adjacent 测试的后端模块。

## 6. 实施步骤

1. 补齐本切片 PRD/REQ/SPEC/PLAN/TASK/CONTEXT。
2. 新增 RED 测试：
   - Bearer admin list all active KB despite spoofed header。
   - Bearer admin upload to foreign private KB despite spoofed header。
   - Bearer teacher own-course metadata without `teacher_` prefix。
   - Bearer `USER sub=teacher_1` cannot upload course metadata。
   - Bearer admin missing document/index task returns `NOT_FOUND`。
   - Bearer `USER sub=admin` missing document/index task returns `FORBIDDEN`。
3. 修改 Controller，从 `CurrentUserService.currentUser()` 读取 `UserContext` 并从 `roles()` 派生 role facts。
4. 修改 `PermissionService` role-aware overload，admin role 可 list/read/write active KB。
5. 修改 `KnowledgeBaseService` / `DocumentService` role-aware signatures，保留旧签名兼容。
6. 修改 `DocumentService` course metadata scope 和 `scopedMissing(...)`。
7. 运行 focused / adjacent / full verification。
8. 创建 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO。

## 7. 测试命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest test
mvn --% -Dtest=PermissionServiceTest test
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,ChatControllerTest,RagEvaluationControllerTest test
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
mvn test
```

## 8. 完成标准

- [x] PRD/REQ/SPEC/PLAN/TASK/CONTEXT 已存在。
- [x] RED 测试已先失败，失败原因命中权限缺口。
- [x] Controller 不使用 legacy helper 派生 Bearer role facts。
- [x] Service role-aware authorization 已实现。
- [x] 旧签名兼容路径未破坏。
- [x] focused/adjacent/full backend tests 已运行或限制已说明。
- [x] 无 API/DB/frontend/dependency drift。
- [x] Evidence 文档已创建。
- [x] Acceptance 报告已创建。
- [x] Changelog / Memory / TODO 已更新。

## 9. 状态

| 字段 | 值 |
|---|---|
| 状态 | 已完成 |
| 负责人 | Codex |
| 开始日期 | 2026-06-09 |
| 完成日期 | 2026-06-09 |
