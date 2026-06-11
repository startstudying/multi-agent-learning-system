# PRD-20260608 模型调用 provider 持久化观测

## 背景

P3-3 要求模型调用日志能完整支撑 provider、model、prompt version、latency、token、error 的审计。P3-3-A 已完成 `AiModelGateway` 结构化输出校验与安全错误码，但 `model_call_log` 仍没有 `provider` 字段，导致持久化审计无法按 provider 归因。

## 目标

在不接真实 provider、不新增依赖、不改前端 API 的前提下，完成 `model_call_log.provider` 最小闭环：

- 为 `model_call_log` 增加低基数 `provider` 字段。
- `ModelCallLog` entity 与 Flyway schema 保持一致。
- 成功模型调用从 gateway response 写入安全 provider。
- 失败模型调用从 gateway failure context 写入安全 provider。
- provider 值必须经过白名单归一化，避免 URL、API key、tenant、endpoint 等敏感或高基数字符串进入 DB / metrics。

## 目标用户

- 管理员：需要按 provider 查看模型失败率、延迟和成本归因。
- 开发/运维：需要在不暴露 raw provider error 的前提下排查模型调用链路。
- 论文/答辩评审：需要看到 Agent Trace、Model Call、Token Usage 的可审计闭环。

## MVP 范围

- 新增 Flyway V18：`model_call_log.provider varchar(80) not null default 'none'`。
- `ModelCallLog` 增加 `provider` 字段和默认值。
- `AgentRunRecorder` 成功/失败路径写 provider。
- `AiModelGateway` provider 归一化为低基数枚举。
- 测试覆盖 migration 文本、MySQL smoke、recorder 成功/失败、gateway provider 归一化。

## 非目标

- 不接入真实 Spring AI / Spring AI Alibaba provider。
- 不新增 provider SDK、API key、base URL、timeout 等配置项。
- 不修改前端展示。
- 不新增模型调用明细 API。
- 不保存 raw prompt、raw completion、raw provider request/response/exception。
- 不把 provider base URL、deployment URL、tenant、region endpoint 落库。

## 成功标准

- Flyway V18 可从空 MySQL schema 迁移成功。
- `model_call_log.provider` 在成功路径写入 gateway provider，例如 `openai`。
- 失败路径写入 configured provider，例如 `openai`，但 `errorMessage` 仍只保存安全错误码。
- 未知或敏感 provider 配置归一化为 `other`，不出现 URL、`apiKey`、`sk-` 等片段。
- 聚焦、相邻回归、全量后端测试和 MySQL smoke 形成 Evidence。

