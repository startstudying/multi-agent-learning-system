# Subagent Run: Agent 状态机收敛

## Subagent Decision

Use Subagents: Yes

Reason: 本任务涉及 Agent/RAG 运行时治理、状态机、API 和测试，符合项目中“涉及 Agent/RAG 必须启用专家”的规则。

Parallelism Level: L1

Selected Subagents:

- Agent Runtime Architect：只读分析当前 Agent 状态写入、状态流转风险和最小改造方案。

Implementation Mode: Single Codex implementation with parallel analysis.

## 初始集成结论

先不做数据库 enum 和异步队列，优先在 `AgentRunRecorder` 这个统一写入点收敛状态集合和流转。取消能力先面向持久化任务状态和 trace 证据，后续异步执行器接入时再中断实际 worker。

## 只读审查结论

状态：WATCH，当前代码已有状态常量和 trace 表，但原始实现不是完整状态机。

关键建议：

- 状态集合和状态流转必须集中校验，不能让各 Agent 或业务服务自由写字符串。
- 资源生成草稿完成后应进入 `RUNNING -> WAITING_REVIEW`，而不是生成后直接视为完成。
- 审核全部通过后，业务任务和关联 `agent_task` 应进入 `DONE`。
- 取消能力先做协作式持久化状态取消，后续异步 worker 再接入实际中断。
- 失败状态可能被外层事务回滚，需要专项验证。

## 集成处理

- 已在 `AgentRuntimeConstants` 增加 task/trace 状态集合、终态集合和可取消状态集合。
- 已在 `AgentRunRecorder` 增加初始状态校验、trace 状态校验、状态流转校验、结构化失败输出和取消能力。
- 已新增 `POST /api/agent/tasks/{taskId}/cancel`。
- 已让资源生成成功后进入 `WAITING_REVIEW`，审核全部通过后进入 `DONE`。
- 已新增模型失败回归测试，并修复 `resource_generation_task` 在模型重试失败后仍停留 `RUNNING` 的问题。
- 已保留边界：完整重试调度、后台恢复扫描和真实异步 worker 中断进入 P0-3。

## 外部参考吸收

- LMS 项目参考：Frappe LMS、Open edX、LearnHouse、Pupilfirst 的价值在于课程/任务/进度/反馈需要稳定状态，本轮落到后端状态机和审核门禁。
- Spring AI 参考：Spring AI、Spring AI Alibaba、Spring AI Alibaba examples 的价值在于 Agent runtime 应具备编排、观测、评估和工具调用边界，本轮落到 `agent_task`、`agent_trace`、`model_call_log` 的统一治理。
