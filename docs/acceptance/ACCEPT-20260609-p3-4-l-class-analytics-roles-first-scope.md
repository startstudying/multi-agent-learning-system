# ACCEPT - P3-4-L class analytics roles-first course scope

## 1. 验收结论

P3-4-L class analytics roles-first course scope 验收通过。

本次只验收 `GET /api/analytics/classes/{courseId}/summary` 的 roles-first class/course 授权与 non-admin missing/foreign anti-enumeration，不代表 broader class/course 权限模型、formal OAuth2/JWK/Spring Security 或全量端点 RBAC 已完成。

## 2. 需求验收

| 需求 | 状态 | 证据 |
|---|---|---|
| Bearer `ADMIN` 读取 existing class summary | 通过 | `teacherClassSummaryUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader` 返回 200。 |
| Bearer `ADMIN` 忽略 spoofed `X-User-Id` | 通过 | 同一测试携带 `X-User-Id: alice` 仍按 Bearer admin 授权。 |
| Bearer `TEACHER` 仅可读取自己课程 | 通过 | `teacherClassSummaryAllowsBearerTeacherWhenSubjectOwnsCourse` 返回 200；foreign teacher 测试返回 403。 |
| Bearer `STUDENT` 不能通过 spoofed admin header 读取 | 通过 | `teacherClassSummaryRejectsBearerStudentWithSpoofedAdminHeader` 返回 `FORBIDDEN`。 |
| 非 admin missing/foreign course anti-enumeration | 通过 | `teacherClassSummaryReturnsForbiddenForNonAdminMissingAndForeignCourse` 对 missing 与 foreign 都返回 `FORBIDDEN`。 |
| admin missing course 运维语义 | 通过 | `teacherClassSummaryAdminMissingCourseRemainsNotFound` 返回 `NOT_FOUND`。 |
| API contract 不变 | 通过 | 未新增 path、DTO 或 response envelope。 |
| 不新增依赖/schema/frontend | 通过 | 未修改 `backend/pom.xml`、migration、`frontend/**`。 |

## 3. 非功能验收

| 项 | 状态 | 说明 |
|---|---|---|
| Focused verification | 通过 | 29 run, 0 failures, 0 errors, 0 skipped |
| Adjacent regression | 通过 | 56 run, 0 failures, 0 errors, 0 skipped |
| Full backend verification | 通过 | 396 run, 0 failures, 0 errors, 1 skipped |
| RED/GREEN | 通过 | Bearer admin class summary 与 non-admin missing course 测试先 RED，修复后 GREEN。 |
| 依赖审查 | 不需要 | 未新增依赖。 |
| 架构漂移 | 通过 | Controller 传递 role flags；service 执行对象授权；无 API/DB/frontend drift。 |

## 4. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

最终全量结果：

```text
Tests run: 396, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. 未验收 / 后续项

- broader class/course 权限模型。
- formal OAuth2/JWK/Spring Security。
- PromptVersion / Evaluation endpoints full RBAC matrix。
- RAG KB management full permission matrix。
- `CourseAccessService` 全局 role-aware overload 迁移。
