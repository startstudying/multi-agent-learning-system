# Agent 状态机收敛需求

## 功能需求

1. Agent Task 只允许写入：`PENDING`、`RUNNING`、`WAITING_REVIEW`、`DONE`、`FAILED`、`CANCELLED`。
2. Agent Trace 只允许写入：`PENDING`、`RUNNING`、`WAITING`、`WAITING_REVIEW`、`DONE`、`FAILED`、`CANCELLED`。
3. `startRun` 只能从 `PENDING` 或 `RUNNING` 创建任务。
4. `recordTraceSteps` 必须校验每个 Trace step 状态。
5. `recordFailure` 必须把 task 转为 `FAILED`，写入结构化 output JSON，并追加失败 trace 和失败 model call。
6. 增加取消能力：`RUNNING`、`PENDING`、`WAITING_REVIEW` 可取消为 `CANCELLED`；`DONE`、`FAILED`、`CANCELLED` 不可取消。
7. 取消必须追加一条 `CANCELLED` trace，说明取消原因。
8. 非法状态或非法流转必须返回业务异常，不允许悄悄写入数据库。

## 数据需求

本轮不新增字段，不新增迁移。

复用：

- `agent_task.status`
- `agent_task.output_json`
- `agent_trace.status`
- `agent_trace.summary`
- `model_call_log.status`

## 约束

- 状态治理在 Service 层完成。
- Controller 只做 HTTP 委托。
- 不新增依赖。
- 不改变现有查询响应 DTO。
