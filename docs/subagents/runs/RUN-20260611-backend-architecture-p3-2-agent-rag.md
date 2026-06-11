# RUN-20260611 backend architecture P3-2 Agent/RAG 子代理报告

## 结论

`docs/planning/backend-architecture-todolist.md` 已将 P3-2 最小生产化标记完成，但文末仍保留两个后续增强项：工业级文档解析和 VectorDB 运维。工业级文档解析整体偏 L，建议继续拆 M 子任务；VectorDB 运维偏 M，其中健康检查、配置校验、dimension gate 和风险治理可默认离线验证，真实 Qdrant 连通只能作为 opt-in smoke。

## 相关现状

- Parser/OCR：`backend/src/main/java/com/learningos/rag/parser/*`
- Index 消费边界：`backend/src/main/java/com/learningos/rag/application/IndexService.java`
- VectorDB 端口：`backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`
- Qdrant adapter：`backend/src/main/java/com/learningos/rag/vector/*`
- Health 接入：`backend/src/main/java/com/learningos/health/application/HealthService.java`
- 测试：`RealParserProviderTest`、`DocumentParserServiceTest`、`ProcessOcrFallbackProviderTest`、`QdrantVectorIndexAdapterTest`、`RagVectorConfigurationTest`、`QdrantVectorExternalSmokeTest`

## 推荐拆分

1. Qdrant external smoke 实测化：将占位测试改成 opt-in 真实连通与 collection/dimension 检查。
2. VectorDB health/ops 完善：补足 disabled/up/down/dimension mismatch 和低敏 metadata 证据。
3. gRPC/Netty 风险复核：在 CI 或可联网环境运行依赖审计。
4. PDF layout/table/TOC provider：限定在 `rag/parser`，不扩散到 Controller 或 retrieval。
5. native/cloud OCR provider confidence：必须单独依赖审查、凭证治理和 opt-in smoke。

## 风险

- Qdrant 真实 smoke 需要外部服务与可选 API key，不能进入默认 `mvn test`。
- VectorDB 只能返回 `chunkId`，业务内容必须回库后再按 allowed KB 过滤。
- Parser 增强不能把 PDFBox/POI/OCR 细节泄漏到 `IndexService` 之外。
- OCR/provider 原始响应、raw vector、raw chunk、secret 不得写入 metadata、trace、log 或 health。

## 测试建议

```powershell
cd backend
mvn --% -Dtest=QdrantVectorIndexAdapterTest,RagVectorConfigurationTest,HealthServiceTest,HealthControllerTest test
mvn --% -Dtest=RealParserProviderTest,DocumentParserServiceTest,ProcessOcrFallbackProviderTest,IndexServiceTest,IndexServiceParserFailureTest test
```

Opt-in:

```powershell
$env:QDRANT_SMOKE_ENABLED='true'
$env:RAG_VECTOR_QDRANT_HOST='127.0.0.1'
$env:RAG_VECTOR_QDRANT_PORT='6334'
$env:RAG_VECTOR_QDRANT_COLLECTION='learning_os_chunks'
$env:RAG_VECTOR_QDRANT_EXPECTED_DIMENSION='1536'
mvn --% -Dtest=QdrantVectorExternalSmokeTest test
```
