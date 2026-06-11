# PRD - Orchestrator Workflow 查询与状态上下文收敛

## 1. 问题陈述

当前 `POST /api/orchestrator/workflows` 已能创建统一工作流入口，并写入 `agent_task` 与 `agent_trace`。但创建响应只返回基础标识，缺少可继续动作和步骤上下文；同时没有按 `workflowId` 查询工作流状态的 API，调用方无法用统一入口追踪学习闭环链路。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 学生端/教师端前端 | 工作流调用方 | 创建后能拿到 `workflowId`，并通过查询接口展示当前状态与下一步动作 |
| 管理员/运维 | Trace 观察者 | 失败时能定位最近失败步骤和 trace 摘要 |
| 后端服务 | 编排层 | 将 Agent 任务和 trace 统一收敛为 workflow 查询上下文 |

## 3. 用户故事

- 作为前端调用方，我希望创建 workflow 后立即拿到 `workflowId`、`agentTaskId`、`traceId`、`status`、`steps`、`nextActions`，以便进入轮询或下一步 UI。
- 作为管理员，我希望通过 `GET /api/orchestrator/workflows/{workflowId}` 查询当前状态、最近失败步骤和 trace 摘要，以便排查失败链路。

## 4. MVP 范围

### 纳入范围

- 扩展创建 workflow 响应，补充 `steps`、`traceSummary`、`recentFailedStep`、`nextActions`。
- 新增 `GET /api/orchestrator/workflows/{workflowId}`。
- 不存在或不属于当前用户的 `workflowId` 返回统一 `NOT_FOUND` envelope。
- Controller 只负责 HTTP 映射，查询聚合逻辑放在 Service。
- 更新 `OrchestratorWorkflowControllerTest` 覆盖 create 后 get 和 missing workflow。

### 非目标

- 不新增 workflow 表。
- 不实现完整工作流状态机推进、重试、取消。
- 不改资源生成、RAG、测评等下游业务流程。
- 不新增依赖或数据库迁移。

## 5. 成功指标

| 指标 | 目标值 | 衡量方式 |
|---|---|---|
| create 后可查询 | 100% | 控制器测试通过 |
| missing workflow 统一错误 | 404 + `code=NOT_FOUND` | 控制器测试通过 |
| 架构边界 | 无漂移 | Controller 委托 Service，无新增依赖 |

## 6. 用户流程

```text
调用方 POST 创建 workflow
-> 响应获取 workflowId、agentTaskId、traceId、steps、nextActions
-> 调用方 GET /api/orchestrator/workflows/{workflowId}
-> 后端聚合 agent_task 与 agent_trace
-> 返回当前状态、最近失败步骤、trace 摘要、可继续动作
```

## 7. 依赖关系

- 依赖：`agent_task.inputJson` 中已有 workflow envelope，`agent_trace` 中已有按 `agentTaskId` 排序的步骤。
- 阻塞：无。

## 8. 待澄清问题

| 问题 | 负责人 | 状态 |
|---|---|---|
| 是否后续引入独立 workflow 表 | 后续 P0 任务 | 暂不处理 |

