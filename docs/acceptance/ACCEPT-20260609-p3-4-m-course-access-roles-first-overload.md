# ACCEPT - P3-4-M Course API / CourseAccessService roles-first overload

## 1. 验收结论

P3-4-M Course API / `CourseAccessService` roles-first overload 局部迁移验收通过。

本次只验收 Course API / Knowledge Catalog 主路径的 roles-first read/list/manage scope，不代表 broader class/course、full RBAC、formal OAuth2/JWK/Spring Security 或全仓库 `CourseAccessService` 迁移已完成。

## 2. 需求验收

| 需求 | 状态 | 证据 |
|---|---|---|
| `CourseAccessService` roles-first read/manage/list overload | 通过 | 新增 overload，旧签名保留委托。 |
| `CourseController` 传递 role facts | 通过 | Course create/list/detail/chapter/graph 入口读取 `UserContext` 并传显式 roles。 |
| `KnowledgePointController` 传递 role facts | 通过 | Knowledge point / dependency 写入口读取 `UserContext` 并传显式 roles。 |
| Bearer `ADMIN sub=ops_admin` 可 list/detail/graph | 通过 | `courseListUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`、`courseDetailAndGraphUseBearerAdminRoleAndIgnoreSpoofedUserIdHeader` 返回 200。 |
| Bearer admin missing course 为 `NOT_FOUND` | 通过 | `bearerAdminSeesMissingCourseAsNotFoundForDetailAndGraph` 返回 404。 |
| Bearer `TEACHER sub=instructor_1` 可读写 own course | 通过 | `bearerTeacherRoleCanReadAndManageOwnedCourseWithoutTeacherIdPrefix` 返回 200。 |
| Bearer student spoofed admin header 不能读写 foreign course | 通过 | `bearerStudentRoleWithSpoofedAdminHeaderCannotReadOrManageForeignCourse` 返回 403 且响应不含 courseId。 |
| Bearer `USER sub=admin` 不升权 | 通过 | `bearerUserSubjectAdminDoesNotGainCourseAdminAccess` 返回 403。 |
| Bearer `USER sub=teacher_1` 不升权 | 通过 | `bearerUserSubjectTeacherPrefixDoesNotGainTeacherManageAccess` 返回 403。 |
| API contract 不变 | 通过 | 未新增 path、DTO 或 response envelope。 |
| 不新增依赖/schema/frontend | 通过 | 未修改 `backend/pom.xml`、migration、`frontend/**`。 |

## 3. 非功能验收

| 项 | 状态 | 说明 |
|---|---|---|
| Focused verification | 通过 | 20 run, 0 failures, 0 errors, 0 skipped |
| Adjacent regression | 通过 | 63 run, 0 failures, 0 errors, 0 skipped |
| Full backend verification | 通过 | 403 run, 0 failures, 0 errors, 1 skipped |
| RED/GREEN | 通过 | Bearer admin/teacher 与 role-confusion 测试先 RED，修复后 GREEN。 |
| 依赖审查 | 不需要 | 未新增依赖。 |
| 架构漂移 | 通过 | Controller 传 role facts；service 执行对象授权；无 API/DB/frontend drift。 |

## 4. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest,AnalyticsControllerTest test
mvn test
```

最终全量结果：

```text
Tests run: 403, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. 未验收 / 后续项

- broader class/course 权限模型。
- formal OAuth2/JWK/Spring Security。
- PromptVersion / Evaluation endpoints full RBAC matrix。
- RAG KB management full permission matrix。
- 全仓库 `CourseAccessService` 旧签名调用方 roles-first 迁移。
