# 答题提交幂等 PRD

## 背景

当前答题提交会写入 `answer_record`、`grading_result`、`mastery_record`、`wrong_question`、`learning_event`，并触发学习路径重规划。上一轮已将掌握度更新改为读取最新 `mastery_record`，因此同一答案重复提交不再只是多一条日志，而会继续改变学习者掌握度和路径状态。

## 目标

- `POST /api/assessment/answers` 支持必填 `requestId`。
- 同一 `learnerId + requestId + payload` 重放时返回第一次提交结果，不重复写业务表。
- 同一 `learnerId + requestId` 但 `questionId` 或 `answer` 不一致时返回 `409 CONFLICT`。
- `requestId` 幂等作用域按 learner 隔离，不跨学生命中。
- 数据库层提供 `(learner_id, request_id)` 唯一约束。

## 非目标

- 不新增独立幂等请求表。
- 不实现并发下完整 `PROCESSING/SUCCEEDED/FAILED` 状态机。
- 不解决不同 `requestId` 并发更新同一知识点的串行化问题。
- 不修改前端页面。

## 用户价值

- 学生端重复点击或网络重试不会重复扣改掌握度。
- 学习分析和错题本不会被重复请求污染。
- 后续 Agent workflow 可以把答题提交纳入可恢复、可重放的学习闭环。
