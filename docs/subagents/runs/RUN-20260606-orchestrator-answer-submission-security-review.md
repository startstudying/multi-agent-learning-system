# Orchestrator ANSWER_SUBMISSION 安全与审计评审

## Summary

风险等级：`MEDIUM`。

本切片必须避免 `ANSWER_SUBMISSION` 被 Orchestrator 接受但不执行，造成永久 `RUNNING` 空审计链。最小实现可以不新增数据库 migration，但要明确 workflow-level replay、traceId 注入和权限边界。

## Must Avoid

1. **空 workflow**
   - `ANSWER_SUBMISSION` 已是合法 workflow type。
   - 若没有执行分支，会创建 `agent_task` 和 `workflow_start` 后停在 `RUNNING`。

2. **traceId 漂移**
   - Orchestrator 调用 assessment 时不能依赖随机 trace 或新的 `TraceContext`。
   - Assessment 需要显式接收 Orchestrator `traceId`。

3. **重复 workflow**
   - Assessment 已有 `(learnerId, requestId)` 幂等。
   - Orchestrator 也要在创建新 task 前 replay 首次 workflow，否则会出现多个 workflow 指向同一业务结果。

4. **误报权限完成**
   - 本轮只维持 `learnerId == currentUserId` 的最小权限。
   - `questionId` 到课程/学习路径的细粒度授权仍属于 P3 权限加固。

5. **审计数据过度保留**
   - 当前 workflow envelope 会保存 `payloadJson`，其中包含学生答案。
   - 本轮保持现状以降低范围，但后续应考虑 envelope 脱敏和 retention。

## Minimal Strategy

- 新增 `AssessmentService.submitAnswerWithTraceId(...)`。
- 新增 assessment replay preflight，供 Orchestrator 在创建 task 前判断 replay/conflict。
- `ANSWER_SUBMISSION` 成功或 replay 都返回 `DONE` 的可查询 workflow。
- 不新增 DB migration。
- 不新增依赖。

## Deferred Risks

- P0-3：workflow 级唯一键、响应快照、scheduler recovery。
- P0-3：失败审计独立事务，避免业务异常回滚 evidence。
- P3：题目/课程/路径级权限校验。
- P3：trace retention、答案脱敏、审计查询权限隔离。

