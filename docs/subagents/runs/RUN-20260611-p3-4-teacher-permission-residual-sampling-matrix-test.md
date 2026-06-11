# RUN-20260611-p3-4-teacher-permission-residual-sampling-matrix-test

## 角色

Test Expert，只读测试策略分析。

## 结论

当前 P3-4 已覆盖大量直接 Controller RBAC、spoofed header、防枚举和无副作用场景。继续补全时，应优先选择 wrapper / replay / aggregation 交界处，而不是重复枚举已经稳定的直接接口路径。

本次 S 级子任务采纳了其中一个低风险、高价值抽样点：

- `AnalyticsControllerTest.teacherClassSummaryPendingReviewsRedactsForeignCourseReviews`

该用例验证教师 class summary 的 `pendingReviews` 聚合只包含请求课程内 review，不混入 foreign course review/resource/task/title，且不暴露 `markdownContent`。

## 推荐但未纳入本 S 子任务的后续风险点

测试专家认为下一项最高价值 P3-4 子任务是：

```text
P3-4 子任务：orchestrator answer submission replay scope revalidation
```

原因：

- `OrchestratorWorkflowService` 的 `ANSWER_SUBMISSION` replay precheck 会调用 `assessmentService.replayAnswerIfPresent(...)`。
- 该 replay 风险点在于可能只校验 learner/request hash，而没有重新校验 `questionId -> course -> active enrollment` scope。
- 直接 `POST /api/assessment/answers` 路径已经有 `requireSubmitQuestionScope(...)`，但 Orchestrator wrapper/replay 顺序仍值得单独验证。

建议后续 RED 测试：

1. foreign course `questionId` workflow submit 拒绝，且无 assessment side effects。
2. 首次提交后 learner enrollment 变为 `DROPPED`，同 requestId replay 不应直接返回旧 workflow。
3. Bearer `USER sub=admin` role-confusion 不得绕过 enrollment。

## 本次边界决策

- 不把 Orchestrator `ANSWER_SUBMISSION` replay/scope revalidation 塞入当前 S 子任务。
- 当前子任务保持 test-only，仅覆盖 KB list、foreign document reindex、class summary pendingReviews 三个教师端残余抽样点。
- 若后续 Orchestrator replay 用例 RED，建议按新的 P3-4 语义子任务处理，并根据生产代码影响重新判定 S/M。

未修改文件，未运行测试，未使用 `node_repl`。
