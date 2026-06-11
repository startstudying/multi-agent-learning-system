# Agent 状态机收敛计划

## 执行顺序

1. 读取 Agent 运行记录、资源生成、Trace 查询现状。
2. 创建 PRD、REQ、SPEC、PLAN、TASK、Context Pack。
3. 启动 L1 架构子代理做只读审查。
4. 先写失败测试：
   - 非法 `startRun` 状态拒绝。
   - 非法 trace step 状态拒绝。
   - 失败输出包含 `recoverable`。
   - 可运行任务可取消并追加 trace。
   - 终态任务不可取消。
5. 实现 `AgentRuntimeStateMachine` 或等价服务类。
6. 在 `AgentRunRecorder` 中接入状态校验和取消能力。
7. 在 `AgentTraceController` / `ResourceGenerationService` 暴露取消接口。
8. 跑聚焦测试和全量后端测试。
9. 更新 TODO、evidence、acceptance、memory、changelog。

## 风险

- 当前 `ResourceGenerationService.createTask` 是同步演示式执行，取消能力主要服务于持久化状态和后续异步任务，不会中断已经完成的同步调用。
- 数据库仍是字符串字段，历史脏状态只能通过服务层新增写入治理，不能自动清理旧数据。
- 完整后台恢复扫描和重试字段仍属于 P0-3 后续任务。
- 架构子代理指出的“失败状态可能被外层事务回滚”风险已处理：资源生成模型失败时对 `ModelCallFailedException` 不回滚，`agent_task`、`agent_trace`、`model_call_log` 和 `resource_generation_task` 都保留失败证据。

## 参考项目吸收

- Frappe LMS、Open edX、LearnHouse、Pupilfirst 主要用于确认 LMS 底座应该稳定表达课程、任务、进度和反馈状态；本轮落到后端任务状态收敛，不改 UI。
- Spring AI、Spring AI Alibaba、Spring AI Alibaba examples 主要用于确认 Agent runtime 需要编排、观测、评估和工具调用边界；本轮落到 `AgentRunRecorder` 统一状态写入、trace 可审计和失败可恢复，不新增依赖。
- 后续接入 Spring AI / Spring AI Alibaba Graph 时，应复用本轮 `agent_task` / `agent_trace` 状态语义，而不是让各 Agent 自己写裸字符串状态。

## 架构漂移检查

- Backend layering：PASS，状态治理在 Service 层。
- Frontend rules：N/A，本轮不改前端。
- Agent/RAG rules：PASS，增强 Agent Trace 可审计性。
- Security：PASS，无新增依赖、无密钥。
- API/Database：PASS，新增 API 已在 SPEC 记录；无数据库变更。
