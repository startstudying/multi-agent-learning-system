# 答题提交幂等规格

## API

### `POST /api/assessment/answers`

请求新增必填字段：

```json
{
  "learnerId": "alice",
  "questionId": "q_sql_join",
  "answer": "JOIN duplicates happen when a one-to-many relation is joined.",
  "requestId": "req_answer_once"
}
```

响应字段保持不变。重放时返回第一次提交的完整响应快照。

## 幂等规则

1. 先校验 `X-User-Id` 与 `learnerId` 一致。
2. 规范化 `requestId = trim(requestId)`。
3. 计算 `requestHash = sha256(learnerId + questionId + answer)`。
4. 按 `learnerId + requestId` 查询 `answer_record`。
5. 如果存在：
   - hash 一致：返回 `response_json`。
   - hash 不一致：抛出 `CONFLICT`。
6. 如果不存在：先创建并 flush 新的 `answer_record` 幂等根，随后写入 grading、mastery、wrong question、learning event，并把响应快照写回 `answer_record.response_json`。
7. 如果创建幂等根时命中数据库唯一约束，说明出现并发重复请求；服务层重新按 `learnerId + requestId` 读取胜出记录，并复用第 5 步的 hash/replay/409 逻辑，避免暴露 500。

## 数据库

新增迁移：`V5__assessment_answer_idempotency.sql`

```sql
ALTER TABLE answer_record ADD COLUMN request_id varchar(120);
ALTER TABLE answer_record ADD COLUMN request_hash varchar(128);
ALTER TABLE answer_record ADD COLUMN response_json text;
CREATE UNIQUE INDEX uk_answer_learner_request ON answer_record(learner_id, request_id);
```

迁移使用现有 V4 的 `add_column_if_missing` / `add_index_if_missing` 模式。

## 错误处理

- 缺失 `requestId`：validation 400。
- 同 learner 同 requestId 不同 payload：`409 CONFLICT`。
- 跨 learner：`403 FORBIDDEN`。
- 并发同 learner 同 requestId 相同 payload：返回胜出请求的响应快照。

## 验收标准

- 相同 requestId 和相同 payload 提交两次，只产生一组业务记录。
- 第二次响应中的 `answerId`、`gradingResultId`、`traceId`、`replanRecordId` 与第一次一致。
- 相同 requestId 不同 payload 返回 409，且计数不变。
- `alice` 和 `bob` 可以使用同一个 requestId，各自生成自己的记录。
- 迁移文本包含 request 字段和唯一索引。
- 模拟唯一键冲突时不会冒泡为 `INTERNAL_ERROR`。
