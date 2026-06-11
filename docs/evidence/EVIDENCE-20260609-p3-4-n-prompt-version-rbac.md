# EVIDENCE - P3-4-N PromptVersion 管理 API RBAC 与 promptText 暴露收口

## 1. 范围

本证据记录 P3-4-N 对 PromptVersion HTTP 管理 API 的局部权限收口。

覆盖路径：

- `POST /api/agent/prompt-versions`
- `GET /api/agent/prompt-versions`
- `GET /api/agent/prompt-versions/{code}/{version}`

非目标：

- 不迁移 formal OAuth2 / JWK / Spring Security。
- 不修改 PromptVersion DB schema。
- 不新增依赖。
- 不修改前端。
- 不处理 Evaluation Set/Run、GradingEvaluation、RAG KB management 或全量 legacy `CourseAccessService` 调用方。
- 不宣称整个 P3-4 或 backend TODO 完成。

## 2. Subagent 证据

| 报告 | 结论 |
|---|---|
| `docs/subagents/runs/RUN-20260609-p3-4-n-next-security.md` | 推荐优先修复 PromptVersion 管理 API。风险为无鉴权管理面 + 可写 active prompt + 读取完整 `promptText`。 |
| `docs/subagents/runs/RUN-20260609-p3-4-n-next-backend.md` | 后端专家推荐 GradingEvaluation roles-first 迁移；已由 Main Codex 捕获为备选。 |
| `docs/subagents/runs/RUN-20260609-p3-4-n-next-test.md` | 测试专家推荐 Evaluation Set/Run roles-first 矩阵；已保留为后续候选。 |
| `docs/subagents/runs/RUN-20260609-p3-4-n-integration-review.md` | 集成评审最终选择 PromptVersion，因为其当前是管理面裸露，安全风险高于已有过渡鉴权但 roles-first 不完整的路径。 |

## 3. 代码证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java` | Controller 注入 `CurrentUserService`，从 `UserContext.roles()` 计算 admin/teacher facts，并传入 Service。 |
| `backend/src/main/java/com/learningos/agent/application/PromptVersionService.java` | `upsert(...)` 需要 admin；`list/get(...)` 需要 admin 或 teacher；未授权用户在查询/写入前被拒绝。 |
| `backend/src/main/java/com/learningos/agent/dto/PromptVersionResponse.java` | 增加 non-null JSON 序列化和 `from(promptVersion, includePromptText)`；teacher metadata 响应省略 `promptText`。 |
| `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java` | 新增 Bearer admin/teacher/student/user、spoofed header、role-confusion 和 teacher 脱敏矩阵。 |
| `backend/src/test/java/com/learningos/agent/application/PromptVersionServiceTest.java` | Service 测试显式使用 role-aware 方法，并覆盖 teacher metadata access 不返回 prompt text。 |

## 4. RED 证据

新增 P3-4-N PromptVersion RBAC 测试后，首次运行 focused 命令出现预期 RED：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=PromptVersionControllerTest test
```

关键失败：

```text
PromptVersionControllerTest.bearerStudentSubjectAdminCannotUpsertPromptVersion: expected 403 but was 200
PromptVersionControllerTest.studentCannotReadPromptVersionText: expected 403 but was 200
PromptVersionControllerTest.bearerUserSubjectWithTeacherPrefixCannotReadPromptManagementData: expected 403 but was 200
PromptVersionControllerTest.teacherCannotUpsertPromptVersion: expected 403 but was 200
PromptVersionControllerTest.teacherPromptVersionListAndDetailDoNotExposePromptText: Expected no value at JSON path "$.data[0].promptText" but found: 'Internal prompt text'
Tests run: 9, Failures: 5, Errors: 0, Skipped: 0
BUILD FAILURE
```

根因：

- `PromptVersionController` 未读取 `CurrentUserService`。
- `PromptVersionService` 没有 admin/teacher gate。
- `PromptVersionResponse` 总是返回完整 `promptText`。

## 5. GREEN / Regression 证据

### 5.1 Focused verification

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest test
```

结果：

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T14:16:36+08:00
```

### 5.2 Adjacent regression

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest,DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
```

结果：

```text
Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T14:16:35+08:00
```

说明：Maven 输出中出现一次 Surefire 临时目录 warning，但最终 `BUILD SUCCESS` 且所有测试通过。

### 5.3 Full backend verification

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 410, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-09T14:18:54+08:00
```

说明：Maven 输出包含 Mockito dynamic agent / JVM sharing warnings，不影响测试结果。

## 6. 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | Controller 只提取 current user / role facts；权限和脱敏策略在 Service/DTO 边界完成。 |
| Frontend rules | PASS | 未修改 `frontend/**`。 |
| Agent / RAG rules | PASS | 未修改 Agent runtime、RAG retrieval、Trace 写入或模型调用链路。 |
| Security | PASS | 权限判断在后端代码完成；Bearer role + spoofed header + role-confusion + `promptText` 脱敏测试已覆盖。 |
| API / Database | PASS | 未新增 API path、request DTO、schema、migration 或依赖。响应字段仅按角色省略 `promptText`。 |

## 7. 限制

- P3-4-N 只完成 PromptVersion HTTP 管理面 RBAC 和 `promptText` 暴露收口。
- `findActiveByCode(...)` 内部模型调用读取能力保留，不纳入 HTTP RBAC。
- Evaluation Set/Run、GradingEvaluation、RAG KB management 和其他 legacy role inference 仍是后续 P3-4 切片。
- formal OAuth2 / JWK / Spring Security 迁移仍未完成。
- 当前工作区不是 git repository，本证据以文件内容和 Maven 输出为准，不依赖 git diff。

