# ACCEPT - P3-4-N PromptVersion 管理 API RBAC 与 promptText 暴露收口

## 1. 验收结论

P3-4-N PromptVersion 管理 API RBAC 与 `promptText` 暴露收口验收通过。

本次只验收 PromptVersion HTTP 管理 API 的 roles-first read/write gate 和 teacher metadata 脱敏，不代表 broader class/course、full RBAC、formal OAuth2/JWK/Spring Security、Evaluation、RAG KB 或全仓库权限迁移已完成。

## 2. 需求验收

| 需求 | 状态 | 证据 |
|---|---|---|
| `POST /api/agent/prompt-versions` admin-only | 通过 | `teacherCannotUpsertPromptVersion`、`bearerStudentSubjectAdminCannotUpsertPromptVersion` 返回 403；admin 写入返回 200。 |
| Bearer admin 忽略 spoofed `X-User-Id` | 通过 | `bearerAdminCanUpsertPromptVersionDespiteSpoofedUserIdHeader` 返回 200。 |
| `GET` list/detail 仅允许 admin/teacher | 通过 | student/user 读取返回 403；teacher/admin 读取返回 200。 |
| Teacher 响应不包含 `promptText` | 通过 | `teacherPromptVersionListAndDetailDoNotExposePromptText` 断言 list/detail 中 `promptText` 不存在。 |
| Bearer `STUDENT sub=admin` 不提权 | 通过 | `bearerStudentSubjectAdminCannotUpsertPromptVersion` 返回 403。 |
| Bearer `USER sub=teacher_1` 不提权 | 通过 | `bearerUserSubjectWithTeacherPrefixCannotReadPromptManagementData` 返回 403。 |
| Service 层无无鉴权 HTTP 管理入口 | 通过 | `PromptVersionService` 公开管理方法均要求显式 role facts；内部 `findActiveByCode` 保留模型调用读取能力。 |
| API path / request DTO 不变 | 通过 | 未新增 path 或 request DTO；只按角色省略 response `promptText`。 |
| 不新增依赖/schema/frontend | 通过 | 未修改 `backend/pom.xml`、migration 或 `frontend/**`。 |

## 3. 非功能验收

| 项 | 状态 | 说明 |
|---|---|---|
| Focused verification | 通过 | 14 run, 0 failures, 0 errors, 0 skipped |
| Adjacent regression | 通过 | 48 run, 0 failures, 0 errors, 0 skipped |
| Full backend verification | 通过 | 410 run, 0 failures, 0 errors, 1 skipped |
| RED/GREEN | 通过 | 5 个预期 RBAC/脱敏失败先 RED，修复后 GREEN。 |
| 依赖审查 | 不需要 | 未新增依赖。 |
| 架构漂移 | 通过 | Controller 传 role facts；Service 执行权限；DTO 控制字段脱敏；无 API/DB/frontend drift。 |

## 4. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest test
mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest,DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
mvn test
```

最终全量结果：

```text
Tests run: 410, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. 未验收 / 后续项

- Evaluation Set/Run roles-first RBAC matrix。
- GradingEvaluation legacy role inference / old `CourseAccessService` caller migration。
- RAG KB management 权限模型。
- broader class/course 权限模型。
- formal OAuth2/JWK/Spring Security。
- PromptVersion 审批、发布、回滚治理流。

