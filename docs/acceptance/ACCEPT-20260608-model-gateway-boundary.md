# ACCEPT - P3-3-A 模型网关结构化校验与日志补齐

## 1. 验收结论

通过。

资源生成模型网关边界已完成最小生产化硬化：`agent-resource-v1` structured output 会在 `AiModelGateway` 边界校验；缺 `resources[]`、缺资源必填字段或非法 `safetyStatus` 不会被当作成功输出；失败证据只保留安全错误码；成功模型调用日志使用 gateway response 的 model、latency、token usage 和 cost。

## 2. 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| ResourceAgent 请求携带 `agent-resource-v1` | PASS | `ResourceGenerationService` 使用 `AgentRuntimeConstants.PROMPT_RESOURCE_V1` |
| 缺 `resources[]` 被拒绝 | PASS | `AiModelGatewayTest.rejectsStructuredResourceOutputWhenRequiredResourcesFieldIsMissing` |
| resource item 缺必填字段被拒绝 | PASS | `AiModelGatewayTest.rejectsStructuredResourceOutputWhenResourceItemMissesRequiredFields` |
| 非法 `safetyStatus` 被拒绝 | PASS | `AiModelGatewayTest.rejectsStructuredResourceOutputWhenSafetyStatusIsInvalid` |
| provider raw error 不进入 exception message / cause / recorder 参数 | PASS | `AiModelGatewayTest.sanitizesProviderFailureBeforeThrowingAndRecordingFailure` |
| malformed structured output 进入安全 recoverable failure | PASS | `ResourceGenerationControllerTest` malformed structured output 覆盖 |
| 成功 `model_call_log.model`、`latencyMs` 来自 gateway response | PASS | `AgentRunRecorderTest` 与 `ResourceGenerationControllerTest` 成功日志断言 |
| 成功 token usage / cost 来自 gateway response | PASS | `AgentRunRecorderTest` 与 `ResourceGenerationControllerTest` 成功 token/cost 断言 |
| 无业务代码直接调用 Spring AI / provider SDK | PASS | `rg -n "ChatClient|ChatModel|EmbeddingModel|OpenAi|DashScope|springframework\.ai" backend\src\main\java` 无匹配 |
| 不新增依赖、不改 DB schema、不改 frontend | PASS | 未修改 `backend/pom.xml`、migration、`frontend/**` |
| Evidence / Acceptance / Memory / Changelog / Retrospective / Skill 更新 | PASS | 本验收文档、Evidence、Memory、Changelog、Retro、Skill Registry 已更新 |

## 3. 测试验收

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=AiModelGatewayTest test` | BUILD SUCCESS；Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn --% -Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest test` | BUILD SUCCESS；Tests run: 24, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn --% -Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,AgentTraceControllerTest,AnalyticsControllerTest test` | BUILD SUCCESS；Tests run: 75, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn test` | BUILD SUCCESS；Tests run: 275, Failures: 0, Errors: 0, Skipped: 1 |

## 4. Open Items

- P3-3-B：接入真实 Spring AI / Spring AI Alibaba Chat provider。
- 后续 schema 切片：如需 DB 层持久化 provider，需要新增 `model_call_log.provider` 字段并走独立 schema / migration / dependency review。
- Embedding provider、VectorDB adapter、hybrid retrieval、RRF、reranker timeout fallback 仍属 P3-2/P3-3 后续工作。
- 当前资源正文仍为 deterministic draft，真实模型输出消费与资源级 citation schema 留待后续实现。
