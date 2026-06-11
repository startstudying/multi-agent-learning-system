# Subagent Run: model-call-prompt-metadata

## 决策

- Use Subagents: Yes。
- Reason: 涉及 Agent trace、模型日志、DB migration、测试和安全治理。
- Parallelism Level: L1 Parallel Analysis。
- Implementation Mode: Single Codex。

## 已启动 subagent

- Architect: 分析模型日志数据模型和调用链。
- Test Engineer: 设计 RED/GREEN 测试。
- Security Reviewer: 审查 prompt/schema 元数据安全边界。

## 已返回报告

Test Engineer 建议：

- 在 `AgentRunRecorderTest` 增加成功和失败模型调用 prompt metadata 测试。
- 在 `SchemaConvergenceMigrationTest` 增加 V12 migration 文本测试。
- 回归命令覆盖 `AgentRunRecorderTest`、`ResourceGenerationControllerTest`、`SchemaConvergenceMigrationTest`、`PromptVersion*`、`AiModelGatewayTest`。

Security Reviewer 指出：

- `model_call_log` 不应保存 raw prompt、学生答案、课程全文、provider response 或完整动态 schema。
- `structuredOutputSchema` 应是后端白名单摘要。
- 现有 provider error 会进入 `model_call_log.errorMessage`、`agent_task.outputJson`、`agent_trace.summary`，应改为安全错误码。

Architect 未在短等待内返回，已关闭，未采用其输出。

## 集成结论

本切片采用最小治理实现：

- `model_call_log` 新增 prompt 元数据列。
- `AgentRunRecorder` 通过 prompt version 白名单推导 prompt code 和 structured output schema。
- `temperature` 暂用 deterministic demo 默认值 `0.0`。
- 不保存 raw prompt 或 raw output。
- 失败 provider message 统一脱敏为 `MODEL_PROVIDER_ERROR`。
