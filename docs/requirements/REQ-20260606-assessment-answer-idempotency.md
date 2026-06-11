# 答题提交幂等需求

## 功能需求

1. `AnswerSubmitRequest` 必须包含 `requestId`，缺失或空白返回 400。
2. 首次提交时，系统必须保存 `requestId`、请求 payload hash 和响应快照。
3. 重复提交同一 `learnerId + requestId` 且 payload 一致时，系统返回首次提交响应。
4. 重复提交同一 `learnerId + requestId` 但 payload 不一致时，系统返回 `409 CONFLICT`。
5. 重放请求不得再次写入 `answer_record`、`grading_result`、`mastery_record`、`wrong_question`、`learning_event`。
6. 不同 learner 可以使用相同 `requestId`，互不影响。
7. 跨 learner 提交仍必须返回 403，且不能产生副作用。

## 数据需求

- `answer_record.request_id`
- `answer_record.request_hash`
- `answer_record.response_json`
- 唯一索引：`uk_answer_learner_request(learner_id, request_id)`

## 约束

- Controller 只做 HTTP 委托。
- Repository 访问只能在 Service 层。
- 不新增外部依赖。
- 新迁移保持 MySQL 兼容，并补迁移文本测试。
