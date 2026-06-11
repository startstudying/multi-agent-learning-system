# 答题提交幂等验收

## 验收结论

状态：通过。

## 验收项

| 验收项 | 结果 | 说明 |
|---|---|---|
| `requestId` 必填 | PASS | 缺失字段返回 `VALIDATION_ERROR` 400 |
| 相同 payload 重放 | PASS | 第二次返回首次 `answerId`、`gradingResultId`、`traceId`、`replanRecordId` |
| 重放无重复副作用 | PASS | `answer_record`、`grading_result`、`mastery_record`、`wrong_question`、`learning_event` 计数不增加 |
| 不同 payload 冲突 | PASS | 同 learner 同 requestId 不同答案返回 `CONFLICT` 409 |
| learner 作用域隔离 | PASS | `alice` 和 `bob` 可使用同一 `requestId`，各自产生记录 |
| 跨 learner 权限 | PASS | `X-User-Id` 与 `learnerId` 不一致返回 403 |
| 并发唯一键冲突 | PASS | 模拟 `uk_answer_learner_request` 冲突后重新读取胜出记录并重放 |
| 数据库迁移 | PASS | V5 增加 request 字段和唯一索引，并有迁移文本测试 |
| 架构分层 | PASS | Controller 未增加业务逻辑；幂等逻辑在 Service 层；无新增依赖 |
| 全量测试 | PASS | `mvn test`：86 tests, 0 failures |

## 限制

- 当前未引入独立幂等请求表，也未实现 `PROCESSING/COMPLETED/FAILED` 状态机；若后续答题批改接入真实长耗时模型，建议升级为独立状态表。
- V5 迁移尚未在真实 MySQL 8 上 smoke test；当前只通过 H2 JPA 测试和迁移文本断言。
- 同一 learner 不同 `requestId` 并发提交同一知识点仍可能竞争最新 mastery，属于后续学习状态一致性优化。
