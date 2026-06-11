# 模型调用 Prompt 元数据复盘

## 1. 做对的事

- 先用 RED 测试证明 `ModelCallLog` 缺少 prompt metadata。
- 用白名单 schema 摘要代替 raw schema，避免把动态模型输出或课程内容写入日志。
- 同时覆盖成功和失败模型调用，避免失败链路缺少 prompt 版本。
- 借安全 subagent 发现 provider error 泄露风险，并在本切片内收敛为安全错误码。

## 2. 发现的问题

- 旧测试曾要求 provider 原始错误进入 `agent_task.outputJson`，这与当前治理目标冲突。
- `AgentRunRecorder` 的模型日志写入仍是内部固定 temperature，未来真实模型接入时需要从模型配置传入。
- PromptVersion 管理 API 目前没有权限边界，后续需要做 teacher/admin 控制或隐藏 promptText。

## 3. 后续建议

- 增加 `promptHash`、`schemaHash`、`schemaVersion`，进一步支持实验对比。
- 将 PromptVersion API 接入真实 RBAC。
- 增加 evaluation set 管理，连接 prompt metadata 与质量指标。

## 4. Skill Extraction

不新增项目专属 skill。本切片继续复用 `agent-trace-governance`、`spring-ai-agent-backend` 和 `test-driven-development`。
