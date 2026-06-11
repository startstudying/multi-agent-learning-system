# RUN-20260609 P3-4-R Assessment / GradingEvaluation Backend Architect

## 范围

只读架构分析，未修改代码。

## 结论

P3-4-R 不应只处理 `GradingEvaluation`。`Assessment` 的 answer / wrong-question 读路径和 `GradingEvaluation` HTTP 主路径都仍通过 `currentUserId` 字符串推断 `admin` / `teacher_*`，与 P3-4-M/O/P/Q 的 roles-first 迁移方向不一致。

推荐最小范围：

- `POST /api/assessment/grading-evaluations`
- `GET /api/assessment/answers`
- `GET /api/assessment/answers/{answerId}`
- `GET /api/assessment/wrong-questions`
- `GET /api/assessment/wrong-questions/{wrongQuestionId}`

明确不纳入：

- `POST /api/assessment/answers` 提交答题写入链路
- DB schema / migration
- API path / request DTO / response DTO
- frontend
- 依赖和 formal OAuth2/JWK/Spring Security

## 关键证据

- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`：所有 Assessment/Grading 入口当前只传 `currentUserService.currentUserId()`。
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`：answer/wrong-question list/detail 使用 `isAdmin(currentUserId)` / `isTeacherUser(currentUserId)`。
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`：HTTP evaluation gate 使用 subject-name inference，并调用 legacy `CourseAccessService.requireCourseRead(currentUserId, courseId)`。
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`：已存在 role-aware overload，可直接复用。

## 推荐实现

1. `AssessmentController` 读取 `UserContext currentUser = currentUserService.currentUser()`。
2. 从 `currentUser.roles()` 派生 `currentUserAdmin/currentUserTeacher`。
3. `AssessmentService` 增加 roles-first overload：
   - `listAnswers(currentUserId, admin, teacher, learnerId, courseId, page, size)`
   - `listWrongQuestions(currentUserId, admin, teacher, learnerId, courseId, page, size)`
   - `answerDetail(currentUserId, admin, teacher, answerId)`
   - `wrongQuestionDetail(currentUserId, admin, teacher, wrongQuestionId)`
4. `GradingEvaluationService` 增加 roles-first overload：
   - `evaluate(currentUserId, admin, teacher, request)`
5. HTTP 主路径全部调用 roles-first overload。
6. legacy overload 保留，委托旧推断，用于兼容非目标路径。

## 风险

| 风险 | 缓解 |
|---|---|
| dev/test `X-User-Id` 回归 | `DevAuthFilter` 会为 header fallback 派生 roles，保留兼容测试 |
| admin missing 误变 403 | missing 分支必须使用 explicit `currentUserAdmin` |
| teacher no-prefix 仍失败 | Assessment/Grading 本切片内 CourseAccess 调用全部改用 role-aware overload |
| 范围扩大 | 不删除 `CourseAccessService` legacy overload，不碰提交答题写入 |
