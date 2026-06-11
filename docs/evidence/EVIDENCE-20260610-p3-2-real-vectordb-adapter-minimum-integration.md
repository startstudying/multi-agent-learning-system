# Evidence: P3-2 real VectorDB adapter minimum integration

## 任务结论

P3-2 子任务 `real VectorDB adapter minimum integration` 已完成最小集成验证。

本子任务完成的是 Qdrant real VectorDB adapter 的最小可运行边界：

- 默认仍为 Noop，不外连 Qdrant。
- 只有显式配置 `learning-os.rag.vector.enabled=true` 且 `provider=qdrant` 时才装配真实 adapter。
- 真实 adapter 挂在项目自有 `VectorIndexAdapter` 后面。
- VectorDB payload 只包含低敏 chunk metadata 和 vector，不包含 raw chunk content、question、prompt、storage key、userId、secret。
- Qdrant search 只请求返回低敏 `chunkId` payload，并禁用 vector 返回，便于服务层回表和二次权限过滤。

P3-2 父项仍保持未完全关闭；工业级 PDF/PDF table/TOC/native-cloud OCR/provider confidence/真实渲染页码等仍是后续子任务。

## 本轮根因与修复证据

### 复现的阻塞

Adjacent 测试曾失败：

```powershell
mvn --% -Dtest=NoopVectorIndexAdapterTest,IndexServiceVectorPayloadTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest,RagQueryServiceTest test
```

失败根因：

```text
No qualifying bean of type 'com.learningos.rag.application.VectorIndexAdapter'
```

实际根因是 `NoopVectorIndexAdapter` 作为 component-scanned `@Service` 同时带 `@ConditionalOnMissingBean(VectorIndexAdapter.class)`，在完整 Spring context 条件报告中被自身 bean definition 影响，导致 fallback bean 没有稳定创建。

### 修复

- `NoopVectorIndexAdapter` 改为普通实现类，不再作为带条件的 component-scanned service。
- `QdrantVectorConfiguration` 提供 `@ConditionalOnMissingBean(VectorIndexAdapter.class)` 的 fallback `VectorIndexAdapter` bean。
- `RagVectorConfigurationTest` 不再用测试配置手动补 Noop bean，改为验证生产配置自身的 fallback 行为。
- `NativeQdrantVectorOperations.search(...)` 从不返回 payload 的风险设计修正为 `setWithPayload(...)` 只 include `chunkId`，同时通过 `setWithVectors(...enable=false)` 禁用 vector 返回。

## 专家 subagent 并行复核

| 专家 | 结论 |
|---|---|
| Debugger | 确认 Noop 条件装配的自我排除问题是 adjacent 失败根因；推荐将 Noop 注册移动到 configuration `@Bean`。 |
| Dependency/Security | 未发现 raw content/question/prompt/secret 进入 VectorDB payload；指出 search 禁 payload 又读 `chunkId` 的真实 Qdrant 命中风险，已修复为只 include `chunkId`。 |
| Verifier | 确认收尾缺 Evidence/Acceptance/Changelog/Memory/TODO，且不得误关 P3-2 父项。 |

## 验证命令与结果

### Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=QdrantVectorIndexAdapterTest,RagVectorConfigurationTest test
```

结果：

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-11T01:18:45+08:00
```

### Adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=NoopVectorIndexAdapterTest,IndexServiceVectorPayloadTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest,RagQueryServiceTest test
```

结果：

```text
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-11T01:19:56+08:00
```

### Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 592, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-11T01:23:01+08:00
```

### Dependency tree

```powershell
cd D:\多元agent\backend
mvn --% dependency:tree -Dincludes=org.springframework.ai:spring-ai-qdrant-store,io.qdrant:client,io.grpc:grpc-netty-shaded,io.grpc:grpc-api
```

结果：

```text
com.learningos:learning-os-backend:jar:0.1.0-SNAPSHOT
+- org.springframework.ai:spring-ai-qdrant-store:jar:1.0.8:compile
|  \- io.qdrant:client:jar:1.13.0:compile
|     \- io.grpc:grpc-netty-shaded:jar:1.65.1:runtime
\- io.grpc:grpc-api:jar:1.75.0:compile
BUILD SUCCESS
Finished at: 2026-06-11T01:23:49+08:00
```

## 安全证据

- 默认配置 `learning-os.rag.vector.enabled=false`，不会创建真实 Qdrant client。
- Qdrant adapter 只在 `enabled=true` 和 `provider=qdrant` 同时满足时创建。
- `QdrantGrpcClient.Builder#withApiKey(...)` 只在配置值非空时调用；adapter 捕获 provider 异常并返回固定错误码，不把 raw exception、host、api key 或 provider response 写入业务结果。
- Upsert point payload 白名单字段：
  - `chunkId`
  - `kbId`
  - `documentId`
  - `documentVersion`
  - `chunkHash`
  - `chunkIndex`
- Search request 不携带 raw question，只携带 query vector、allowed KB ids 和 topK。
- Search response 只需要低敏 `chunkId`，并禁用 vector 返回。
- Native Qdrant API 映射：
  - upsert 使用 `PointStruct.Builder#setVectors(VectorsFactory.vectors(...))` 写入向量；
  - upsert 使用 `PointStruct.Builder#putAllPayload(...)` 写入白名单 payload；
  - search 使用 `SearchPoints.Builder#setWithPayload(...)` include `chunkId`；
  - search 使用 `SearchPoints.Builder#setWithVectors(WithVectorsSelector(enable=false))` 禁用 vector 返回。
- Provider 异常转换为固定错误码：
  - `VECTOR_UPSERT_FAILED`
  - `VECTOR_SEARCH_FAILED`
  - `VECTOR_DELETE_FAILED`
  - `VECTOR_PROVIDER_NOT_CONFIGURED`

## 已接受限制 / 后续风险

- 未运行真实 Qdrant 服务 smoke；本子任务范围为最小 adapter 集成和 fake operations 单测。
- 未实现 Qdrant collection schema 初始化、dimension 校验、health endpoint 或 ops alert。
- `spring-ai-qdrant-store:1.0.8` 传递 `grpc-netty-shaded:1.65.1`，同时项目解析到 `grpc-api:1.75.0`；本轮只记录 dependency tree，不盲目覆盖传递依赖，也不声明 CVE 已关闭。后续应独立做 fresh dependency tree、dependency-check/CVE 复核、gRPC/Netty 依赖升级或风险接受评审。

## Evidence Verdict

PASS for P3-2 real VectorDB adapter minimum integration.
