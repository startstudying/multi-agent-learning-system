# Subagent Run - RAG 索引 Worker / Progress 安全质量审查

## 1. 角色

- 角色：Security & Quality Expert
- 范围：P3-2 RAG 索引 worker、progress、heartbeat、retry/requeue、task detail API
- 模式：只读分析，不修改文件

## 2. Findings

| Severity | Finding | 说明 |
|---|---|---|
| High | worker execution is not concurrency-safe | 当前 `processIndexTask(String)` 没有 atomic claim / lease，多个 worker 可能处理同一任务。 |
| High | raw exception messages are persisted and exposed | 当前 `safeError(...)` 只截断异常消息；`DocumentStatusResponse.errorMessage` 会向有读权限用户返回。 |
| Medium | timeout recovery uses `updatedAt` as implicit heartbeat | 长时间解析/切块可能被误判为超时。 |
| Medium | auto retry/requeue lacks bounded policy | 没有 `maxRetryCount`、`nextRetryAt`、backoff 或非重试终态分类。 |
| Medium | task detail API must not be implemented by task id alone | 需要 KB read 权限校验，且避免未授权枚举。 |
| Medium | progress model is missing | 当前 `kb_index_task` 无 progress、stage、heartbeat、lease、next retry 字段。 |
| Low | tests cover foundation behavior, not production worker behavior | 需要补并发 claim、heartbeat、retry exhaustion、IDOR、错误脱敏测试。 |

## 3. 必须满足的安全质量验收

- worker 必须原子抢占任务；两个 worker 不能处理同一个 `PENDING` task。
- task detail API 必须先执行 KB read 权限检查。
- detail/document 响应不得暴露原始异常、storage key、对象路径、文档文本或 provider 错误。
- `heartbeatAt` / `leaseUntil` 必须替代 `updatedAt` 成为恢复判断依据。
- retry 必须有最大次数、backoff、`nextRetryAt` 和最终失败状态。

## 4. 依赖与秘密扫描

- RAG 范围未发现硬编码生产密钥。
- 不建议本切片新增依赖。

