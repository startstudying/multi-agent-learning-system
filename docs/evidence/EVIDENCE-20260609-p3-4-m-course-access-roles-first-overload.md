# EVIDENCE - P3-4-M Course API / CourseAccessService roles-first overload

## 1. 范围

本证据记录 P3-4-M 对 Course API / `CourseAccessService` roles-first overload 的局部收口。

覆盖路径：

- `GET /api/courses`
- `GET /api/courses/{courseId}`
- `GET /api/courses/{courseId}/knowledge-graph`
- `POST /api/courses`
- `POST /api/courses/{courseId}/chapters`
- `POST /api/knowledge-points`
- `POST /api/knowledge-dependencies`

非目标：

- 不迁移 formal OAuth2 / JWK / Spring Security。
- 不全量迁移全仓库 `CourseAccessService` 调用方。
- 不修改 Assessment、RAG Document、Learning Workflow、Resource Generation 等已验收切片。
- 不新增依赖、DB schema、前端、RAG、model provider 或 VectorDB 改动。
- 不宣称整个 P3-4 完成。

## 2. Subagent 证据

| 报告 | 结论 |
|---|---|
| `docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-security.md` | 风险等级 HIGH；建议 Course API 主路径传入 `currentUserId + isAdmin + isTeacherUser`，新增 roles-first overload，避免扩展到 formal OAuth2/JWK/Spring Security。 |
| `docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-backend.md` | 推荐在 `CourseAccessService` 增加 read/manage/list roles-first overload，旧签名保留委托；优先迁移 CourseController、KnowledgePointController、KnowledgeCatalogService。 |
| `docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-test.md` | 建议 `CourseKnowledgeControllerTest` 新增 Bearer admin/teacher/student/spoofed header/missing/foreign 最小 RED 矩阵。 |

## 3. 代码证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/java/com/learningos/knowledge/api/CourseController.java` | Course create/list/detail/chapter/graph 入口读取 `UserContext`，传入显式 role facts，不再只传 legacy `currentUserId`。 |
| `backend/src/main/java/com/learningos/knowledge/api/KnowledgePointController.java` | Knowledge point / dependency 写入口读取 `UserContext`，传入显式 role facts。 |
| `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java` | 新增 roles-first `requireCourseRead(...)`、`requireCourseManage(...)`、`listCoursesForUser(...)` overload；旧签名保留并委托 legacy inference。 |
| `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java` | 新增 roles-first create/read/list/chapter/knowledge point/dependency/graph public overload，并在内部调用 roles-first course access。 |
| `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java` | 新增 JWT helper 与 Bearer admin、teacher、student、`sub=admin`、`sub=teacher_1` role-confusion 防回归测试。 |

## 4. RED 证据

新增 P3-4-M Course API roles-first 测试后，首次运行 focused 命令出现预期 RED：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest test
```

关键失败：

```text
courseListUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader: expected data.length() 2 but was 0
courseDetailAndGraphUseBearerAdminRoleAndIgnoreSpoofedUserIdHeader: expected 200 but was 403
bearerAdminSeesMissingCourseAsNotFoundForDetailAndGraph: expected 404 but was 403
bearerTeacherRoleCanReadAndManageOwnedCourseWithoutTeacherIdPrefix: expected 200 but was 403
bearerUserSubjectAdminDoesNotGainCourseAdminAccess: expected 403 but was 200
bearerUserSubjectTeacherPrefixDoesNotGainTeacherManageAccess: expected 403 but was 200
```

根因：

- `CourseController` / `KnowledgePointController` 只传 `currentUserId`，未把 Bearer role facts 传入业务服务。
- `CourseAccessService` / `KnowledgeCatalogService` 继续用 `"admin"`、`"teacher"`、`teacher_` 字符串推断角色。
- 在 Bearer token 已建立 `UserContext.roles()` 后，业务层二次字符串推断会造成 admin 降权、teacher 降权，以及 `USER sub=admin` / `USER sub=teacher_1` 的角色混淆。

## 5. GREEN / Regression 证据

### 5.1 Focused verification

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest test
```

结果：

```text
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T13:34:15+08:00
```

### 5.2 Adjacent regression

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest,AnalyticsControllerTest test
```

结果：

```text
Tests run: 63, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T13:35:32+08:00
```

### 5.3 Full backend verification

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 403, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-09T13:38:20+08:00
```

说明：Maven 输出包含 Mockito dynamic agent / JVM sharing warnings，不影响测试结果。

## 6. 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | Controller 只提取 current user / role facts；对象授权在 `KnowledgeCatalogService` / `CourseAccessService` 内完成。 |
| Frontend rules | PASS | 未修改 `frontend/**`。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG runtime。 |
| Security | PASS | 权限判断在后端代码完成；Bearer role + spoofed header + role-confusion 测试已覆盖 Course API 主路径。 |
| API / Database | PASS | 未新增 API path、DTO、schema、migration 或依赖。 |

## 7. 限制

- P3-4-M 仅完成 Course API / Knowledge Catalog 主路径 roles-first overload。
- 其他模块对旧 `CourseAccessService` 签名的调用仍保留，属于后续全仓库迁移或各模块 RBAC 切片。
- formal OAuth2 / JWK / Spring Security 迁移仍未完成。
- PromptVersion / Evaluation endpoints full RBAC matrix、RAG KB management full matrix 仍是后续安全切片。
- 当前工作区不是 git repository，本证据以文件内容和 Maven 输出为准，不依赖 git diff。
