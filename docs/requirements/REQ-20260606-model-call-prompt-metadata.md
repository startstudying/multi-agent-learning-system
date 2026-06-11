# 模型调用 Prompt 元数据需求

## 功能需求

1. `model_call_log` 新增字段：
   - `prompt_code`
   - `prompt_version`
   - `temperature`
   - `structured_output_schema`
2. 成功模型调用日志必须记录：
   - prompt code
   - prompt version
   - model
   - temperature
   - structured output schema
3. 失败模型调用日志也必须记录同样的 prompt 元数据。
4. 资源生成链路使用的 prompt code 固定为 `resource-generation`，prompt version 使用现有 `agent-resource-v1`。
5. deterministic demo 模型温度默认为 `0.0`。

## 安全需求

- `structuredOutputSchema` 只保存 schema 名称或字段白名单，不保存原始模型输出。
- 不在新增字段中保存学生答案、课程原文、用户 prompt 或 provider 原始响应。
- `errorMessage` 的现有行为本轮不扩大暴露面。

## 兼容需求

- 旧模型日志新增列可为空或有安全默认值。
- 不改变现有 token usage 写入。
- 不改变现有 Prompt Version API。

## 测试需求

- RED 测试覆盖实体字段缺失和 V12 migration 缺失。
- GREEN 后覆盖成功和失败两条模型调用日志。
- 回归资源生成控制器、PromptVersion、AiModelGateway 相关测试。
