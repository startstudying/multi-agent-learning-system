# EVIDENCE - Micrometer 运行指标
状态：已完成（2026-06-07）

## 1. 验证范围

P3-5 Micrometer 业务指标切片：HTTP 请求、RAG 查询、模型调用、token 使用、Actuator 暴露。

## 2. 验证结果

| 命令 | 结果 |
|---|---|
| `cd backend && mvn --% -Dtest=StructuredRequestLoggingFilterTest,RagQueryServiceTest,AiModelGatewayTest,AgentRunRecorderTest test` | 通过，28 tests，0 failures，0 errors |
| `cd backend && mvn --% -Dtest=GlobalExceptionHandlerTest,HealthControllerTest,StructuredRequestLoggingFilterTest test` | 通过，9 tests，0 failures，0 errors |
| `cd backend && mvn test` | 通过，244 tests，0 failures，0 errors，1 skipped |

## 3. 关键证据

- `LearningOsMetrics` 统一封装 `MeterRegistry`，集中维护 meter name 和低基数 tag 白名单。
- `StructuredRequestLoggingFilter` 在 MVC slice 下可安全工作，不再要求测试上下文一定提供 `CurrentUserService`。
- `application.yml` 只新增 `metrics` 暴露，未打开 `env`、`beans`、`heapdump`、`threaddump` 等高风险 endpoint。
- `RagQueryService`、`AiModelGateway`、`AgentRunRecorder` 的定向测试均通过，指标语义未回退。

## 4. 审查补充

Security & Quality 专家子代理因 `agent thread limit reached` 未能启动；已由 Main Codex 本地完成安全审查与 tag 白名单检查，未发现敏感字段进入 metric tags。

