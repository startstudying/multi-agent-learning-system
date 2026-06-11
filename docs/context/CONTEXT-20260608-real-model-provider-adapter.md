# Context Pack - P3-3 真实模型 Provider Adapter

## 当前任务

完成 `docs/planning/backend-architecture-todolist.md` 中 P3-3 “用 Spring AI/Spring AI Alibaba 接入真实 Chat/Embedding 模型”的最小 Spring AI OpenAI-compatible provider adapter 切片。

## 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`（如存在）

## 关联文档

- PRD：`docs/product/PRD-20260608-real-model-provider-adapter.md`
- REQ：`docs/requirements/REQ-20260608-real-model-provider-adapter.md`
- SPEC：`docs/specs/SPEC-20260608-real-model-provider-adapter.md`
- PLAN：`docs/plans/PLAN-20260608-real-model-provider-adapter.md`
- TASK：`docs/tasks/TASK-20260608-real-model-provider-adapter.md`
- Dependency Review：`docs/security/DEPENDENCY-REVIEW-20260608-real-model-provider-adapter.md`
- Expert Reports：
  - `docs/subagents/runs/RUN-20260608-p3-3-real-model-provider-dependency.md`
  - `docs/subagents/runs/RUN-20260608-p3-3-real-model-provider-architecture.md`
  - `docs/subagents/runs/RUN-20260608-p3-3-real-model-provider-security.md`
  - `docs/subagents/runs/RUN-20260608-p3-3-real-model-provider-integration.md`

## 已选 Skills

- `feature-development-workflow`
- `spring-ai-agent-backend`
- `educational-rag-pipeline`
- `agent-trace-governance`
- `model-gateway-boundary`
- `rag-embedding-vector-adapter`
- `dependency-review`
- `architecture-drift-check`
- `test-driven-development`
- `verification-before-completion`
- `Confidence Check`

## Subagent 计划

### 是否启用 Subagent

是。

### 原因

任务涉及 Backend、Agent、RAG、Security、Dependency、Architecture，且用户明确要求专家 subagent 并行开发。

### 任务复杂度

| 影响模块数 | 涉及 Agent/RAG | 涉及安全 |
|---|---|---|
| 3+ | 是 | 是 |

### 选中的专家

- Dependency Expert：依赖选型和版本风险。
- Spec Architect：gateway/embedding/vector 边界分析。
- Security & Quality：secret、provider error、usage/cost、安全边界审查。
- Integration Reviewer：主 Codex 合并专家结果。

### 并行级别

- [x] L1 - 仅并行分析
- [ ] L2 - 并行设计
- [ ] L3 - worktree 并行实现

### 执行模式

Single Codex 串行实现。Subagent 不并行修改代码。

## 关联代码区域

- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java`
- `backend/src/test/java/com/learningos/rag/application/EmbeddingServiceTest.java`

## 允许修改的文件

- `backend/pom.xml`
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java`
- `backend/src/test/java/com/learningos/rag/application/EmbeddingServiceTest.java`
- 本任务相关 workflow / subagent / security / evidence / acceptance / memory / changelog / retrospective / planning 文档。

## 禁止修改的文件

- `frontend/**`
- `backend/src/main/resources/db/migration/**`
- `backend/src/main/java/com/learningos/*/api/**`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`
- `docs/superpowers/**`
- 任何真实 secret 文件。

## 测试命令

```powershell
cd backend
mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest test
mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest,ResourceGenerationControllerTest,RagQueryServiceTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest test
mvn test
```

依赖验证：

```powershell
cd backend
mvn dependency:tree
mvn compile
```

## 任务边界

本次只完成 OpenAI-compatible Spring AI Chat/Embedding adapter 最小切片。DashScope、VectorDB、真实 provider smoke、provider 管理 UI、RAG answer generation 改造不在本任务范围。

## Confidence Check

- No duplicate implementations：PASS。当前无真实 `ChatModel` / `EmbeddingModel` adapter。
- Architecture compliance：PASS。接入点保持在 gateway/service 内部。
- Official documentation verified：PASS。已核对 Spring AI 1.0.8 `ChatModel.call(Prompt)`、`EmbeddingModel.call(EmbeddingRequest)`、OpenAI auto-configuration properties。
- Working OSS implementation referenced：PARTIAL。使用 Spring AI 官方 source/javadoc 与 Maven metadata，不复制外部项目代码。
- Root cause identified：PASS。缺口是 provider adapter 装配缺失。

Confidence：0.95。可以进入 TDD 实现。

## 执行结果

- `AiModelGateway` 已通过 Spring AI OpenAI-compatible `ChatModel` 接入真实 chat provider adapter。
- `EmbeddingService` 已通过 Spring AI OpenAI-compatible `EmbeddingModel` 接入真实 embedding provider adapter。
- 默认 `AI_MODEL_PROVIDER=none` 保持本地 deterministic/noop，不外呼。
- provider 配置完整但缺 Spring AI bean 时 fail closed，不返回 placeholder 成功。
- `application.yml` 已显式禁用未使用的 OpenAI image/audio/moderation auto-config，避免缺少对应 API key 影响启动和测试。
- 已完成 focused、adjacent、dependency tree、compile、full backend Maven 验证，证据见 `docs/evidence/EVIDENCE-20260608-real-model-provider-adapter.md`。
