# EVIDENCE-20260610-p3-4-dev-test-legacy-fallback-cleanup

## 1. 任务

P3-4 子任务：dev/test legacy fallback cleanup

## 2. 变更摘要

收紧 dev/test legacy subject-name fallback 的边界：

- 保留 dev/test 无 Bearer 时 `X-User-Id` header-only legacy fallback。
- Spring Security `JwtAuthenticationToken` 建立的 Bearer 身份不再因为 `sub = "admin"` 或 `sub = "teacher_*"` 被 `CurrentUserService.isAdmin()` / `isTeacherUser()` 提权。
- 新增 `CurrentUserService.allowsLegacySubjectInferenceForHolderContext()`，仅当没有 Spring Security JWT context 时才允许 legacy subject inference。
- 新增 `SecurityJwtAuthenticationTest` 回归，覆盖 dev/test JWT `roles=["USER"]` + subject-name role-confusion。

## 3. 修改文件

- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
- `backend/src/test/java/com/learningos/common/auth/SecurityJwtAuthenticationTest.java`
- `docs/tasks/TASK-20260610-p3-4-dev-test-legacy-fallback-cleanup.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-dev-test-legacy-fallback-cleanup.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Verification

### RED

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SecurityJwtAuthenticationTest test
```

结果：

- `Tests run: 6, Failures: 1, Errors: 0, Skipped: 0`
- 失败点证明 dev/test JWT `sub=admin` 会被 legacy `isAdmin()` 提权。

### Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SecurityJwtAuthenticationTest test
```

结果：

- `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Adjacent auth

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest,SecurityFilterChainTest,SseProductionAuthStrategyTest test
```

结果：

- `Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- `Tests run: 578, Failures: 0, Errors: 0, Skipped: 1`
- `BUILD SUCCESS`

说明：此前一次 full backend 失败由并行 Maven 进程互踩 `backend/target` 引发 `ClassNotFoundException`；停止并行 Maven 后重跑通过，不判定为代码缺陷。

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 仅改 auth context helper；业务 Controller/Service 未改。 |
| Frontend rules | PASS | 未改前端。 |
| Agent / RAG rules | PASS | 未改 Agent/RAG runtime。 |
| Security | PASS | Bearer JWT role facts 优先，dev/test header-only fallback 兼容保留且不覆盖 JWT。 |
| API / Database | PASS | 未改 API contract 或 schema。 |

## 6. Acceptance

| Criteria | Verdict |
|---|---|
| dev/test 无 Bearer `X-User-Id` fallback 保持兼容 | PASS |
| dev/test Spring Security JWT `sub=admin, roles=["USER"]` 不再让 `isAdmin()` 为 true | PASS |
| dev/test Spring Security JWT `sub=teacher_1, roles=["USER"]` 不再让 `isTeacherUser()` 为 true | PASS |
| production/staging Bearer/header fail-closed 语义未回退 | PASS |
| focused / adjacent auth / full backend 验证完成 | PASS |

最终结论：PASS。P3-4 父项仍保持 open。
