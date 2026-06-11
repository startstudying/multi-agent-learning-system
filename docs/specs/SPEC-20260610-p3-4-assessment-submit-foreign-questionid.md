# SPEC-20260610-p3-4-assessment-submit-foreign-questionid

## 1. 设计摘要

在 `AssessmentService.submitAnswerWithTraceId(...)` 中增加服务层授权前置检查：

```text
AnswerSubmitRequest.questionId
-> AssessmentFeedbackService.resolveKnowledgePointId(questionId)
-> KnowledgePointRepository.findById(knowledgePointId)
-> KnowledgePoint.courseId
-> CourseAccessService.requireCourseRead(learnerId, false, false, courseId)
```

如果知识点存在且绑定课程，则提交 learner 必须能以学生身份读取该课程，也就是具备 ACTIVE enrollment。校验必须发生在 idempotency replay 和任何持久化副作用之前，避免旧响应 replay 或新提交绕过权限。

## 2. 授权语义

| 场景 | 结果 |
|---|---|
| `questionId` 可解析到现有 `KnowledgePoint`，learner 已 ACTIVE enrollment | 允许提交 |
| `questionId` 可解析到现有 `KnowledgePoint`，learner 未 enrollment / DROPPED / foreign course | `FORBIDDEN` |
| `questionId` 可解析到现有 `KnowledgePoint`，course 缺失或不可读 | 非 admin 语义下 `FORBIDDEN` |
| `questionId` 无对应 `KnowledgePoint` | 保留 legacy/template 兼容，不在本切片拒绝 |

说明：submit path 当前只支持 `currentUserId == request.learnerId()`，不引入 admin 代提交语义；因此课程校验固定按 student enrollment 语义执行。

## 3. 副作用边界

授权检查必须早于：

- `answerRecordRepository.findByLearnerIdAndRequestId(...)` replay
- `contentSafetyService.checkUserInput(...)`
- `transactionTemplate.execute(...)`
- `AnswerRecord` / `GradingResult` / `MasteryRecord` / `WrongQuestion` / `LearningEvent` 写入

## 4. API / DB / 依赖

- API：无变更。
- DTO：无变更。
- DB schema：无变更。
- Dependency：无新增。
- Frontend：无变更。

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 仍只委托 Service；授权放在 `AssessmentService`。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime；Orchestrator 继续调用同一 Service。 |
| Security | PASS | 权限检查在后端代码中，且早于持久化副作用。 |
| API / Database | PASS | 不改 API/DTO/schema。 |

## 6. 风险

- 现有 legacy/template `questionId` 没有题库实体，不能在本切片强制拒绝，否则会破坏现有答题闭环测试。
- 后续若建立正式 assessment question 表，应将本切片的兼容逻辑收口为“所有生产题目必须有可授权父资源”。
