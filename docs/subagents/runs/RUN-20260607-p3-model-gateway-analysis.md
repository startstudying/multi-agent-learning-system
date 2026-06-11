# RUN-20260607 P3-3 模型接入边界分析

## 结论

P3-3 当前仍是“占位模型网关 + 规则/确定性业务输出”状态。`AiModelGateway` 已是资源生成注入边界，但没有真实 Spring AI Chat/Embedding 调用，`EmbeddingService` 仍是 `noop`。

## 证据

- `backend/pom.xml` 只声明 Spring AI BOM，没有 provider starter。
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java` 即使 provider configured 也返回本地 structured output，`externalCall=false`。
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java` 返回 `noop-embedding-v1`。
- `backend/src/main/java/com/learningos/agent/domain/ModelCallLog.java` 有 model/prompt/latency/token/error 基础字段，但缺 provider 字段。

## 推荐切片

先做无新增依赖的模型边界/日志/结构化输出验证切片，再单独做真实 Spring AI provider dependency review。不要在当前回合直接接外部 SDK。

## 建议测试

```bash
cd backend && mvn "-Dtest=AiModelGatewayTest,AgentRunRecorderTest,IndexServiceTest,SchemaConvergenceMigrationTest" test
cd backend && mvn test
```

