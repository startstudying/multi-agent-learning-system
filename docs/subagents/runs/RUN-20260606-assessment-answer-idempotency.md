# Subagent Run: 答题提交幂等

## Subagent Decision

Use Subagents: Yes

Reason: 本任务影响后端 API、数据库迁移、事务一致性、安全边界和测试，符合项目中“涉及权限、数据库、安全必须启用专家”的规则。

Parallelism Level: L1

Selected Subagents:

- Backend Architect：设计最小后端幂等方案。
- Security Reviewer：审查权限、payload 冲突、迁移和测试风险。

Implementation Mode: Single Codex implementation with parallel analysis.

## 集成结论

两个子代理结论一致：使用显式 `requestId`，作用域为 `(learner_id, request_id)`，落在 `answer_record` 这个业务根表；同 requestId 不同 payload 必须 409；数据库唯一索引必须作为兜底。

## 采纳项

- `AnswerSubmitRequest` 增加必填 `requestId`。
- `answer_record` 增加 `request_id`、`request_hash`、`response_json`。
- 唯一索引 `uk_answer_learner_request(learner_id, request_id)`。
- 重放时返回首次响应快照。
- payload hash 不一致返回 `CONFLICT`。

## 暂不采纳项

- 暂不新增独立 `assessment_submission_request` 表。
- 暂不实现完整并发 `PROCESSING/SUCCEEDED/FAILED` 状态机。
- 暂不引入 Testcontainers MySQL。

## Code Review 处理

- 采纳 HIGH：`AssessmentService` 现在先 `saveAndFlush` 幂等根；并发唯一键冲突会重新读取 `(learnerId, requestId)` 胜出记录，相同 payload 重放首次响应，不同 payload 走 409，不再向接口暴露 500。
- 采纳 MEDIUM：`requestHash` 已调整为 `sha256(learnerId + questionId + answer)`，与 SPEC 一致；`requestId` 只用于幂等查找作用域。
- 记录 LOW：V5 目前仍是迁移文本测试，真实 MySQL Flyway smoke 属于 P3-1，未在本轮引入 Testcontainers 或 Docker 验证。
