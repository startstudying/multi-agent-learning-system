# Orchestrator 失败与重试策略 Retrospective

## 做得有效

- 失败证据继续落在 `agent_task` 和 `agent_trace`，没有引入新的 workflow 表。
- retry endpoint 只开放可从 envelope 安全还原 payload 的 `RESOURCE_GENERATION`，避免为了 retry 保存 RAG 问题或学生答案原文。
- 测试覆盖 owner、非 owner、FAILED、非 FAILED 和敏感信息脱敏路径。

## 需要注意

- 当前 retry 是最小同步能力，不是完整后台恢复系统。
- `retryOfWorkflowId` 已进入 response/envelope，后续前端和审计页面可以据此展示重试链路。
- `RAG_QA` / `ANSWER_SUBMISSION` 的 retry 需求要先定隐私策略，再决定是否支持。

## 后续建议

- 在 P0/P3 中补 `retry_count`、`next_retry_at`、退避策略和后台恢复队列。
- 将 workflow 节点级失败策略整理成结构化策略表，便于前端展示和论文说明。
