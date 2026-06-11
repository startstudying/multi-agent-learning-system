# P3-3 真实模型 Provider 依赖专家分析报告

## 1. 任务定位

- 任务项：`docs/planning/backend-architecture-todolist.md` 未完成项 P3-3「用 Spring AI/Spring AI Alibaba 接入真实 Chat/Embedding 模型」。
- 本报告角色：P3-3 Model Provider Dependency Expert，并行专家分析。
- 本轮约束：只读代码为主；仅写入本报告；不修改代码、不修改 `backend/pom.xml`、不写入任何 secret。
- 任务类型：依赖接入评估 / 后端 AI provider 边界分析。

## 2. Skill Selection Gate

| 项目 | 结论 |
|---|---|
| Task type | 依赖评估 + 后端 AI provider 边界分析 |
| Selected skills | `feature-development-workflow`、`model-gateway-boundary`、`rag-embedding-vector-adapter`、`dependency-review`、`security-review`、`architecture-drift-check` |
| 选择原因 | 本项涉及真实模型 SDK / Spring AI starter 接入、模型网关边界、Embedding/RAG 索引边界、secret 与 provider 错误脱敏、架构漂移风险 |
| Missing skills | 无必须新增技能；后续可抽取 `real-model-provider-adapter` 项目技能 |
| GitHub research needed | 本轮不需要深度 GitHub 代码参考；依赖选型需官方文档和 Maven/GitHub 元数据核验 |
| Subagent level | L1 Parallel Analysis；不进入实现 |

## 3. 当前代码与依赖现状

### 3.1 `backend/pom.xml`

当前已有 Spring AI BOM：

```xml
<spring-ai.version>1.0.3</spring-ai.version>
...
<artifactId>spring-ai-bom</artifactId>
```

但当前没有真实 provider starter 依赖：

- 未发现 `spring-ai-starter-model-openai`。
- 未发现 `spring-ai-alibaba-starter-dashscope`。
- 未发现 `ChatClient`、`ChatModel`、`EmbeddingModel`、`DashScope`、`OpenAi` 在 `backend/src/main/java` 中的使用。

结论：项目已有 Spring AI BOM 管理入口，但尚未真正接入 Spring AI / Spring AI Alibaba provider 客户端。当前模型调用仍是项目自研 deterministic / placeholder gateway。

### 3.2 `AiModelGateway`

文件：`backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`

现状：

- 所有资源生成模型调用通过 `AiModelGateway`，符合 P3-3 已完成边界要求。
- `generateStructured(...)` 根据 `learning-os.ai-model.provider` / `chatModel` 返回 deterministic 或 `gateway-placeholder`。
- 明确 `externalCall=false`，当前不会访问外部 provider。
- 已有结构化输出校验：`agent-resource-v1` 必须包含非空 `resources[]`，字段包括 `title/type/modality/markdownContent/citationSummary/safetyStatus`。
- 已有 retry：最大 2 次。
- 已有 provider 白名单归一化：`none/openai/dashscope/anthropic/gemini/mock`，未知值归为 `other`。
- 已有安全失败码：`MODEL_PROVIDER_ERROR`、`STRUCTURED_OUTPUT_INVALID`，`ModelCallFailedException` 不携带 raw cause。
- 已有 metrics：`LearningOsMetrics.recordModelCall(...)`。

依赖接入影响：

- Chat provider 最小接入点应只在 `AiModelGateway` 内部或其下沉 adapter 中完成。
- 业务服务、Agent、Orchestrator、ResourceGenerationService 不应直接注入 `ChatClient` / `ChatModel` / provider SDK。
- 真实 provider 输出必须先在 gateway 做 JSON 解析和 schema 校验，再进入业务持久化与 trace / model log。

### 3.3 `EmbeddingService`

文件：`backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`

现状：

- 默认 `provider=none` 或未配置 `embeddingModel` 时返回 `EmbeddingStatus.DISABLED`。
- 配置了 provider + embedding model 时，当前仍不调用外部服务，而是返回：
  - `EmbeddingStatus.PROVIDER_ERROR`
  - `EMBEDDING_PROVIDER_NOT_CONFIGURED`
- `currentModelVersion()` 已能返回真实 embedding model 配置名。

依赖接入影响：

- Embedding provider 最小接入点是 `EmbeddingService`。
- 当前 `EmbeddingBatchResult` 只记录状态、模型版本、chunk 数、延迟、错误码，不承载向量数组。
- 由于 `VectorIndexAdapter` 当前 upsert DTO 只携带 chunk 引用，不携带 embedding vector，若要真实向量入库，必须另外设计 vector 传递边界或 adapter 内部重新 embedding query/chunk。该点会扩大切片。

### 3.4 `VectorIndexAdapter`

文件：

- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/application/NoopVectorIndexAdapter.java`

现状：

- 默认 `NoopVectorIndexAdapter`，`isEnabled=false`。
- `deleteDocument/upsert/search` 均返回 disabled。
- `VectorSearchRequest` 已携带 `allowedKbIds/question/topK`。
- `ChunkService` 已在 vector hit 回表后按 `allowedKbIds` 二次过滤。
- `IndexService` 已有 `EMBEDDING`、`VECTOR_UPSERT` 阶段与失败清理机制。

依赖接入影响：

- 如果本切片只接入真实 Chat + Embedding provider，而不接 VectorDB，可以保留 `NoopVectorIndexAdapter`。
- 如果目标包含真实 vector retrieval，则必须另起 VectorDB dependency review，不建议和 Chat/Embedding provider 同一最小切片合并。

### 3.5 配置结构

文件：

- `backend/src/main/java/com/learningos/config/AiModelProperties.java`
- `backend/src/main/resources/application.yml`
- `backend/.env.example`

现状：

```yaml
learning-os:
  ai-model:
    provider: ${AI_MODEL_PROVIDER:none}
    chat-model: ${AI_CHAT_MODEL:}
    embedding-model: ${AI_EMBEDDING_MODEL:}
```

配置结构目前只有 provider/model 名称，没有 Spring AI 官方 starter 通常需要的 API key/base URL 绑定项。后续应只使用环境变量绑定 secret，不写入配置文件明文。

### 3.6 测试结构

重点现有测试：

- `AiModelGatewayTest`：覆盖 deterministic、provider 归一化、retry、失败脱敏、结构化输出校验、metrics。
- `EmbeddingServiceTest`：覆盖 disabled 与“已配置但未实现 provider 时安全失败”。
- `NoopVectorIndexAdapterTest`：覆盖 noop 与 request 不暴露 raw chunk content。
- `IndexServiceEmbeddingVectorTest`：覆盖 embedding provider failure 时 task/document 失败、清理 chunks、错误码安全。
- `ChunkServiceVectorRetrievalTest`：覆盖 vector search allowed-KB 下推、forbidden hit 二次过滤、provider error fallback。

## 4. 依赖评估

### 4.1 当前已有依赖判断

| 项 | 当前状态 |
|---|---|
| Spring AI BOM | 已有，`org.springframework.ai:spring-ai-bom:1.0.3` |
| Spring AI OpenAI starter | 未添加 |
| Spring AI Alibaba DashScope starter | 未添加 |
| 真实 provider API key 配置 | 未添加；只有项目自有 `AI_MODEL_PROVIDER/AI_CHAT_MODEL/AI_EMBEDDING_MODEL` |
| VectorDB client | 未添加；当前 noop |

### 4.2 候选路径

| 路径 | 依赖候选 | 适用性 | 维护 / License 证据 |
|---|---|---|---|
| Spring AI 官方 OpenAI-compatible provider | `org.springframework.ai:spring-ai-starter-model-openai` | 最小、官方、适合先接通 Chat + Embedding；若目标 provider 兼容 OpenAI API，可减少生态复杂度 | Spring AI GitHub：约 8.9k stars、Apache-2.0、2026-06-08 有 push；Maven metadata 显示 starter 与 BOM 有 1.0.x、1.1.x、2.0 RC 版本 |
| Spring AI Alibaba DashScope | `com.alibaba.cloud.ai:spring-ai-alibaba-starter-dashscope` | 适合阿里云百炼 / DashScope / Qwen 场景；但需单独确认 Spring Boot 3.5.7、Spring AI 版本矩阵与 starter 兼容关系 | Alibaba Spring AI Alibaba GitHub：约 9.9k stars、Apache-2.0、2026-06-07 有 push；Maven metadata 显示 1.0.0.x、1.1.x、2.0 milestone 版本 |
| 直接调用 provider SDK | 例如 DashScope SDK / OpenAI SDK | 不推荐作为 P3-3 主路径 | 会绕开 Spring AI 统一抽象，增加 gateway 适配和安全审查负担 |

### 4.3 推荐

推荐最小依赖路线：

1. 优先选一个 provider 完成单一最小切片，不同时接 OpenAI 与 DashScope。
2. 若没有明确国内云厂商绑定，优先 Spring AI 官方 `spring-ai-starter-model-openai`，因为项目已声明 Spring AI BOM，且 Spring AI 是项目既定技术栈。
3. 若目标明确为 Qwen / DashScope，则采用 Spring AI Alibaba DashScope starter，但必须先做独立 dependency review，确认：
   - 与当前 Spring Boot `3.5.7` 兼容。
   - 与 Spring AI BOM 版本兼容，不出现双 BOM 冲突。
   - starter 传递依赖中 HTTP client、JSON、netty/reactor 版本是否与现有 Spring Boot 管理冲突。

### 4.4 版本与安全风险

当前 `spring-ai.version=1.0.3` 不应直接视为可上线版本：

- Maven metadata 显示 Spring AI BOM 已有 `1.0.4` 到 `1.0.8`、`1.1.x`、`2.0.0-RC1` 等后续版本。
- 接入真实模型前应查 Spring 官方 security advisories、Maven dependency tree 和 `mvn dependency:tree`，判断是否需要先从 `1.0.3` 升到同一稳定线的更新 patch 版本。
- 若引入 VectorStore/VectorDB 相关 Spring AI 模块，还要单独核查 VectorStore filter / SQL / metadata expression 类 CVE 与依赖版本。

本轮不改 POM，因此以上为后续 dependency review 必查项，不是本轮实现动作。

## 5. 推荐最小切片边界

### 切片 A：真实 Chat provider adapter（推荐先做）

目标：

- 让 `AiModelGateway.generateStructuredWithRetry(...)` 可在显式配置 provider 时调用真实 Chat provider。
- 仍保留 `provider=none` deterministic fallback。
- 不改变外部 API、数据库 schema、Agent/Orchestrator 业务服务调用方式。

边界：

- 只允许 `AiModelGateway` 或新增 `agent/application/provider` 内部 adapter 使用 `ChatClient` / `ChatModel`。
- 真实模型返回必须解析为 `Map<String,Object>` 后复用现有 `validateStructuredOutput(...)`。
- provider exception 统一映射为 `MODEL_PROVIDER_ERROR`，不得把 raw provider message、request id、endpoint、API key、prompt、chunk 写入 DB/log/trace。
- token usage 如 provider 可返回则映射到 `ModelResponse.TokenUsage`；否则继续用估算，但需在报告/证据说明。

建议实现策略：

- 用 Spring DI 注入可选 `ChatModel` 或 `ChatClient.Builder`，仅当 provider 启用且 bean 存在时调用。
- bean 不存在但 provider 配置了时安全失败，不再假装 `gateway-placeholder` 成功。
- 对 `agent-resource-v1` 要求模型输出严格 JSON；解析失败计入 `STRUCTURED_OUTPUT_INVALID` 或 `MODEL_PROVIDER_ERROR`，建议前者用于 schema/JSON 不合规。

### 切片 B：真实 Embedding provider（可与 A 分开，建议第二步）

目标：

- 让 `EmbeddingService.embedDocumentChunks(...)` 调用真实 Spring AI `EmbeddingModel`。
- 仍保留未配置时 `DISABLED`。
- provider 失败时保留现有 index failure / chunk 清理语义。

边界：

- 只在 `EmbeddingService` 或其下沉 adapter 使用 `EmbeddingModel`。
- 不在 `metadataJson`、task error、日志中写 raw vector 或 raw provider response。
- 当前 `EmbeddingBatchResult` 没有承载向量，若不接 VectorDB，则真实 embedding 只能验证 provider 可用性和记录模型状态，不能产生真正 vector retrieval 收益。

重要判断：

- 若产品目标是“真实 embedding + keyword/RRF 仍可运行”，B 可以最小化完成。
- 若目标是“真实语义检索可用”，必须新增切片 C：VectorDB adapter，另做依赖和 schema/deployment review。

### 切片 C：真实 VectorDB adapter（不建议并入本 P3-3 最小切片）

需要额外决策：

- 选型：pgvector / Redis Vector / Milvus / Elasticsearch / OpenSearch / Spring AI VectorStore。
- schema：chunk id、kb id、document id/version、embedding dimension、metadata filtering。
- 安全：allowed KB 过滤必须在 vector query 下推并回表二次过滤。
- 迁移：如果使用数据库扩展或独立服务，需要运维和 smoke test。

## 6. 后续允许修改文件建议

本轮未修改以下文件；这是后续 PLAN / Context Pack 可采用的最小白名单。

### 切片 A 允许修改

- `backend/pom.xml`：仅在 dependency review 批准后添加一个 provider starter 或调整 Spring AI patch 版本。
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- 可新增：`backend/src/main/java/com/learningos/agent/application/provider/*`
- `backend/src/main/java/com/learningos/config/AiModelProperties.java`
- `backend/src/main/resources/application.yml`
- `backend/.env.example`：只增加环境变量名，不写真实值。
- `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java`
- 可新增：`backend/src/test/java/com/learningos/agent/application/provider/*Test.java`
- `docs/security/DEP-*.md`：新增依赖审查报告。

### 切片 B 允许修改

- `backend/pom.xml`：同上，需先过 dependency review。
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- 可新增：`backend/src/main/java/com/learningos/rag/application/provider/*`
- 如必须传递向量，才允许修改：
  - `backend/src/main/java/com/learningos/rag/application/EmbeddingBatchResult.java`
  - `backend/src/main/java/com/learningos/rag/application/VectorUpsertRequest.java`
  - `backend/src/main/java/com/learningos/rag/application/VectorChunkReference.java`
- `backend/src/test/java/com/learningos/rag/application/EmbeddingServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceEmbeddingVectorTest.java`

### 不允许在最小切片中修改

- Controller / API DTO。
- 数据库 migration，除非 VectorDB/schema 切片明确批准。
- `ResourceGenerationService`、`OrchestratorWorkflowService` 业务流程，除非发现 gateway contract 不足且 SPEC 更新。
- 前端。
- 任何 secret、真实 API key、provider raw request/response 记录。

## 7. 主要风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 误把配置了 provider 但未成功注入 ChatModel 的状态当成功 | 产生假的 AI 成功证据 | provider enabled + bean missing 必须安全失败；`provider=none` 才 deterministic |
| 真实 provider 输出非严格 JSON | 资源生成失败或脏数据入库 | gateway 层 JSON parse + schema validation；失败只写安全错误码 |
| raw provider error / prompt / chunk / API key 泄露 | 安全事故 | exception 不透传 cause；日志、trace、model_call_log 只写白名单错误码 |
| Spring AI BOM 1.0.3 过旧 | 安全/兼容风险 | dependency review 中核查官方 advisory，优先稳定 patch 线；不要盲目用 milestone/RC |
| Spring AI Alibaba 与 Spring AI 官方 BOM 混用冲突 | 启动失败或隐性依赖冲突 | 固定一种 provider 路径；跑 `mvn dependency:tree`；确认官方版本矩阵 |
| Embedding 接通但 VectorDB 仍 noop | 用户以为语义检索已可用 | 文档和 health/metadata 明确 `vectorIndexStatus=DISABLED`；语义检索另起切片 |
| token usage 与 cost 不准确 | 成本治理误差 | provider 有 usage 时映射；没有 usage 时标记估算来源，并保留现有估算 |
| CI/本地无 API key | 测试不稳定 | 单元测试 mock `ChatModel/EmbeddingModel`；真实 provider smoke 默认跳过、显式 opt-in |

## 8. 必须测试命令

后续实现最小切片后，至少运行：

```powershell
cd backend
mvn test -Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest
mvn test -Dtest=EmbeddingServiceTest,IndexServiceEmbeddingVectorTest,IndexServiceTest,ChunkServiceVectorRetrievalTest,NoopVectorIndexAdapterTest
mvn test
```

若修改依赖，必须补充：

```powershell
cd backend
mvn dependency:tree
mvn compile
```

若新增 opt-in 真实 provider smoke，应满足：

- 默认 CI 不需要 secret，不执行真实外呼。
- 仅在显式环境变量存在时执行。
- smoke 输出不得打印 API key、raw prompt、raw provider response。

## 9. 依赖审查清单

在修改 `pom.xml` 前必须创建 `docs/security/DEP-YYYYMMDD-real-model-provider.md`，至少包含：

1. 选择 OpenAI-compatible 还是 DashScope，不能同时扩大到多个 provider。
2. Maven 坐标、版本、license、传递依赖摘要。
3. 与 Spring Boot `3.5.7`、Java 21、Spring AI BOM 的兼容性。
4. 是否需要升级 `spring-ai.version=1.0.3`。
5. 官方 security advisory / CVE 检查结果。
6. secret 管理方式：仅环境变量，不进 `.env.example` 真实值，不进日志/trace/DB。
7. fallback 策略：provider disabled、bean missing、timeout、rate limit、schema invalid。
8. 测试策略：mock 单测 + opt-in smoke。

## 10. 结论

当前项目尚未真正接入 Spring AI / Spring AI Alibaba provider，只是预置了 Spring AI BOM 和项目自有 `AiModelGateway` / `EmbeddingService` / `VectorIndexAdapter` 边界。

推荐后续按两个最小切片推进：

1. 先做真实 Chat provider adapter：只动 gateway/provider 配置和测试，保证资源生成模型调用真实外呼、结构化校验、失败脱敏、model_call_log/metrics 仍闭环。
2. 再做真实 Embedding provider：只动 `EmbeddingService` 边界和 RAG 索引测试；VectorDB/语义检索另起依赖审查，不并入本切片。

本轮不建议直接改 `pom.xml`。下一步应先完成 dependency review，明确 provider 路线与 Spring AI 版本线，再进入 SPEC / PLAN / TASK / Context Pack。

## 11. Sources

- Spring AI GitHub：`https://github.com/spring-projects/spring-ai`（2026-06-08 API 查询：Apache-2.0，约 8.9k stars，最近 push 为 2026-06-08）
- Spring AI Maven metadata：`https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-bom/maven-metadata.xml`
- Spring AI OpenAI starter Maven metadata：`https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-starter-model-openai/maven-metadata.xml`
- Spring AI Alibaba GitHub：`https://github.com/alibaba/spring-ai-alibaba`（2026-06-08 API 查询：Apache-2.0，约 9.9k stars，最近 push 为 2026-06-07）
- Spring AI Alibaba DashScope starter Maven metadata：`https://repo1.maven.org/maven2/com/alibaba/cloud/ai/spring-ai-alibaba-starter-dashscope/maven-metadata.xml`
