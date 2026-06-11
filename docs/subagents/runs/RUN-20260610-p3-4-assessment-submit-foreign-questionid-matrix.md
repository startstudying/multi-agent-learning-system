# RUN-20260610 P3-4 Assessment submit foreign-questionId matrix

## 范围

只处理 `POST /api/assessment/answers` 的 foreign / cross-course `questionId` 伪造提交边界。不处理 Evaluation/Review、dev/test fallback、frontend SSE。

## 已新增

- Mini TASK：`docs/tasks/TASK-20260610-p3-4-assessment-submit-foreign-questionid-matrix.md`
- HTTP 回归测试：`AssessmentControllerTest.submitAnswerRejectsBearerStudentForeignCourseQuestionIdWithoutSideEffects`

## 测试语义

Bearer `USER sub=alice` 即使伪造 `X-User-Id: teacher_b`，也不能用 `alice` 的 `learnerId` 提交 `bob` foreign course 下的 `questionId`。

期望：

- HTTP `403 FORBIDDEN`
- 响应无 `data`
- 响应不泄露 foreign `questionId`、foreign `courseId`、foreign knowledge title、`requestId`
- 拒绝请求不得新增 `AnswerRecord`、`GradingResult`、`MasteryRecord`、`WrongQuestion`、`LearningEvent`

## 初步安全判断

当前代码审查显示 `AssessmentController.submit(...)` 只调用 `currentUserService.currentUserId()`，`AssessmentService.submitAnswerWithTraceId(...)` 在创建业务行前只校验 `userId == request.learnerId()`。因此本测试很可能 RED，并暴露真实生产授权缺口：course-bound `questionId` 未在 submit 写路径执行 active enrollment 校验。

## 验证结果

已运行：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
```

结果：RED。

- `Tests run: 49, Failures: 1, Errors: 0, Skipped: 0`
- 失败用例：`AssessmentControllerTest.submitAnswerRejectsBearerStudentForeignCourseQuestionIdWithoutSideEffects`
- 失败原因：期望 `403 FORBIDDEN`，实际 `200 OK`
- 失败位置：`AssessmentControllerTest.java:1414`

安全结论：当前 `POST /api/assessment/answers` 接受 Bearer `USER sub=alice` 提交 foreign course 的 `questionId`，即使请求头伪造为 `X-User-Id: teacher_b` 也会按 token subject `alice` 创建提交。这证明 submit 写路径缺少 `questionId -> KnowledgePoint.courseId -> ACTIVE enrollment` 授权校验。

## 升级判断

需要升级为 M：补 `AssessmentService` submit 写路径的 `questionId -> KnowledgePoint.courseId -> ACTIVE enrollment` 授权校验，并确认 legacy/template `q_sql_join` 兼容语义。当前 S 子任务仅保留 RED 测试和缺陷证据，不修改 production。
