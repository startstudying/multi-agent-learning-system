# RUN-20260608 P3-3-A 模型边界专家报告

## 结论

P3-3-A 采用“不新增依赖、不改 DB schema、不改 frontend、不接真实 provider”的最小模型边界硬化切片。当前 `AiModelGateway` 已是资源生成模型调用入口，但仍存在两个缺口：

- `structuredOutput` 只是占位 Map，没有按资源生成 schema 校验必填字段。
- 成功 `model_call_log` 的 `model` / `latencyMs` 仍来自 trace 占位值，而不是 gateway response。

## 建议

1. 在 `AiModelGateway` 内部按 `agent-resource-v1` 校验 `resources[]` 及资源字段：
   - `title`
   - `type`
   - `modality`
   - `markdownContent`
   - `citationSummary`
   - `safetyStatus`
2. 结构化输出校验失败走 retry；重试耗尽后写失败 task / trace / model_call_log。
3. `ModelResponse` 补充 `latencyMs`，让成功日志以 gateway response 为事实源。
4. 本切片不新增 `model_call_log.provider` 字段；provider 仍通过 gateway response 与 Micrometer tags 保留，持久化 provider 放入 P3-3-B 或独立 schema 切片。

## 建议修改文件

- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java`
- `backend/src/test/java/com/learningos/agent/application/AgentRunRecorderTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`

## 不建议修改

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- RAG embedding / VectorDB / reranker 实现
