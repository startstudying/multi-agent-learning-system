# PRD - P3-3 真实模型 Provider Adapter

## 1. 问题陈述

当前后端已经建立 `AiModelGateway`、`EmbeddingService`、`VectorIndexAdapter`、`model_call_log`、`token_usage_log` 等边界，但真实 Chat/Embedding provider 尚未接入。`AI_MODEL_PROVIDER=openai` 这类配置目前只会产生 placeholder 或 `EMBEDDING_PROVIDER_NOT_CONFIGURED`，无法满足 P3-3 “用 Spring AI/Spring AI Alibaba 接入真实 Chat/Embedding 模型”的生产化目标。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 后端开发者 | AI/RAG 功能维护者 | 通过统一 gateway 接入真实 provider，而不是在业务层散落 SDK 调用。 |
| 运维/管理员 | 平台运行维护 | 通过环境变量配置 provider/key/model，健康与日志不泄露敏感值。 |
| 教师/学生 | 间接受益用户 | 资源生成和 RAG 索引能从 deterministic/noop 走向真实模型能力。 |

## 3. 用户故事

- 作为后端开发者，我希望 `ResourceGenerationService` 仍只调用 `AiModelGateway`，以便真实模型接入不破坏业务分层。
- 作为 RAG 维护者，我希望 `EmbeddingService` 能调用真实 `EmbeddingModel`，以便索引流程进入真实 embedding provider 边界。
- 作为管理员，我希望未配置 provider 时系统继续本地可测，配置 provider 但缺少 SDK/bean 时 fail closed，而不是伪装成成功。

## 4. MVP 范围

### 纳入范围

- 新增 Spring AI 官方 OpenAI-compatible starter 作为本切片唯一真实 provider 路径。
- 将 Spring AI BOM 升级到同一稳定线的 `1.0.8`。
- 在 `AiModelGateway` 内部接入 `ChatModel` adapter。
- 在 `EmbeddingService` 内部接入 `EmbeddingModel` adapter。
- 保持 provider `none` 的 deterministic/noop 本地行为。
- Provider 异常、结构化输出异常、embedding 异常全部使用固定安全错误码。

### 非目标

- 不接入 Spring AI Alibaba DashScope starter；后续独立切片处理。
- 不接入 VectorDB，不实现真实语义检索排序。
- 不修改 Controller、外部 API、DB schema、frontend。
- 不新增 provider 管理 UI。
- 不提交 API key、base URL、organization/project 等敏感配置值。

## 5. 成功指标

| 指标 | 目标值 | 衡量方式 |
|---|---|---|
| Chat adapter 可用 | provider bean 存在时通过 `AiModelGateway` 返回真实 structured output | `AiModelGatewayTest` |
| Embedding adapter 可用 | provider bean 存在时 `EmbeddingService` 返回 `SUCCEEDED` | `EmbeddingServiceTest` |
| 安全错误脱敏 | provider raw error 不进入异常、trace、DB 失败参数 | 单元测试断言固定错误码 |
| 默认本地兼容 | provider `none` 不需要 key，不外呼 | 现有回归测试 |
| 依赖审查完整 | 新依赖有 `docs/security/` review | 文档检查 |

## 6. 高层流程

```text
环境变量配置 provider/model/key
-> Spring AI auto-configuration 创建 ChatModel/EmbeddingModel
-> 业务服务继续调用 AiModelGateway / EmbeddingService
-> adapter 调用真实 provider
-> gateway 校验 structured output / 记录 model evidence
-> provider none 或缺配置时按安全降级/失败语义返回
```

## 7. 依赖关系

- 前置：P3-3-A/B 已完成模型 gateway structured output 与 provider observability。
- 前置：P3-2-A 已完成 embedding/vector boundary。
- 新增依赖：`org.springframework.ai:spring-ai-starter-model-openai`，详见 `docs/security/DEPENDENCY-REVIEW-20260608-real-model-provider-adapter.md`。

## 8. 待澄清问题

| 问题 | 状态 |
|---|---|
| 是否必须同步接 DashScope | 本切片否；P3-3 后续可独立扩展。 |
| 是否必须同步接 VectorDB | 本切片否；VectorDB 需独立依赖和部署审查。 |

## 9. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | APPROVED |
