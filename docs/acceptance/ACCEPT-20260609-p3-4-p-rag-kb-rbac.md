# ACCEPT - P3-4-P RAG KB management roles-first RBAC

## 1. 验收结论

P3-4-P RAG KB management roles-first RBAC 验收通过。

本次只验收 RAG Knowledge Base management 主路径的 roles-first 授权、Bearer spoof / subject-name role-confusion 回归、course metadata scope 和 missing/foreign anti-enumeration 行为；不代表 broader class/course、formal OAuth2/JWK/Spring Security、`/api/rag/query` retrieval runtime 或整个 P3-4 已完成。

## 2. 需求验收

| 需求 | 状态 | 证据 |
|---|---|---|
| KB create/list Controller 从 `UserContext.roles()` 派生 role facts | 通过 | `KnowledgeBaseController` 调用 `currentUser()` 并显式传入 admin/teacher facts。 |
| Document management Controller 从 `UserContext.roles()` 派生 role facts | 通过 | `DocumentController` upload/list/detail/reindex/index-task detail 均显式传入 role facts。 |
| Bearer admin 可忽略 spoofed `X-User-Id` 管理 KB | 通过 | Bearer `ADMIN sub=ops_admin` 可 list 全部 active private KB，可 upload foreign private KB。 |
| Teacher 不依赖 `teacher_` subject 前缀 | 通过 | Bearer `TEACHER sub=instructor_1` 可上传 own-course metadata。 |
| `USER sub=teacher_1` 不获得 teacher 权限 | 通过 | `USER sub=teacher_1` 上传 course metadata 返回 403。 |
| `USER sub=admin` 不获得 admin missing 语义 | 通过 | missing document/reindex/index-task 返回 safe `FORBIDDEN`，admin role missing 保留 `NOT_FOUND`。 |
| Service role-aware overload 已实现且旧签名兼容 | 通过 | `KnowledgeBaseService`、`DocumentService`、`PermissionService` 新增 role-aware overload；旧签名保留默认非 admin/teacher。 |
| Course metadata scope 使用 role-aware course access | 通过 | `DocumentService` 调用 role-aware `CourseAccessService.requireCourseRead/requireCourseManage(...)`。 |
| Personal KB 创建能力保留 | 通过 | 未新增学生/普通用户创建个人 KB 限制。 |
| 不改 API / DB / dependency / frontend | 通过 | 未修改 `backend/pom.xml`、migration、frontend 或 API path / request DTO。 |

## 3. 非功能验收

| 项 | 状态 | 说明 |
|---|---|---|
| RED/GREEN | 通过 | RED 首次 focused 运行 `26 run, 6 failures`，修复后 focused GREEN。 |
| Controller focused verification | 通过 | `26 run, 0 failures, 0 errors`。 |
| Permission service focused verification | 通过 | `4 run, 0 failures, 0 errors`。 |
| RAG adjacent regression | 通过 | `30 run, 0 failures, 0 errors`。 |
| Auth / Course adjacent regression | 通过 | `34 run, 0 failures, 0 errors`。 |
| Full backend verification | 通过 | `426 run, 0 failures, 0 errors, 1 skipped`。 |
| 架构漂移 | 通过 | Controller role facts + Service authorization；无 API/DB/frontend/dependency drift。 |
| 依赖审查 | 不需要 | 未新增依赖。 |

## 4. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest test
mvn --% -Dtest=PermissionServiceTest test
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,ChatControllerTest,RagEvaluationControllerTest test
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
mvn test
```

最终全量结果：

```text
Tests run: 426, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-09T16:17:17+08:00
```

## 5. 未验收 / 后续项

- `/api/rag/query` retrieval runtime roles-first 迁移。
- broader class/course 权限模型。
- formal OAuth2/JWK/Spring Security 迁移。
- 更多 legacy `CourseAccessService` caller 迁移。
- RAG KB 与课程绑定 schema / 生命周期治理。
- 更广的权限渗透测试矩阵。
