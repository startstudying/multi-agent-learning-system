# TASK - P3-3 真实模型 Provider Adapter

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260608-real-model-provider-adapter.md`
- SPEC：`docs/specs/SPEC-20260608-real-model-provider-adapter.md`
- 任务编号：TASK-20260608-real-model-provider-adapter

## 2. 目标

在不修改外部 API、DB schema、frontend 的前提下，通过 Spring AI 官方 OpenAI-compatible starter 接入真实 Chat/Embedding provider adapter，并保持默认 `provider=none` 本地兼容。

## 3. 范围

### 纳入范围

- 新增 Spring AI OpenAI starter。
- 升级 Spring AI BOM 到 `1.0.8`。
- `AiModelGateway` 接入 `ChatModel` adapter。
- `EmbeddingService` 接入 `EmbeddingModel` adapter。
- 增加 TDD 测试覆盖成功、bean 缺失、raw error 脱敏、schema invalid。
- 更新 evidence / acceptance / changelog / memory / TODO。

### 排除范围

- 不接入 DashScope。
- 不接入 VectorDB。
- 不改 RAG answer generation。
- 不改 Controller/API/DB/frontend。
- 不新增真实 provider smoke 测试作为默认测试。

## 4. 允许修改的文件

- `backend/pom.xml`
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java`
- `backend/src/test/java/com/learningos/rag/application/EmbeddingServiceTest.java`
- `docs/product/PRD-20260608-real-model-provider-adapter.md`
- `docs/requirements/REQ-20260608-real-model-provider-adapter.md`
- `docs/specs/SPEC-20260608-real-model-provider-adapter.md`
- `docs/plans/PLAN-20260608-real-model-provider-adapter.md`
- `docs/tasks/TASK-20260608-real-model-provider-adapter.md`
- `docs/context/CONTEXT-20260608-real-model-provider-adapter.md`
- `docs/security/DEPENDENCY-REVIEW-20260608-real-model-provider-adapter.md`
- `docs/subagents/runs/RUN-20260608-p3-3-real-model-provider-*.md`
- `docs/evidence/EVIDENCE-20260608-real-model-provider-adapter.md`
- `docs/acceptance/ACCEPT-20260608-real-model-provider-adapter.md`
- `docs/retrospectives/RETRO-20260608-real-model-provider-adapter.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. 禁止修改的文件

- `frontend/**`
- `backend/src/main/resources/db/migration/**`
- `backend/src/main/java/com/learningos/*/api/**`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`
- `docs/superpowers/**`
- 任何 `.env` 或含真实 secret 的文件

## 6. 实施步骤

1. 创建/更新 workflow 文档和 dependency review。
2. RED：为 `AiModelGateway` 增加 fake `ChatModel` 测试，验证真实 provider adapter 行为。
3. GREEN：修改 `pom.xml` 和 `AiModelGateway`，通过测试。
4. RED：为 `EmbeddingService` 增加 fake `EmbeddingModel` 测试。
5. GREEN：修改 `EmbeddingService`，通过测试。
6. 运行 focused、adjacent、full Maven 测试。
7. 更新 Evidence、Acceptance、Changelog、Memory、TODO、Retro。

## 7. 测试命令

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

## 8. 完成标准

- [x] 代码已实现。
- [x] TDD RED/GREEN 记录在 evidence。
- [x] 测试通过或限制已记录。
- [x] 无架构漂移。
- [x] 仅修改 Context Pack 允许文件。
- [x] 证据文档已创建。
- [x] 验收报告已创建。
- [x] Changelog / Memory / TODO 已更新。

## 9. 状态

| 字段 | 值 |
|---|---|
| 状态 | 完成 |
| 负责人 | Main Codex |
| 开始日期 | 2026-06-08 |
| 完成日期 | 2026-06-08 |
