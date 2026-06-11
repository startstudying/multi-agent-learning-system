# ACCEPT - P3-4-O Evaluation Set / Run roles-first RBAC

## 1. 验收结论

P3-4-O Evaluation Set / Run roles-first RBAC 验收通过。

本次只验收 Evaluation Set / Run HTTP 管理主路径的 roles-first 授权、Bearer spoof / role-confusion 回归和 missing/foreign anti-enumeration 行为；不代表 broader class/course、RAG KB management、formal OAuth2/JWK/Spring Security 或整个 P3-4 已完成。

## 2. 需求验收

| 需求 | 状态 | 证据 |
|---|---|---|
| Controller 从 `UserContext.roles()` 派生 role facts | 通过 | `EvaluationSetController` / `EvaluationRunController` 调用 `currentUser()` 并显式传入 admin/teacher facts。 |
| Evaluation Set 管理接口不再依赖 legacy subject 推断 | 通过 | Bearer `ADMIN sub=ops_admin` 可用；`STUDENT sub=admin` 与 `USER sub=teacher_1` 均返回 403。 |
| Evaluation Run record/compare 不再依赖 legacy subject 推断 | 通过 | Bearer admin spoofed header 成功；student/user subject-name role confusion 均返回 403。 |
| Teacher 无需 `teacher_` subject 前缀即可按 role 访问授权范围 | 通过 | `bearerTeacherCanCreateEvaluationSetWithoutTeacherIdPrefix` 覆盖。 |
| 非 admin missing 与 foreign evaluation set 行为统一 | 通过 | `teacherForeignAndMissingEvaluationSetReturnSameForbiddenEnvelope` 覆盖 403 且无 `data`。 |
| 非 admin missing 与 foreign evaluation run comparison 行为统一 | 通过 | `teacherForeignAndMissingEvaluationRunComparisonReturnSameForbiddenEnvelope` 覆盖 403 且无 `data`。 |
| Admin missing 保留 `NOT_FOUND` | 通过 | 旧有 missing 管理路径回归保留 404/`NOT_FOUND`。 |
| Service public 管理方法要求显式 role facts | 通过 | `EvaluationSetServiceTest` / `EvaluationRunServiceTest` 已迁移到 roles-first 签名。 |
| 不改 API / DB / dependency / frontend | 通过 | 未修改 `backend/pom.xml`、migration、frontend 或 API path / request DTO。 |

## 3. 非功能验收

| 项 | 状态 | 说明 |
|---|---|---|
| RED/GREEN | 通过 | RED 首次 focused 运行 `15 run, 9 failures`，修复后 focused GREEN。 |
| Controller focused verification | 通过 | `15 run, 0 failures, 0 errors`。 |
| Service focused verification | 通过 | `19 run, 0 failures, 0 errors`。 |
| Auth-adjacent regression | 通过 | `48 run, 0 failures, 0 errors`。 |
| Cross-RBAC adjacent regression | 通过 | `73 run, 0 failures, 0 errors`。 |
| Full backend verification | 通过 | `419 run, 0 failures, 0 errors, 1 skipped`。 |
| 架构漂移 | 通过 | Controller role facts + Service authorization；无 API/DB/frontend/dependency drift。 |
| 依赖审查 | 不需要 | 未新增依赖。 |

## 4. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test
mvn --% -Dtest=EvaluationSetServiceTest,EvaluationRunServiceTest test
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,PromptVersionControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
mvn test
```

最终全量结果：

```text
Tests run: 419, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. 未验收 / 后续项

- RAG KB management RBAC。
- GradingEvaluation 其他 legacy caller roles-first 迁移。
- broader class/course 权限模型。
- formal OAuth2/JWK/Spring Security 迁移。
- 教师端授权班级/课程数据扩展。
- 学生端 broader class/course 策略扩展。
- 更广的权限渗透测试矩阵。
