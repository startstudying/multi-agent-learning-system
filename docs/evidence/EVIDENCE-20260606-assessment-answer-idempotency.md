# 答题提交幂等证据

## TDD RED

命令：

```bash
cd backend
mvn "-Dtest=AssessmentServiceTest" test
```

结果：失败，符合预期。

关键失败：

```text
AssessmentServiceTest.replaysExistingResponseWhenConcurrentInsertHitsIdempotencyConstraint
org.springframework.dao.DataIntegrityViolationException: uk_answer_learner_request
```

说明旧实现遇到并发同 `learnerId + requestId` 唯一键冲突时，会把数据库异常冒泡，无法按幂等契约重放。

## GREEN 验证

命令：

```bash
cd backend
mvn "-Dtest=AssessmentServiceTest" test
```

结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 聚焦回归

命令：

```bash
cd backend
mvn "-Dtest=AssessmentServiceTest,AssessmentControllerTest,AssessmentFeedbackServiceTest,SchemaConvergenceMigrationTest" test
```

结果：

```text
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 全量后端验证

命令：

```bash
cd backend
mvn test
```

结果：

```text
Tests run: 86, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 覆盖范围

- 缺失 `requestId` 返回 validation 400。
- 同一 learner、同一 `requestId`、相同 payload 返回首次响应快照，业务表计数不增加。
- 同一 learner、同一 `requestId`、不同 payload 返回 409。
- 不同 learner 可复用同一 `requestId`，互不影响。
- 跨 learner 提交仍返回 403。
- V5 迁移文本包含 `request_id`、`request_hash`、`response_json` 和 `uk_answer_learner_request`。
- 模拟唯一键冲突时，服务层重新读取胜出记录并重放，不返回 500。

## 代码审查处理

- 已采纳并发唯一键冲突处理：用 `TransactionTemplate` 包住写入事务，冲突后重新读取胜出记录。
- 已采纳 hash 合同一致性处理：`requestHash` 只覆盖 `learnerId`、`questionId`、`answer`。
- 暂未完成真实 MySQL migration smoke；该项保留在 P3-1。

## 备注

测试输出包含 Mockito dynamic agent 的 JDK 未来兼容 warning，不影响本轮测试结果。
