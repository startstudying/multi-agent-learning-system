# RUN-20260608 Assessment Record RBAC Security Review

## 1. 结论

P3-4-E 风险级别：MEDIUM。

当前 assessment 没有公开答题记录读接口，直接泄露面有限；但新增详情接口前必须先定义对象级授权，否则会形成 answerId / wrongQuestionId 的 IDOR 风险。

## 2. 现状证据

- `AssessmentController` 公开 `POST /api/assessment/answers` 与 `POST /api/assessment/grading-evaluations`。
- `AssessmentService.submitAnswerWithTraceId(...)` 已强制 `currentUserId == learnerId`。
- `AnswerRecord` / `GradingResult` / `WrongQuestion` / `MasteryRecord` / `LearningEvent` 均持久化 learner 相关学习数据。
- `AnswerRecord` 与 `GradingResult` 不含 `courseId`，教师授权需通过 `questionId -> knowledgePointId -> courseId` 推导。
- `KnowledgePoint.courseId` 可作为现阶段课程父级。

## 3. 推荐切片

- 新增 `GET /api/assessment/answers/{answerId}`。
- 新增 `GET /api/assessment/wrong-questions/{wrongQuestionId}`。
- 不做 list。
- 不做 migration。
- 非 admin missing / foreign 统一 `FORBIDDEN`。
- admin missing 返回 `NOT_FOUND`。

## 4. 测试重点

1. student foreign answer detail -> `FORBIDDEN` no `data`。
2. student missing answer detail -> `FORBIDDEN` no `data`。
3. teacher own-course active enrollment learner answer detail -> 200。
4. teacher foreign-course answer detail -> `FORBIDDEN`。
5. admin any answer -> 200；admin missing -> `NOT_FOUND`。
6. wrong-question detail 复用相同授权语义。
7. 响应不包含 `requestHash`、`responseJson`、`payloadJson`。

## 5. 保留 open

- 真实 JWT/RBAC。
- assessment list / pagination。
- course-scoped grading evaluation。
- assessment 记录 `courseId` schema 归一化。
