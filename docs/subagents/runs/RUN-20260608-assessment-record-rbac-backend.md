# RUN-20260608 Assessment Record RBAC Backend Analysis

## 1. 结论

P3-4-E 可作为单模块后端切片实现，不需要新增 schema 或依赖。

推荐落点：

- Controller：`AssessmentController`
- Service：`AssessmentService`
- DTO：新增 answer / wrong-question detail 白名单响应
- Repository：补最新 grading / wrong-question 查询方法
- Test：`AssessmentControllerTest`

## 2. 实现建议

1. 新增 `GET /api/assessment/answers/{answerId}`。
2. 新增 `GET /api/assessment/wrong-questions/{wrongQuestionId}`。
3. `AssessmentController` 只读取 `currentUserId` 和 path variable。
4. `AssessmentService` 在返回 DTO 前执行授权：
   - `admin`：全局读取，missing 返回 `NOT_FOUND`。
   - `learnerId == currentUserId`：允许读取。
   - `teacher / teacher_*`：必须能通过课程父级 scope，且 learner 在该课程 active enrollment 中。
5. `WrongQuestion` 已有 `knowledgePointId`，优先使用它推导 course；answer 详情通过 `questionId -> AssessmentFeedbackService.resolveKnowledgePointId(...)` 推导。

## 3. 风险

| Risk | Mitigation |
|---|---|
| answer 记录没有 `courseId` | 本切片通过 knowledge point 推导；后续列表接口再考虑 schema 或 scoped query 优化 |
| DTO 泄露 idempotency 快照 | 使用专用白名单 DTO，测试断言敏感字段不存在 |
| 非 admin 对象枚举 | missing / foreign 统一 `FORBIDDEN` 且无 `data` |
| 扩大接口面 | 只做详情，不做 list / pagination |

## 4. 推荐测试

- student foreign answer detail -> `FORBIDDEN` no `data`。
- student missing answer detail -> `FORBIDDEN` no `data`。
- teacher own-course active enrollment learner answer detail -> 200。
- teacher foreign-course answer detail -> `FORBIDDEN`。
- admin any answer -> 200；admin missing -> `NOT_FOUND`。
- wrong-question detail 复用同一授权语义。
- DTO 不包含 `requestId`、`requestHash`、`responseJson`、`payloadJson`。
