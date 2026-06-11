# Agent 状态机收敛 PRD

## 背景

当前后端已经有 `agent_task`、`agent_trace`、`model_call_log`、`token_usage_log`，但 Agent 任务和 Trace 状态仍主要依赖字符串常量。随着后续接入 Spring AI / Spring AI Alibaba 的多 Agent 编排、Tool Calling、RAG 索引和评估任务，裸字符串状态会导致失败不可恢复、取消不可控、Trace 难以审计。

参考方向：

- Open edX / Frappe LMS：平台需要稳定的课程、任务、进度与反馈状态，不能只靠一次性生成。
- Spring AI / Spring AI Alibaba：Agent 运行时应有清晰的执行状态、失败记录、观测日志和后续评估能力。

## 目标

- 收敛 Agent Task 和 Agent Trace 可写状态。
- 在 Service 层阻止非法状态和非法状态流转。
- 失败时写入结构化 `outputJson`、失败 Trace、Model Call failure。
- 提供最小取消能力，避免取消后的长任务继续被视为可运行。

## 非目标

- 不把数据库字段改成 enum。
- 不新增 Spring AI / Spring AI Alibaba 依赖。
- 不实现异步队列、后台恢复扫描和完整重试调度。
- 不改前端页面。

## 用户价值

- 管理端能更可靠地解释 Agent 任务处于什么状态。
- 后续资源生成、RAG 索引、测评批改等长任务可以复用同一套运行时状态语义。
- 论文/答辩中可以说明系统具备可审计、可恢复的 Agent Runtime 基础。
