# EVIDENCE - P3-4-L class analytics roles-first course scope

## 1. 范围

本证据记录 P3-4-L 对 `GET /api/analytics/classes/{courseId}/summary` 的 roles-first 授权与 class course anti-enumeration 收口。

本切片只覆盖 class analytics summary 路径：

- Bearer `ADMIN` role 可以读取 existing course 的 class summary，并忽略 spoofed `X-User-Id`。
- Bearer `TEACHER` role 只有在 `token.sub == Course.teacherId` 时可以读取 class summary。
- Bearer `STUDENT` 即使携带 spoofed `X-User-Id: admin` 也不能读取。
- 非 admin missing course 与 foreign course 统一返回 `FORBIDDEN`。
- admin missing course 保留 `NOT_FOUND` 运维语义。

非目标：

- 不迁移 formal OAuth2 / JWK / Spring Security。
- 不设计 broader class/course domain。
- 不全量迁移 `CourseAccessService` role-aware overload。
- 不新增依赖、DB schema、前端、RAG、model provider 或 VectorDB 改动。
- 不宣称整个 P3-4 完成。

## 2. Subagent 证据

| 报告 | 结论 |
|---|---|
| `docs/subagents/runs/RUN-20260609-p3-4-l-class-analytics-roles-first-scope.md` | Security / Backend / Test 三类专家均建议只修 class summary 入口，controller 传 roles flags，service 保留 teacher own-course 与 admin missing semantics，避免扩大到 `CourseAccessService` 全量迁移。 |

## 3. 代码证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java` | `teacherClassSummary(...)` 调用 `analyticsService.teacherClassSummary(courseId, currentUserId, isAdmin, isTeacherUser)`，不再只传 legacy `currentUserId`。 |
| `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java` | 新增 role-aware overload；admin existing course 允许、admin missing course 返回 `NOT_FOUND`；teacher 必须满足 `currentUserId == Course.teacherId`；非 admin missing/foreign 统一 `FORBIDDEN`。 |
| `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java` | 新增 Bearer admin/teacher/student/spoofed header/missing/foreign class summary 矩阵测试。 |

## 4. RED 证据

新增 P3-4-L class summary 测试后，首次运行 focused 命令出现预期 RED：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest test
```

关键失败：

```text
teacherClassSummaryUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader: expected 200 but was 403
teacherClassSummaryReturnsForbiddenForNonAdminMissingAndForeignCourse: expected 403 but was 404
```

根因：

- `AnalyticsController.teacherClassSummary(...)` 只传 `currentUserId`，未传 Bearer role-derived `isAdmin()` / `isTeacherUser()`。
- `AnalyticsService.teacherClassSummary(...)` 在 course missing 时直接返回 `NOT_FOUND`，非 admin 可区分 missing 与 foreign course。

## 5. GREEN / Regression 证据

### 5.1 Focused verification

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest test
```

结果：

```text
Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T12:49:48+08:00
```

### 5.2 Adjacent regression

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
```

结果：

```text
Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T12:50:55+08:00
```

### 5.3 Full backend verification

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 396, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-09T12:53:14+08:00
```

说明：Maven 输出包含 Mockito dynamic agent / JVM sharing warnings，不影响测试结果。

## 6. 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | Controller 只提取 current user / role flags；对象授权在 `AnalyticsService` 内完成。 |
| Frontend rules | PASS | 未修改 `frontend/**`。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG runtime。 |
| Security | PASS | 权限判断在后端代码完成；Bearer role 优先于 spoofed header 的 class summary 测试已覆盖。 |
| API / Database | PASS | 未新增 API path、DTO、schema、migration 或依赖。 |

## 7. 限制

- P3-4-L 仅完成 class analytics summary roles-first scope。
- broader class/course 权限模型仍未完成。
- formal OAuth2 / JWK / Spring Security 迁移仍未完成。
- PromptVersion / Evaluation endpoints full RBAC matrix、RAG KB management full matrix 仍是后续安全切片。
- 当前工作区不是 git repository，本证据以文件内容和 Maven 输出为准，不依赖 git diff。
