# ACCEPT-20260610 P3-4 子任务：Assessment submit foreign-questionId

## Acceptance Summary

Status: ACCEPTED.

`POST /api/assessment/answers` 的 foreign course `questionId` 提交缺陷已修复。答题提交现在会在 idempotency replay 和任何持久化副作用之前，对可解析到现有 `KnowledgePoint` 的题目执行 `KnowledgePoint.courseId -> CourseAccessService.requireCourseRead(learnerId, false, false, courseId)` 校验。未绑定正式知识点的 legacy/template question id 兼容路径保留。

## Acceptance Criteria

| Criteria | Status | Evidence |
|---|---|---|
| RED 测试证明学生可提交 foreign course `questionId` 的生产缺陷 | PASS | `AssessmentControllerTest.submitAnswerRejectsBearerStudentForeignCourseQuestionIdWithoutSideEffects` 初始 RED：期望 `403`，实际 `200`。 |
| `questionId -> KnowledgePoint -> course -> ACTIVE enrollment` 校验在副作用前执行 | PASS | `AssessmentService.submitAnswerWithTraceId(...)` 在 replay / content safety / transaction 前调用 `requireSubmitQuestionScope(...)`。 |
| foreign course `questionId` 提交返回安全 `FORBIDDEN` | PASS | Focused `AssessmentControllerTest,AssessmentServiceTest` passed。 |
| 越权响应无 `data` 且不泄露 foreign question/course/knowledge/request 信号 | PASS | 新增 MockMvc 断言覆盖。 |
| 越权请求不新增 answer/grading/mastery/wrong-question/event | PASS | 新增 repository count 断言覆盖。 |
| legacy/template `q_sql_join` 兼容路径保持通过 | PASS | Focused assessment tests passed。 |
| Orchestrator `ANSWER_SUBMISSION` 相邻路径未回归 | PASS | Adjacent `AssessmentControllerTest,AssessmentServiceTest,OrchestratorWorkflowControllerTest` passed。 |
| Evidence / Changelog / Memory / TODO 更新 | PASS | 本文件、Evidence、Changelog、Project/Backend/RAG memory、planning TODO 已更新。 |

## Verification

- RED: `mvn --% -Dtest=AssessmentControllerTest test` -> `49 run, 1 failure`，新增测试证明缺陷。
- Focused: `mvn --% -Dtest=AssessmentControllerTest,AssessmentServiceTest test` -> `60 run, 0 failures, 0 errors`.
- Adjacent: `mvn --% -Dtest=AssessmentControllerTest,AssessmentServiceTest,OrchestratorWorkflowControllerTest test` -> `92 run, 0 failures, 0 errors`.
- Full backend: `mvn test` -> `578 run, 0 failures, 0 errors, 1 skipped`.

## Accepted Limitations / Follow-up

- P3-4 父项不关闭。
- 未新增正式 assessment question 表；正式题库模型如后续落地，应将 legacy/template 兼容路径继续收口。
- 本切片不改变 admin/teacher 代提交语义；当前 submit path 仍要求 `currentUserId == learnerId`。
