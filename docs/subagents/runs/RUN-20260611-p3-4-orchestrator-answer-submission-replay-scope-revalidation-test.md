# RUN-20260611 P3-4 Orchestrator ANSWER_SUBMISSION Replay Scope Revalidation Test

## 结论

推荐最小 RED 测试放在：

- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`

核心测试名：

```java
answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow
```

该测试应先 RED：已有 `ANSWER_SUBMISSION` workflow 在 learner 的 course enrollment 从 `ACTIVE` 变为 `DROPPED` 后，当前实现可能直接返回旧 workflow，而不会重新执行课程范围校验。

## 推荐测试流程

1. seed 一个 course，teacher 为 `teacher_a`。
2. seed `alice` 对该 course 的 `ACTIVE` enrollment。
3. seed course-bound `KnowledgePoint`，生成 `questionId = "q_" + knowledgePoint.id.substring("kp_".length())`。
4. `alice` 首次调用 `POST /api/orchestrator/workflows`：
   - `workflowType=ANSWER_SUBMISSION`
   - `learnerId=alice`
   - payload 中使用上述 `questionId`
   - `requestId=req_orch_answer_replay_scope`
   - 预期成功，保存旧 `workflowId / agentTaskId / traceId`。
5. 将 `alice` 对该 course 的 enrollment 更新为 `DROPPED`。
6. 用相同 Bearer/headers、相同 `requestId` 和 payload 再次调用。
7. GREEN 后应断言：
   - HTTP 403。
   - `$.code == FORBIDDEN`。
   - `$.data` 不存在。
   - 响应不包含旧 `workflowId / agentTaskId / traceId`。
   - 响应不包含 `questionId`、`requestId`、raw answer、courseId。
   - answer/grading/mastery/wrong-question/learning-event 计数保持第一次成功后的数量，不新增。

## 辅助 fixture

`OrchestratorWorkflowControllerTest` 已有：

- `CourseRepository`
- `CourseEnrollmentRepository`
- assessment 业务表 repository

需要最小补充：

- `KnowledgePointRepository`
- `KnowledgePoint` import
- helper：`seedCourseKnowledgeAndEnrollment(...)`

## 验证命令

RED focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest#answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow test
```

GREEN focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest#answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest,AssessmentControllerTest,AssessmentServiceTest test
```

Full：

```powershell
cd D:\多元agent\backend
mvn test
```

## 验收重点

- 必须观察 RED 失败，且失败原因是 replay 返回旧 workflow 或未重新校验 dropped enrollment。
- 修复后 replay 路径和 fresh submit 路径的课程 scope 语义一致。
- 不改变 API/DTO/schema/dependency/frontend。
