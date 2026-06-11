# EVIDENCE - P3-4-Q Analytics student summary roles-first RBAC

## 1. 范围

本证据覆盖 P3-4-Q：`GET /api/analytics/students/{learnerId}/summary?courseId=...` 的课程读取从 legacy `CourseAccessService.requireCourseRead(currentUserId, courseId)` 迁移到 role-aware overload。

## 2. 代码证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java` | `requireCourseReadForStudentSummary(...)` 现在调用 `courseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId)` |
| `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java` | 新增 Bearer admin spoof、admin missing、teacher no-prefix、`USER sub=teacher_1` role-confusion、teacher missing/foreign anti-enumeration 回归测试 |

## 3. TDD RED 证据

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest test
```

结果：

```text
Tests run: 34, Failures: 3, Errors: 0, Skipped: 0
BUILD FAILURE
```

预期失败：

- `studentSummaryUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`: expected `200`, actual `403`
- `studentSummaryBearerAdminMissingCourseRemainsNotFound`: expected `404`, actual `403`
- `bearerTeacherCanReadCourseScopedStudentSummaryForOwnCourseWithoutTeacherIdPrefix`: expected `200`, actual `403`

解释：失败命中 legacy `CourseAccessService` 调用丢失 explicit role facts 的缺口。

## 4. GREEN / Focused 验证

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest test
```

结果：

```text
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T17:03:06+08:00
```

## 5. Adjacent 验证

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
```

结果：

```text
Tests run: 68, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T17:04:14+08:00
```

## 6. Full backend 验证

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 431, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-09T17:06:39+08:00
```

## 7. 架构漂移复检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 继续只传入身份事实；Service 层执行课程权限 |
| Frontend rules | PASS | 未修改 frontend |
| Agent / RAG rules | PASS | 未修改 Agent/RAG runtime |
| Security | PASS | 无 secrets；无新增依赖；role-confusion 有回归测试 |
| API / Database | PASS | 无 API path/DTO/DB schema 变更 |

## 8. 限制

- 本切片未处理 Assessment / GradingEvaluation / LearningPath / ResourceGeneration 的其他 legacy `CourseAccessService` caller。
- 本切片未引入 formal OAuth2/JWK/Spring Security。
- P3-4 仍未整体完成。
