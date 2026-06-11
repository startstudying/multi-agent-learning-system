# EVIDENCE - P3-4-R Assessment / GradingEvaluation roles-first RBAC

## 1. 范围

本证据覆盖 P3-4-R：

- Assessment answer / wrong-question read paths
- GradingEvaluation HTTP path

目标是让 HTTP 主路径使用 explicit `UserContext.roles()` facts，阻断 legacy subject-name role-confusion，并保留现有 API/DB/DTO 行为。

## 2. 代码证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java` | list/detail/grading paths 读取 `UserContext currentUser = currentUserService.currentUser()`，并用本地 `hasRole(currentUser, "ADMIN"/"TEACHER")` 派生 role facts |
| `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java` | answer/wrong-question list/detail 新增 roles-first overload；HTTP 主路径传入 explicit role facts；CourseAccess 调用使用 role-aware overload |
| `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java` | 新增 `evaluate(currentUserId, currentUserAdmin, currentUserTeacher, request)`；HTTP path 使用 role-aware `CourseAccessService.requireCourseRead(...)` |
| `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java` | 新增 Bearer admin spoof、teacher no-prefix、`USER sub=admin`、`USER sub=teacher_1` role-confusion 和 admin/teacher course missing 语义测试 |

## 3. TDD RED 证据

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
```

结果：

```text
Tests run: 37, Failures: 11, Errors: 0, Skipped: 0
BUILD FAILURE
```

失败命中预期：

- Bearer `ADMIN sub=ops_admin` 读 answer detail / 运行 grading evaluation 返回 `403`。
- Bearer `TEACHER sub=instructor_1` 读 own-course answer/wrong-question / 运行 grading evaluation 返回 `403`。
- Bearer `USER sub=admin` / `USER sub=teacher_1` 错误获得 admin/teacher 权限，返回 `200`。
- Bearer admin missing grading course 返回 `403` 而不是 `NOT_FOUND`。

## 4. Focused GREEN

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
```

结果：

```text
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T17:33:58+08:00
```

## 5. Adjacent Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,EvaluationSetControllerTest,EvaluationRunControllerTest,AnalyticsControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
```

结果：

```text
Tests run: 123, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T17:35:14+08:00
```

## 6. Full Backend Verification

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 442, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-09T17:37:39+08:00
```

## 7. Architecture Drift Re-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 提取身份事实；Service 执行 object/course scope 授权 |
| Frontend rules | PASS | 未修改 frontend |
| Agent / RAG rules | PASS | 未修改 Agent/RAG runtime |
| Security | PASS | 无 secrets；无新增依赖；role-confusion 有回归测试 |
| API / Database | PASS | 无 API path/DTO/DB schema 变更 |

## 8. Integration Review

| Reviewer | Result | Report |
|---|---|---|
| Hegel / code-reviewer | PASS | `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-integration-review.md` |

要点：未发现 API path / DTO / DB migration / frontend / dependency drift；Assessment read paths 与 GradingEvaluation HTTP path 均已使用 explicit role facts；`submit` 写入链路不属于本切片范围。

## 9. 限制

- 未处理 LearningPath / ResourceGeneration course-bound create role facts。
- 未处理 broader class/course matrix。
- 未引入 formal OAuth2/JWK/Spring Security。
- 不声明 P3-4 或 backend architecture TODO 整体完成。
