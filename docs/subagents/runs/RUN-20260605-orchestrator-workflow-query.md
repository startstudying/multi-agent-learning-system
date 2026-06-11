# Subagent Run - Orchestrator Workflow 查询与状态上下文收敛

## 决策

- Use Subagents：否，不启用额外并行 worker。
- Reason：当前会话是后端 worker，任务范围窄，文件归属集中；并行实现会增加冲突风险。
- Parallelism Level：L1 分析。
- Selected Expertise：Backend Expert、Agent/RAG Expert。
- Implementation Mode：Single backend worker。

## 后端分析

- Controller 应只暴露 `POST` 与 `GET` 映射，当前用户由 `CurrentUserService` 获取。
- Service 负责从 `agent_task` 与 `agent_trace` 聚合 workflow 状态上下文。
- 缺失 workflow 应抛 `ApiException(ErrorCode.NOT_FOUND, "Workflow not found")`。

## Agent/Trace 分析

- 创建 workflow 已写入 `agent_task` 和 `workflow_start` trace，可作为查询上下文来源。
- 查询接口不新增 trace 记录，避免读操作污染执行轨迹。
- 最近失败步骤从已存在 trace 中筛选最后一个 `FAILED` 状态。

## 集成结论

采用最小实现：不新增表，不改执行链路，只为 `AgentTaskRepository` 增加 owner + inputJson marker 查询方法，并在 Orchestrator Service 中组装 response。

