# Agent 状态机收敛任务

## 任务清单

- [x] 读取项目记忆、架构基线、Agent 代码和测试。
- [x] 创建本轮 workflow 文档。
- [x] 吸收架构子代理审查结论：优先在 `AgentRunRecorder` 统一写入点收敛状态，并修复失败记录可能被事务回滚的问题。
- [x] 写失败测试覆盖状态白名单、失败输出、取消，以及资源生成模型失败后业务任务不能停留在 `RUNNING`。
- [x] 实现状态机校验、结构化失败输出、失败持久化和取消能力。
- [x] 暴露 `POST /api/agent/tasks/{taskId}/cancel` 取消接口。
- [x] 跑聚焦测试。
- [x] 跑全量后端测试。
- [x] 更新 evidence、acceptance、memory、changelog。

## Done Criteria

- 非法 task/trace 状态不能写入。
- `recordFailure` 输出结构化失败 JSON，包含 `recoverable`。
- 可运行任务可取消并追加 `CANCELLED` trace。
- `DONE`、`FAILED`、`CANCELLED` 再取消返回 409。
- 资源生成模型失败后，`agent_task`、`agent_trace`、`model_call_log`、`resource_generation_task` 都保留失败状态或失败证据。
- `mvn test` 通过或记录失败原因。
