# EVIDENCE-20260608 学生分析摘要课程范围权限收口

## 1. 范围

本证据对应 `GET /api/analytics/students/{learnerId}/summary?courseId=...` 的 P3-4-J 切片：

- student 只能读取自己；带 `courseId` 时必须具备 active enrollment。
- teacher 必须提供 `courseId`，且只能读取 own-course active enrolled learner 的课程内摘要。
- admin 可读取任意 learner 的全局摘要或课程内摘要。
- course-scoped 摘要只聚合对应课程的 path、mastery、wrong-question 信号。

## 2. RED 观察

本切片前序 TDD RED 已观察到旧行为不满足新权限矩阵：

| Test | 旧行为 |
|---|---|
| `teacherStudentSummaryRequiresCourseId` | expected `400`, got `403` |
| `teacherCanReadCourseScopedStudentSummaryForActiveEnrolledLearner` | expected `200`, got `403` |
| `adminCanReadGlobalAndCourseScopedStudentSummary` | expected `200`, got `403` |
| `studentCourseScopedSummaryRejectsUnenrolledCourse` | expected `403`, got `200` |

结论：旧实现仍偏 owner-only，且 `courseId` 未参与授权和聚合过滤。

## 3. GREEN / 回归验证

### Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest test
```

结果：

```text
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-08T22:55:45+08:00
```

### Adjacent regression

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,AssessmentControllerTest test
```

结果：

```text
Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-08T22:56:34+08:00
```

### Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 350, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-08T22:58:34+08:00
```

## 4. 关键代码证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java` | `studentSummary(...)` 接收可选 `courseId`，并传入 `currentUserId`、`isAdmin()`、`isTeacherUser()`。 |
| `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java` | service 层执行 teacher 缺少 `courseId` 校验、student foreign learner 拒绝、`CourseAccessService.requireCourseRead(...)`、teacher active enrolled learner 校验。 |
| `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java` | course-scoped 聚合过滤 `LearningPath.goalId`、`LearningPathNode.pathId`、`KnowledgePoint.courseId`、`MasteryRecord.knowledgePointId`、`WrongQuestion.knowledgePointId`。 |
| `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java` | 覆盖 teacher missing courseId、teacher own-course enrolled learner、teacher foreign/unenrolled、student unenrolled course、admin global/course-scoped 行为。 |

## 5. 架构漂移检查

| Check | Result |
|---|---|
| Controller 只负责读取请求与当前用户上下文 | PASS |
| 权限与课程范围聚合在 Service 层完成 | PASS |
| 未新增依赖 | PASS |
| 未新增 DB migration / schema | PASS |
| 未修改 frontend | PASS |
| 未修改 RAG parser / model provider / assessment / resource 非相关实现 | PASS |

## 6. 限制与后续

- 本切片只关闭 student summary 课程范围权限矩阵，不等同于 P3-4 全部完成。
- broader class/course 权限矩阵、正式 OAuth2/JWK/Spring Security 资源服务器和更完整渗透测试仍是后续 P3-4 工作。
