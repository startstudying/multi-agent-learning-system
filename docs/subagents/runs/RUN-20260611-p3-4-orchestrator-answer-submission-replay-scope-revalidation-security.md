# RUN-20260611 P3-4 Orchestrator ANSWER_SUBMISSION Replay Scope Revalidation Security

## 结论

当前存在 replay scope revalidation 风险。

`ANSWER_SUBMISSION` workflow 在创建新 workflow 前会先调用 `AssessmentService.replayAnswerIfPresent(...)`。该 replay 入口只校验 `learnerId`、`requestId` 和 payload hash，未重新校验当前 `questionId -> KnowledgePoint.courseId -> ACTIVE enrollment`。因此学生在首次提交时有课程权限、之后 enrollment 变为 `DROPPED` 后，同 `requestId` / 同 payload 的 Orchestrator replay 可能直接返回旧 workflow。

## 风险说明

- 风险类型：权限重放绕过 / stale authorization replay。
- 风险级别：中。
- 影响：不会新增 answer/grading/mastery/wrong-question/event 业务行，但会在当前课程授权已失效后返回历史 `workflowId / agentTaskId / traceId / steps`。
- 安全语义冲突：直接 `submitAnswerWithTraceId(...)` 已重新校验 scope，Orchestrator replay precheck 与直接提交路径不一致。

## 关键证据

- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
  - `createWorkflow(...)` 对 `ANSWER_SUBMISSION` 调用 `assessmentService.replayAnswerIfPresent(ownerUserId, answerSubmitRequest)`，命中后直接 `replayAnswerSubmissionWorkflow(...)`。
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
  - `submitAnswerWithTraceId(...)` 已调用 `requireSubmitQuestionScope(...)`。
  - `replayAnswerIfPresent(...)` 未调用 `requireSubmitQuestionScope(...)`。
  - `requireSubmitQuestionScope(...)` 已存在，可复用现有课程/ACTIVE enrollment 语义。

## 推荐修复点

最小修复应放在：

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`

建议在 `replayAnswerIfPresent(...)` 中，`requireRequestId(...)` 后、计算 request hash 和查询历史 answer 前，调用：

```java
requireSubmitQuestionScope(request.learnerId(), request.questionId());
```

保持以下约束：

- 不引入 admin/teacher override。
- 不改 Orchestrator response DTO。
- 不改 DB schema、依赖、前端或认证框架。
- 不把权限判断下沉到 Controller。

## 必须覆盖的安全测试矩阵

1. 已存在 `ANSWER_SUBMISSION` workflow，learner enrollment 从 `ACTIVE` 变为 `DROPPED` 后，同 requestId replay 必须返回 `FORBIDDEN`，不能返回旧 workflow。
2. 拒绝响应不得包含旧 `workflowId / agentTaskId / traceId`、`questionId`、`requestId`、raw answer 或课程标题。
3. 拒绝 replay 不得新增 answer/grading/mastery/wrong-question/learning-event。
4. Bearer token 优先于 spoofed `X-User-Id`；`Bearer USER sub=admin` 不得获得 admin 语义。

## Size 建议

建议分类：S。

理由：

- 修复集中在一个服务方法。
- 不改变 REST API、DTO、DB schema、依赖、部署或前端。
- 测试集中在一个 Orchestrator MockMvc 测试类。
- 已经用专家 subagent 做并行安全/测试/架构分析，编码应由主 Codex 单线程集成，避免多个 worker 修改同一测试文件。

升级触发：

- 若需要改 Orchestrator workflow envelope、AgentTask schema、认证框架或跨 3 个以上生产模块，则升级为 M。
