# P3-2 子任务：real VectorDB adapter minimum integration 专家并行报告

## 任务定位

- 父项：P3-2 RAG 索引生产化
- 子任务：real VectorDB adapter minimum integration
- 日期：2026-06-10
- 并行级别：L1/L2，专家并行分析与设计；实现由 Main Codex 单线集成

## Skill Selection Report

### Task Type

RAG / VectorDB 生产化增强；涉及后端依赖、外部服务配置、索引与检索链路、安全审查。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 强制 Project Memory、Skill Selection、Size、文档、测试、Evidence/Acceptance 流程 |
| educational-rag-pipeline | 约束 RAG indexing/retrieval/citation/permission filter |
| spring-ai-agent-backend | 约束 Spring Boot / Spring AI 后端封装边界 |
| rag-embedding-vector-adapter | 使用项目已有 embedding/vector adapter 规则 |
| security-review | 新增外部连接、凭据、endpoint、日志脱敏风险 |
| dependency-review | 新增 Spring AI Qdrant / Qdrant Java client 依赖 |
| test-driven-development | 实现前先 RED，再 GREEN |

### Missing Skills

无。现有项目技能足够覆盖本切片。

### GitHub Research Needed

No。只使用官方 Spring AI、Qdrant、Maven Central / dependency tree 核验；不做泛 GitHub 参考。

### New Project-Specific Skill To Create

暂不新增。完成后如形成稳定 Qdrant adapter 规则，可更新 `rag-embedding-vector-adapter`。

## Size Classification

- Size: L
- Reason:
  - 新增真实 VectorDB 依赖与外部网络连接。
  - 影响 RAG indexing 与 online retrieval 两条链路。
  - 需要 dependency review / security review / architecture drift check。
  - 涉及 3+ 模块：配置、RAG application、VectorDB provider、测试、文档。
- Required Documents:
  - PRD / REQ / SPEC / PLAN / TASK / CONTEXT
  - Dependency review under `docs/security/`
  - Evidence / Acceptance after implementation
- Can Skip:
  - 泛 GitHub reference。
  - 真实 Qdrant/Testcontainers smoke，作为后续 opt-in 子任务。
- Upgrade Trigger:
  - 若引入 Docker/Testcontainers、DB schema、REST API、frontend、health endpoint 或部署拓扑变更，需要另开子任务或扩展 PLAN。

## Subagent Decision

- Use Subagents: Yes
- Reason: L 级任务，涉及 RAG、依赖安全、测试验证。
- Parallelism Level: L1/L2
- Selected Subagents:
  - Dependency/Security Expert
  - RAG Architecture Expert
  - Test/Verification Expert
- Implementation Mode: Parallel analysis/design + Single Codex implementation

## 专家结论集成

### 依赖 / 安全专家

结论：

- 推荐使用 `org.springframework.ai:spring-ai-qdrant-store:1.0.8`，不使用 `spring-ai-starter-vector-store-qdrant` 自动配置。
- 原因：项目已有 `VectorIndexAdapter` 端口，手动封装更容易保持默认 Noop、缺配置 fail-fast、避免全局 `VectorStore` 意外接管。
- 官方 Spring AI 1.0.8 Qdrant 文档显示 Qdrant store 需要 Qdrant client bean，并通过 `QdrantVectorStore.builder(...)` 创建。
- Maven 本地核验：
  - `spring-ai-qdrant-store:1.0.8` 可解析。
  - `spring-ai-starter-vector-store-qdrant:1.0.8` 可解析，但本切片不采用。
  - `spring-ai-qdrant-store:1.0.8` POM 传递 `io.qdrant:client:1.13.0`。
- 需记录 gRPC/Netty CVE 风险：专家提示 `grpc-netty-shaded:1.65.1` 可能受 `CVE-2025-55163 / GHSA-prj3-ccx8-p6x4` 影响；需要 `mvn dependency:tree` 和后续安全扫描确认是否覆盖。

### RAG 架构专家

结论：

- 真实 adapter 必须挂在 `VectorIndexAdapter` 后面，不允许业务层直接依赖 Qdrant SDK 或 Spring AI `VectorStore`。
- 推荐新增实现包 `com.learningos.rag.vector`：
  - `QdrantVectorIndexAdapter`
  - `RagVectorProperties`
  - 必要的 client factory / port wrapper
- MySQL 仍为事实源；VectorDB 只存低敏 point payload 和向量。
- Upsert payload 只允许：
  - `chunkId`
  - `kbId`
  - `documentId`
  - `documentVersion`
  - `chunkHash`
  - `chunkIndex`
- 禁止进入 VectorDB：
  - raw chunk content
  - question / answer / prompt
  - storage key
  - user id
  - secret / API key
  - raw provider response
- Search 必须：
  - 下推 `allowedKbIds` filter。
  - 返回 `VectorSearchHit(chunkId, score)`。
  - 服务层继续回表并按 `allowedKbIds` 二次过滤。

### 测试专家

结论：

- RED 应先覆盖：
  - 默认 disabled 仍 Noop，不连接外部服务。
  - enabled 且配置完整才启用真实 adapter。
  - upsert 映射 vector payload，且不含 raw content/secret。
  - search 下推 `allowedKbIds` / `topK`，不携带 raw question。
  - provider upsert/search 失败返回安全错误码。
- 不依赖真实 Qdrant；fake client / fake gateway 即可验证 adapter contract。
- Testcontainers / 真 Qdrant smoke 属于后续 opt-in，不进入默认 `mvn test`。

## 主线程官方核验

已核验：

- Spring AI 1.0.8 Qdrant 文档存在 `spring-ai-qdrant-store`、`QdrantClient`、`QdrantVectorStore.builder(...)`、metadata filtering 说明。
- 本地 Maven 成功解析：
  - `org.springframework.ai:spring-ai-qdrant-store:1.0.8`
  - `org.springframework.ai:spring-ai-starter-vector-store-qdrant:1.0.8`
  - `io.qdrant:client:1.15.0`
- `spring-ai-qdrant-store:1.0.8` jar 中存在 `QdrantVectorStore`，其 public API 偏向 `Document` / `SearchRequest`，不直接接收项目已生成的 `EmbeddingVector` payload。

设计推论：

- 本切片仍引入官方 `spring-ai-qdrant-store`，但真实 adapter 优先使用其传递的 Qdrant native client，而不是让业务层直接调用 Spring AI `VectorStore`。
- 若直接使用 `QdrantVectorStore`，可能重新引入 raw text / internal embedding，与当前 `VectorUpsertRequest` 不携带 raw content 的安全 contract 冲突。

## 集成决策

1. 使用项目自有 `VectorIndexAdapter` 作为唯一 RAG vector port。
2. 引入 `spring-ai-qdrant-store`，不引入 starter 自动配置。
3. 默认配置保持 disabled/noop。
4. 显式启用且配置完整时创建 `QdrantVectorIndexAdapter`。
5. 本切片不改变 REST API、DB schema、frontend、Agent/Orchestrator。
6. 本切片不做真实 Qdrant smoke；后续可单独做 opt-in smoke。
7. Dependency/security review 必须记录传递依赖和 CVE 风险。
