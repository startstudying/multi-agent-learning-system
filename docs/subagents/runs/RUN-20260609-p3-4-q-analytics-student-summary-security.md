# Security & Quality Expert 只读安全分析报告

**建议文件名：** `docs/subagents/runs/RUN-20260609-p3-4-q-backend-permission-security.md`  
**范围：** backend P3-4 剩余权限项，只读检查当前代码与测试  
**约束遵守：** 未修改文件；未启动 `node_repl`；未联网；不声明 P3-4 完成  
**总体风险：** HIGH

## 1. 当前最明显且适合小切片修复的权限风险

### 1.1 Assessment / Grading Evaluation 仍使用 subject-name 推断角色

**严重级别：** HIGH  
**OWASP：** A01 Broken Access Control  
**位置：**

- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`

**问题：**  
`AssessmentController` 只把 `currentUserService.currentUserId()` 传入 service，没有传递 Bearer token 中的显式 `roles()`。下游 `AssessmentService` / `GradingEvaluationService` 使用 `"admin".equals(currentUserId)` 和 `teacher_` 前缀判断权限。这样会产生两类风险：

- `Bearer roles=["ADMIN"] sub="ops_admin"` 不能按 ADMIN 处理，导致 roles-first 正常用户被拒绝。
- `Bearer roles=["USER"] sub="admin"` 或 `sub="teacher_1"` 可能被服务层误提升为 ADMIN / TEACHER，形成 subject-name role-confusion。
- 对 assessment detail/list 的 missing-vs-foreign 行为也会被错误 admin 判定影响，`USER sub=admin` 可能获得 `NOT_FOUND` oracle 或读取管理范围数据。

### 1.2 legacy CourseAccessService caller 仍会放大 role-confusion

**严重级别：** HIGH  
**OWASP：** A01 Broken Access Control  
**位置：**

- `AnalyticsService`
- `GradingEvaluationService`
- `AssessmentService`
- `CourseAccessService`

**问题：**  
P3-4-M 已经给 `CourseAccessService` 增加 roles-first overload，但仍有多个调用点使用 legacy 签名：

```java
courseAccessService.requireCourseRead(currentUserId, courseId);
```

legacy 签名内部继续调用：

```java
return requireCourseRead(currentUserId, isAdmin(currentUserId), isTeacherUser(currentUserId), courseId);
```

这意味着任何传入 `admin` / `teacher_1` subject 的调用路径，都会重新进入 subject-name 推断。

## 2. 必须覆盖的 RED 测试矩阵

建议 P3-4-Q 优先在 HTTP 层增加 RED，因为 HTTP 层最能验证 Bearer 覆盖 spoofed header、roles-first 与 oracle 行为。

| 场景 | 目标端点 | Token / Header | 预期 |
|---|---|---|---|
| Bearer ADMIN spoofed header | `GET /api/analytics/students/{learnerId}/summary?courseId=...` | `Bearer sub=ops_admin roles=[ADMIN]` + `X-User-Id=alice` | 允许 admin 范围 |
| TEACHER no-prefix | `GET /api/analytics/students/{learnerId}/summary?courseId=own` | `Bearer sub=instructor_1 roles=[TEACHER]` | own course OK，不要求 `teacher_` 前缀 |
| USER sub=teacher_1 | `GET /api/analytics/students/{learnerId}/summary?courseId=teacher_1_course` | `Bearer sub=teacher_1 roles=[USER]` | `FORBIDDEN`，不得按教师读班级 |
| teacher missing/foreign | course-scoped student summary | `Bearer roles=[TEACHER]` | missing 与 foreign 均 `FORBIDDEN`，响应不含对象/course id |

Assessment / Grading Evaluation 建议作为后续 P3-4-R 单独切片覆盖。

## 3. 推荐 P3-4-Q 范围和非目标

**推荐范围：**

- 只处理 Analytics student summary 的 roles-first `CourseAccessService` 调用。
- `AnalyticsService.requireCourseReadForStudentSummary(...)` 调用 role-aware overload。
- 对上述 RED 矩阵补测试，特别是 no-prefix teacher 与 `USER sub=teacher_1`。

**非目标：**

- 不做正式 OAuth2/JWK/Spring Security 替换。
- 不删除 `CourseAccessService` legacy 签名，避免扩大切片。
- 不改数据库 schema、API contract、前端。
- 不引入新依赖。
- 不宣称 broader class/course matrix 全部完成。
- 不纳入 Assessment / GradingEvaluation；该项作为 P3-4-R。

## 4. 验证命令建议

```powershell
mvn -f backend/pom.xml -Dtest=AnalyticsControllerTest test
mvn -f backend/pom.xml -Dtest=CourseKnowledgeControllerTest,AnalyticsControllerTest,CurrentUserServiceTest test
mvn -f backend/pom.xml test
```

本轮未运行测试。
