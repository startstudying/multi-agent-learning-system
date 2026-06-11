# Dependency Review - P3-3 真实模型 Provider Adapter

## Dependency

| Field | Value |
|---|---|
| Name | `org.springframework.ai:spring-ai-starter-model-openai` |
| Version | Managed by `org.springframework.ai:spring-ai-bom:1.0.8` |
| Package Manager | Maven |
| Added By | TASK-20260608-real-model-provider-adapter |

## Justification

P3-3 要求用 Spring AI/Spring AI Alibaba 接入真实 Chat/Embedding 模型。当前项目仅有 Spring AI BOM `1.0.3`，没有具体 provider starter，`AiModelGateway` 和 `EmbeddingService` 仍是 placeholder/noop。引入 Spring AI 官方 OpenAI-compatible starter 后，可以通过 Spring Boot auto-configuration 获得 `ChatModel` 和 `EmbeddingModel`，并将真实 provider 调用限制在现有 gateway/service 边界内。

同时将 Spring AI BOM 从 `1.0.3` 升级到 `1.0.8`。Maven Central 元数据显示 `1.0.8` 是当前 1.0.x patch 线可用版本之一；Spring 官方安全公告存在 Spring AI 已知问题，继续基于 `1.0.3` 接入真实 provider 不合适。本切片不选择 milestone/RC 版本。

## Alternatives Considered

| Alternative | Pros | Cons | Decision |
|---|---|---|---|
| Spring AI 官方 OpenAI-compatible starter | 与现有 Spring AI BOM 对齐；同时提供 Chat/Embedding；适合最小切片 | 需要配置 `spring.ai.openai.api-key`，依赖 HTTP/WebFlux 传递依赖 | 采用 |
| Spring AI Alibaba DashScope starter | 适合 Qwen/DashScope 场景 | 需单独确认版本矩阵，避免与 Spring AI BOM 冲突 | 后续独立切片 |
| 直接 provider SDK | 短期直接 | 绕开 Spring AI 抽象，增加安全/日志/重试分散风险 | 拒绝 |
| 不新增依赖，只保留 placeholder | 无依赖风险 | 无法完成 P3-3 真实 provider 接入 | 拒绝 |

## License

| Field | Value |
|---|---|
| License | Apache License 2.0 |
| Compatible | Yes |

证据：Maven POM `spring-ai-starter-model-openai-1.0.8.pom` 和 `spring-ai-openai-1.0.8.pom` 声明 Apache 2.0。

## Security

- [x] Trusted publisher：`org.springframework.ai`，Spring 官方项目。
- [x] Maintained actively：Maven Central metadata 显示 1.0.x、1.1.x、2.0.0 RC 仍在发布。
- [x] No secrets required in source：key 只通过 `OPENAI_API_KEY` / Secret Manager 注入。
- [x] Known advisory considered：Spring 官方安全公告存在 Spring AI 已知风险；本切片升级到 `1.0.8`，不继续使用 `1.0.3`。
- [x] No VectorDB dependency：避免把向量存储安全风险并入本切片。
- [ ] Full SCA：本地 `dependency-check` 受网络初始化限制，不能形成完整 CVE 结论；以 Maven metadata、Spring 官方安全公告和 dependency tree 作为本切片审查证据。

## Impact

- Bundle size impact：新增 Spring AI OpenAI starter 及其 auto-configuration/model 依赖。
- Transitive dependencies concern：`spring-ai-openai` 包含 `spring-ai-model`、`spring-ai-retry`、`spring-webflux`、json schema 相关依赖；需通过 `mvn dependency:tree` 记录。
- Runtime requirement：真实 provider 外呼只在显式配置 `AI_MODEL_PROVIDER`、model、provider key 且 Spring AI bean 存在时发生；默认 `provider=none` 不外呼。

## Secret Handling

- 不在 `application.yml` 写真实 key。
- 不在 `.env.example` 写真实 key。
- 不在 memory/evidence/trace/model_call_log/metrics/health 输出 key/base URL/organization/project。
- 不允许用户请求参数、prompt、课程内容控制 provider base URL。

## Approval

| Role | Date | Status |
|---|---|---|
| Dependency Expert | 2026-06-08 | APPROVED WITH CONDITIONS |
| Security & Quality Expert | 2026-06-08 | APPROVED WITH CONDITIONS |
| Main Codex | 2026-06-08 | APPROVED |

## Conditions

1. 真实 provider SDK 只能在 `AiModelGateway` / `EmbeddingService` 的内部 adapter 使用。
2. Provider 配置完整但 bean 缺失时必须 fail closed。
3. Raw provider error / prompt / chunk / vector / secret 不得持久化。
4. DashScope 与 VectorDB 后续单独 dependency review。
