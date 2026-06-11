# Agent 状态机收敛证据

## TDD RED

命令：

```bash
cd backend
mvn "-Dtest=ResourceGenerationControllerTest#persistsFailedAgentTaskWhenModelGenerationFails" test
```

结果：失败，符合预期。

关键失败：

```text
ResourceGenerationControllerTest.persistsFailedAgentTaskWhenModelGenerationFails
expected: "FAILED"
 but was: "RUNNING"
```

说明旧实现中，资源生成模型调用连续失败后，`agent_task` 已写入失败证据，但 `resource_generation_task` 仍停留在 `RUNNING`，会误导后台恢复任务和管理端看板。

## GREEN 验证

命令：

```bash
cd backend
mvn "-Dtest=ResourceGenerationControllerTest#persistsFailedAgentTaskWhenModelGenerationFails" test
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
mvn "-Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest,ReviewGovernanceServiceTest,ResourceReviewControllerTest,AiModelGatewayTest" test
```

结果：

```text
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
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
Tests run: 91, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 覆盖范围

- `AgentRunRecorder.startRun` 拒绝非法 task status。
- `AgentRunRecorder.recordTraceSteps` 拒绝非法 trace status。
- `recordFailure` 将 `agent_task.status` 写为 `FAILED`，追加失败 trace 和失败 model call。
- 失败 `outputJson` 包含 `recoverable: true`。
- 资源生成成功后，`agent_task` 与 `resource_generation_task` 进入 `WAITING_REVIEW`。
- 资源审核全部通过后，`agent_task` 与 `resource_generation_task` 进入 `DONE`。
- 可取消任务进入 `CANCELLED`，并追加取消 trace。
- 终态任务重复取消返回 `409 CONFLICT`。
- 跨用户取消返回 `403 FORBIDDEN`。
- 模型重试后仍失败时，`agent_task`、`agent_trace`、`model_call_log` 和 `resource_generation_task` 都保留失败证据。

## 代码审查处理

- 已采纳架构子代理关于统一状态入口的建议：状态集合、trace 状态集合、终态集合和可取消集合集中在 `AgentRuntimeConstants`。
- 已采纳失败事务风险处理：`ResourceGenerationService.createTask` 对 `ModelCallFailedException` 不回滚，并同步把业务任务标记为 `FAILED`。
- 已采纳审核闭环建议：资源全部审核通过后，关联 `AgentTask` 进入 `DONE`。

## 备注

- 当前取消是协作式状态取消，不中断已经执行完成或正在同步执行的模型 HTTP 调用。
- 后台恢复扫描、重试调度字段和真实异步 worker 中断仍属于 P0-3 后续任务。
- 测试输出包含 Mockito dynamic agent 的 JDK 未来兼容 warning；这是现有测试栈告警，不影响本轮验证结果。
