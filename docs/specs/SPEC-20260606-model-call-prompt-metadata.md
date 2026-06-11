# 模型调用 Prompt 元数据 SPEC

## 数据模型

表：`model_call_log`

新增字段：

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `prompt_code` | `varchar(120)` | nullable | prompt 业务代码，例如 `resource-generation` |
| `prompt_version` | `varchar(120)` | nullable | prompt 版本，例如 `agent-resource-v1` |
| `temperature` | `double` | nullable | 模型温度；deterministic demo 使用 `0.0` |
| `structured_output_schema` | `varchar(1000)` | nullable | 结构化输出 schema 摘要或字段白名单 |

## Prompt Version 映射

现有 trace prompt version 命名形如：

```text
agent-resource-v1
agent-critic-v1
agent-tutor-v1
agent-safety-v1
```

本轮最小映射：

| promptVersion | promptCode | schema |
|---|---|---|
| `agent-resource-v1` | `resource-generation` | `ResourceGenerationOutputSchema{resources:[title,type,modality,markdownContent,citationSummary,safetyStatus]}` |
| `agent-critic-v1` | `critic-review` | `CriticReviewOutputSchema{status,summary,citationCheck,safetyCheck,revisionSuggestion}` |
| `agent-tutor-v1` | `tutor-response` | `TutorResponseSchema{answer,steps,citations,nextAction}` |
| `agent-safety-v1` | `safety-review` | `SafetyReviewSchema{status,reason,flags}` |

未知 prompt version 使用：

```text
promptCode = unknown
structuredOutputSchema = GenericStructuredOutputSchema{}
```

## 服务行为

- `AgentRunRecorder.recordSuccessfulModelEvidence(...)` 写入成功模型日志时，从 trace 的 `promptVersion` 推导 prompt code 和 schema。
- `AgentRunRecorder.recordFailure(...)` 写入失败模型日志时，使用传入 `promptVersion` 推导 prompt code 和 schema。
- 本轮不改变 `recordRuntimeFailure(...)`，因为它当前不写 `model_call_log`。
- `temperature` 使用 recorder 内部默认值 `0.0`，后续真实 provider 接入时再从模型配置传入。

## 安全边界

- `structuredOutputSchema` 是固定白名单字符串，不读取 request payload、promptText、model output。
- 不新增对外 API 暴露模型日志明细。
- 失败模型调用持久化和 trace summary 使用安全错误码 `MODEL_PROVIDER_ERROR`，不保存 provider 原始异常消息。

## Migration

新增：

```text
V12__model_call_prompt_metadata.sql
```

按现有 `add_column_if_missing` 风格给 `model_call_log` 补列。
