# EVIDENCE - P3-4-O Evaluation Set / Run roles-first RBAC

## 1. 范围

本证据记录 P3-4-O 对 Evaluation Set / Evaluation Run HTTP 管理主路径的局部 RBAC 收口。

覆盖路径：

- `POST /api/evaluation-sets`
- `GET /api/evaluation-sets`
- `GET /api/evaluation-sets/{setId}`
- `POST /api/evaluation-runs`
- `GET /api/evaluation-runs/comparison`

非目标：

- 不迁移 formal OAuth2 / JWK / Spring Security。
- 不修改 API path、request DTO、response DTO、DB schema、migration 或 frontend。
- 不新增依赖。
- 不处理 RAG KB management、broader class/course、GradingEvaluation 其他 legacy caller。
- 不宣称整个 P3-4 或 backend TODO 完成。

## 2. Subagent / Integration 证据

| 报告 | 结论 |
|---|---|
| `docs/subagents/runs/RUN-20260609-p3-4-o-evaluation-rbac.md` | Backend / Security / Test 专家建议本切片收口 Evaluation Set / Run 管理 API 的 roles-first RBAC，并补齐 Bearer spoof、role-confusion、IDOR oracle 回归矩阵。 |
| `docs/subagents/runs/RUN-20260609-p3-4-o-integration-review.md` | 集成评审确认单 Codex 实施；Controller 只从 `UserContext.roles()` 派生 role facts，Service 不再从 `userId` 字符串推断 admin/teacher。 |

## 3. 代码证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/java/com/learningos/evaluation/api/EvaluationSetController.java` | Controller 调用 `CurrentUserService.currentUser()`，从 `UserContext.roles()` 派生 `ADMIN` / `TEACHER` facts，并传给 `EvaluationSetService.upsert/list/get(...)`。 |
| `backend/src/main/java/com/learningos/evaluation/api/EvaluationRunController.java` | Controller 调用 `CurrentUserService.currentUser()`，从 `UserContext.roles()` 派生 `ADMIN` / `TEACHER` facts，并传给 `EvaluationRunService.record/compare(...)`。 |
| `backend/src/main/java/com/learningos/evaluation/application/EvaluationSetService.java` | 管理方法接收 `currentUserId/currentUserAdmin/currentUserTeacher`；授权 helper 不再通过 `admin` / `teacher_` subject 字符串推断角色；非 admin missing/foreign 返回统一 `FORBIDDEN`。 |
| `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java` | record/compare 使用显式 role facts 校验 evaluation set 可见性；非 admin missing/foreign comparison 返回统一 `FORBIDDEN`，admin missing 保留 `NOT_FOUND`。 |
| `backend/src/test/java/com/learningos/evaluation/api/EvaluationSetControllerTest.java` | 新增 Bearer JWT helper 与 admin spoof、teacher no-prefix、`STUDENT sub=admin`、`USER sub=teacher_1`、missing/foreign anti-enumeration 覆盖。 |
| `backend/src/test/java/com/learningos/evaluation/api/EvaluationRunControllerTest.java` | 新增 Bearer JWT helper 与 admin spoof、student subject-admin、USER subject-teacher、comparison missing/foreign anti-enumeration 覆盖。 |
| `backend/src/test/java/com/learningos/evaluation/application/EvaluationSetServiceTest.java` | Service 测试迁移到 roles-first 签名。 |
| `backend/src/test/java/com/learningos/evaluation/application/EvaluationRunServiceTest.java` | Service 测试迁移到 roles-first 签名，并保留原有 prompt-version comparison 业务回归。 |

## 4. RED 证据

新增 Evaluation Set / Run Bearer spoof 与 role-confusion 测试后，首次 focused 运行出现预期 RED：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test
```

观察结果：

```text
Tests run: 15, Failures: 9, Errors: 0, Skipped: 0
BUILD FAILURE
```

关键失败命中：

- Bearer `ADMIN` 被错误拒绝，因为旧路径依赖 legacy `currentUserId == "admin"`。
- Bearer `STUDENT sub=admin` 被错误放行。
- Bearer `USER sub=teacher_1` 被错误放行。
- Teacher missing 与 foreign evaluation set / run comparison 形成 `403` vs `404` oracle。

## 5. GREEN / Regression 证据

### 5.1 Controller focused verification

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test
```

结果：

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 5.2 Service focused verification

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetServiceTest,EvaluationRunServiceTest test
```

结果：

```text
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 5.3 Auth-adjacent regression

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,DevAuthFilterTest,CurrentUserServiceTest test
```

结果：

```text
Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 5.4 Cross-RBAC adjacent regression

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,PromptVersionControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
```

结果：

```text
Tests run: 73, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 5.5 Full backend verification

最终全量后端验证重新运行：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 419, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-09T15:03:11+08:00
```

说明：输出中包含 Mockito dynamic agent / JVM sharing warnings，不影响 Maven 测试结果。

## 6. 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | Controller 仅处理 HTTP 当前用户上下文与 role facts；业务授权仍在 application service。 |
| Frontend rules | PASS | 未修改 `frontend/**`，未引入前端 LLM 调用或密钥暴露。 |
| Agent / RAG rules | PASS | 未修改 Agent runtime、RAG retrieval、trace 写入、model gateway 或 citation 链路。 |
| Security | PASS | 权限判断在后端代码完成；Bearer roles 优先于 spoofed header；无 subject-name role inference；missing/foreign 非 admin 统一 `FORBIDDEN`。 |
| API / Database | PASS | 未新增或修改 API path、request DTO、DB schema、migration、dependency。 |

## 7. 限制

- 本切片只完成 Evaluation Set / Run 管理主路径 roles-first RBAC。
- `CurrentUserService.isAdmin()` / `isTeacherUser()` 的 dev/test legacy fallback 仍存在于兼容层；本切片通过 Controller 显式 `UserContext.roles()` 绕开该兼容推断。
- RAG KB management、broader class/course、formal OAuth2/JWK/Spring Security 仍是后续 P3-4 工作。
- 当前工作区不是 git repository，本证据以文件内容、测试名称与 Maven 输出为准，不依赖 git diff。
