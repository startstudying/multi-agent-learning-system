# 模型调用 Prompt 元数据 PRD

## 背景

P2-1 要求补齐 Prompt Version 与实验集管理能力。当前系统已有 `prompt_version` 表和 `agent_trace.promptVersion`，但 `model_call_log` 只记录模型、状态、延迟、错误和成本，无法稳定回答“某次模型调用使用了哪个 prompt code/version、什么模型参数、什么结构化输出 schema”。

## 目标

- 每次模型调用日志记录 prompt code、prompt version、model、temperature、结构化输出 schema。
- 成功和失败模型调用都保留相同的审计元数据。
- 元数据只保存稳定标识和 schema 摘要，不保存原始 prompt、学生答案或课程全文。

## 非目标

- 不实现 evaluation set 管理。
- 不接入真实模型 provider。
- 不在本轮修改 Prompt Version 管理 API。
- 不保存 raw prompt 或 raw model output。

## 用户价值

- 论文实验可以按 prompt version 和 schema 对比模型调用质量。
- 后台治理可以追踪失败调用对应的 prompt 版本。
- 后续评估集和 trace dashboard 能直接复用 `model_call_log` 元数据。

## 验收标准

- `model_call_log` 表具备 `prompt_code`、`prompt_version`、`temperature`、`structured_output_schema`。
- 成功模型调用写入上述字段。
- 失败模型调用写入上述字段。
- 资源生成集成路径能持久化资源生成 schema 元数据。
