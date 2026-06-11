# EVIDENCE-20260610-p3-4-prompt-version-forged-id-object-oracle-guards

## 1. 任务

P3-4 子任务：PromptVersion forged-id object-oracle guards

## 2. 变更摘要

测试-only 补强 PromptVersion 管理接口 forged-id / object-oracle 矩阵。

新增测试：

- `bearerAdminMissingPromptVersionReturnsNotFoundDespiteSpoofedUserIdHeader`
- `teacherMissingPromptVersionKeepsAuthorizedNotFoundWithoutPromptText`
- `studentMissingPromptVersionReturnsForbiddenWithoutOracle`
- `bearerUserSubjectAdminCannotReadPromptVersionManagementData`

验证点：

- Bearer `ADMIN` + spoofed `X-User-Id` 查询 missing prompt version 保持授权运维语义 `NOT_FOUND`，且无 `data`。
- Bearer `TEACHER` 是规格允许的 metadata reader；missing detail 保持 `NOT_FOUND`，且不泄露 `promptText` 或 `data`。
- Bearer `STUDENT` + spoofed admin header 查询 missing prompt version 返回 `FORBIDDEN`，响应不包含 forged code/version 或 `Prompt version not found`。
- Bearer `USER sub=admin` 不能通过 subject-name role-confusion 读取 PromptVersion list/detail，响应不泄露 prompt code 或 prompt text。

## 3. 修改文件

- `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-prompt-version-forged-id-object-oracle-guards.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-prompt-version-forged-id-object-oracle-guards.md`
- `docs/subagents/runs/RUN-20260610-p3-4-prompt-version-forged-id-object-oracle-guards.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Verification

### Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=PromptVersionControllerTest test
```

结果：

- `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=PromptVersionControllerTest,SecurityFilterChainTest test
```

结果：

- `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- `Tests run: 572, Failures: 0, Errors: 0, Skipped: 1`
- `BUILD SUCCESS`

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 仅补测试，未改 Controller / Service / Repository 分层。 |
| Frontend rules | PASS | 未改前端。 |
| Agent / RAG rules | PASS | 未改 Agent/RAG runtime。 |
| Security | PASS | 补强 Bearer/header spoofing、role-confusion、object-oracle 测试；未新增 secret。 |
| API / Database | PASS | 未改 API contract 或 schema。 |

## 6. Acceptance

| Criteria | Verdict |
|---|---|
| PromptVersion missing forged-id / object-oracle 测试已补齐 | PASS |
| Bearer spoofed header 不改变 admin/teacher/student 授权结果 | PASS |
| `USER sub=admin` 不读取 PromptVersion 管理数据 | PASS |
| 无生产代码、API、DTO、DB、依赖、前端变更 | PASS |
| focused / adjacent / full backend 验证完成 | PASS |

最终结论：PASS。P3-4 父项仍保持 open。
