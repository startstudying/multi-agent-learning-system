# Acceptance: P3-2 real VectorDB adapter minimum integration

## 验收结论

Verdict: PASS

P3-2 子任务 `real VectorDB adapter minimum integration` 已满足 L 级子任务验收要求。

注意：本结论只关闭该语义子任务，不关闭 P3-2 父项。

## 验收标准

| 标准 | 结果 | 证据 |
|---|---|---|
| 默认禁用时使用 Noop，且不连接 Qdrant | PASS | `RagVectorConfigurationTest.defaultsToNoopWhenVectorDbIsDisabled`；focused `6 run, 0 failures` |
| 显式启用且配置完整时装配 Qdrant adapter | PASS | `RagVectorConfigurationTest.createsQdrantAdapterOnlyWhenExplicitlyEnabledAndConfigured` |
| 启用但 Qdrant 配置不完整时 fail fast | PASS | `RagVectorConfigurationTest.failsFastWhenEnabledQdrantConfigIsIncomplete` |
| upsert payload 不含 raw content/question/prompt/userId/storage key/secret | PASS | `QdrantVectorIndexAdapterTest.upsertMapsVectorPayloadWithoutRawChunkContent`；`IndexServiceVectorPayloadTest` |
| search 下推 `allowedKbIds` 和 `topK` | PASS | `QdrantVectorIndexAdapterTest.searchPassesAllowedKbIdsTopKAndQueryVectorWithoutRawQuestion` |
| search 不携带 raw question | PASS | `VectorSearchRequest` 只携带 query vector；focused 测试断言 `toString()` 不含 raw question/vector values |
| search 只返回低敏 `chunkId` payload，不返回 vector | PASS | `NativeQdrantVectorOperations.search(...)` 使用 `setWithPayload(...)` include `chunkId` + `setWithVectors(...enable=false)` |
| provider 失败返回固定低敏错误码 | PASS | `QdrantVectorIndexAdapterTest.providerFailuresReturnSafeErrorCodes` |
| RAG 查询在 vector disabled/failure 时保持 fallback | PASS | `ChunkServiceVectorRetrievalTest`、`RagQueryServiceTest` adjacent 通过 |
| indexing upsert/delete failure 不伪装成功 | PASS | `QdrantVectorIndexAdapterTest` provider failure 行为覆盖 |
| Full backend 测试通过 | PASS | `mvn test`：`592 run, 0 failures, 0 errors, 1 skipped` |
| 依赖审查和 dependency tree 已记录 | PASS with risk | dependency tree 确认 `grpc-netty-shaded:1.65.1` 传递存在且 `grpc-api:1.75.0` 被解析；未运行 dependency-check/CVE scanner，后续独立评审 |

## 验证记录

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=QdrantVectorIndexAdapterTest,RagVectorConfigurationTest test
```

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=NoopVectorIndexAdapterTest,IndexServiceVectorPayloadTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest,RagQueryServiceTest test
```

```text
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```powershell
cd D:\多元agent\backend
mvn test
```

```text
Tests run: 592, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

```powershell
cd D:\多元agent\backend
mvn --% dependency:tree -Dincludes=org.springframework.ai:spring-ai-qdrant-store,io.qdrant:client,io.grpc:grpc-netty-shaded,io.grpc:grpc-api
```

```text
+- org.springframework.ai:spring-ai-qdrant-store:jar:1.0.8:compile
|  \- io.qdrant:client:jar:1.13.0:compile
|     \- io.grpc:grpc-netty-shaded:jar:1.65.1:runtime
\- io.grpc:grpc-api:jar:1.75.0:compile
BUILD SUCCESS
```

## 已接受限制

1. 未连接真实 Qdrant 服务做 smoke test。
2. 未实现 collection schema 初始化、dimension 校验或 startup compatibility check。
3. 未新增 health endpoint、ops alert、Docker/Testcontainers 编排。
4. 未在本任务中覆盖 `grpc-netty-shaded:1.65.1` 或对全部 `grpc-*` / `netty-*` 做 dependency-check/CVE scanner，只记录传递依赖风险；后续需要独立依赖安全切片处理。

## 后续建议

- 新建独立子任务：Qdrant real service smoke + collection dimension validation。
- 新建独立子任务：gRPC/Netty dependency upgrade / risk acceptance review。
- 新建独立子任务：Qdrant health/ops integration and startup compatibility check。
- P3-2 父项继续保持 open，等待工业级 PDF layout/table/TOC、native/cloud OCR、provider confidence pipeline、真实渲染页码等后续任务完成。
