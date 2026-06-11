# Agent 状态机收敛验收

## 验收结论

状态：通过。

## 验收项

| 验收项 | 结果 | 说明 |
|---|---|---|
| task 状态白名单 | PASS | `startRun` 只允许 `PENDING` 或 `RUNNING` 初始状态，非法状态返回业务异常 |
| trace 状态白名单 | PASS | `recordTraceSteps` 写入前校验每个 step 状态 |
| 状态流转约束 | PASS | `PENDING/RUNNING/WAITING_REVIEW` 按规格流转，终态不允许继续流转 |
| 失败任务记录 | PASS | `recordFailure` 写入 `FAILED` task、失败 trace、失败 model call |
| 失败可恢复标记 | PASS | 失败 `outputJson` 包含 `recoverable: true` |
| 资源生成失败状态 | PASS | 模型重试后失败时，`resource_generation_task.status = FAILED`，不再停留在 `RUNNING` |
| 成功后等待审核 | PASS | 资源生成草稿完成后进入 `WAITING_REVIEW`，不直接发布给学生 |
| 审核通过后完成 | PASS | 全部资源审核通过后，业务任务和 Agent task 都进入 `DONE` |
| 取消能力 | PASS | 可运行任务可取消为 `CANCELLED` 并追加取消 trace |
| 重复取消 | PASS | `DONE`、`FAILED`、`CANCELLED` 终态任务再次取消返回 409 |
| 权限检查 | PASS | 非 owner 取消或查看 trace 返回 403 |
| 架构分层 | PASS | 状态治理在 Service/Recorder 层；Controller 只做 HTTP 委托；无新增依赖 |
| 聚焦测试 | PASS | `mvn "-Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest,ReviewGovernanceServiceTest,ResourceReviewControllerTest,AiModelGatewayTest" test`：23 tests, 0 failures |
| 全量测试 | PASS | `mvn test`：91 tests, 0 failures |

## 限制

- 当前取消能力是持久化状态取消，不负责中断已经进入同步执行的模型调用。
- 数据库状态字段仍是字符串，历史脏状态需要后续数据治理或迁移脚本处理。
- 长任务 `retry_count`、`next_retry_at`、`last_error`、后台恢复扫描仍在 P0-3。
- 真实 MySQL migration smoke 未在本轮覆盖，仍保留在 P3-1。
