# PLAN - P3-3 真实模型 Provider Adapter

## 1. 追踪

- PRD：`docs/product/PRD-20260608-real-model-provider-adapter.md`
- REQ：`docs/requirements/REQ-20260608-real-model-provider-adapter.md`
- SPEC：`docs/specs/SPEC-20260608-real-model-provider-adapter.md`

## 2. 实施阶段

| 阶段 | 说明 | 关联任务 | 状态 |
|---|---|---|---|
| 1 | 专家分析与集成决策 | TASK-01 | 完成 |
| 2 | 依赖审查与 workflow 文档 | TASK-01 | 完成 |
| 3 | TDD：Chat provider adapter | TASK-01 | 完成 |
| 4 | TDD：Embedding provider adapter | TASK-01 | 完成 |
| 5 | 回归测试、证据、验收、记忆更新 | TASK-01 | 完成 |

## 3. 文件变更清单

| 文件 | 操作 | 阶段 | 负责人 |
|---|---|---|---|
| `backend/pom.xml` | 修改 | 3 | Main Codex |
| `backend/src/main/resources/application.yml` | 修改 | 3/4 | Main Codex |
| `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java` | 修改 | 3 | Main Codex |
| `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java` | 修改 | 4 | Main Codex |
| `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java` | 修改 | 3 | Main Codex |
| `backend/src/test/java/com/learningos/rag/application/EmbeddingServiceTest.java` | 修改 | 4 | Main Codex |
| Workflow / security / evidence / acceptance / memory docs | 新增/修改 | 2/5 | Main Codex |

## 4. 依赖

- 前置条件：`docs/security/DEPENDENCY-REVIEW-20260608-real-model-provider-adapter.md` 已批准。
- 新增依赖：`org.springframework.ai:spring-ai-starter-model-openai`。
- 版本调整：`spring-ai.version=1.0.8`。

## 5. 风险评估

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| provider bean 缺失但业务返回成功 placeholder | 形成虚假 AI 成功证据 | provider 配置完整时 fail closed。 |
| raw provider error 泄露 secret/prompt/chunk | 安全事故 | 只记录固定错误码。 |
| provider 输出非 JSON | 脏数据入库 | gateway JSON parse + schema validation。 |
| Embedding 成功但 VectorDB 仍 noop | 误以为语义检索完成 | 文档明确 VectorDB 不在本切片；TODO 仍保留 P3-2 真实向量检索增强。 |
| 新依赖带来安全/兼容风险 | 构建或运行失败 | Maven dependency tree、focused/full tests，版本保持 1.0.x patch 线。 |

## 6. 回滚策略

- 将 `AI_MODEL_PROVIDER` 设回 `none` 可恢复 deterministic/noop 本地行为。
- 如依赖导致构建问题，撤回 `spring-ai-starter-model-openai` 和 BOM 升级。
- 本切片不改 DB schema，无 migration rollback。

## 7. 测试策略

```powershell
cd backend
mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest test
mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest,ResourceGenerationControllerTest,RagQueryServiceTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest test
mvn test
```

依赖检查：

```powershell
cd backend
mvn dependency:tree
mvn compile
```

## 8. Subagent 计划

| 专家 | 是否需要 | 职责 | 状态 |
|---|---|---|---|
| Dependency Expert | 是 | Spring AI provider starter 选型、版本、依赖风险 | 已完成 |
| Spec Architect | 是 | gateway/embedding/vector 边界设计 | 已完成 |
| Security & Quality | 是 | secret、raw error、usage/cost、权限前置风险 | 已完成 |
| Integration Reviewer | 是 | 合并专家结论，限定最小切片 | 已完成，由 Main Codex 执行 |

并行级别：L1 并行分析。实现模式：Single Codex 串行 TDD。

## 9. 架构漂移检查

实施前检查：PASS。

- Controller 仍只处理 HTTP。
- 业务服务仍调用 gateway/service，不调用 SDK。
- Backend owns AI calls。
- 新依赖已审查。
- 不改 API / DB / frontend。

实施后检查：PASS。

- Spring AI SDK 只出现在 `AiModelGateway` / `EmbeddingService` 边界内。
- `ResourceGenerationService`、`RagQueryService`、`VectorIndexAdapter`、Controller、frontend、DB migration 未被本切片修改。
- provider raw error 映射为固定安全错误码，不泄露 secret、prompt、chunk 或 base URL。
- 默认 `AI_MODEL_PROVIDER=none` 保持本地 deterministic/noop 行为。

## 10. 验证结果

| 命令 | 结果 | 证据 |
|---|---|---|
| `mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest test` | PASS | 18 tests，0 failures，0 errors |
| `mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest,ResourceGenerationControllerTest,RagQueryServiceTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest test` | PASS | 53 tests，0 failures，0 errors |
| `mvn dependency:tree` | PASS | Spring AI OpenAI starter dependency tree resolved |
| `mvn compile` | PASS | BUILD SUCCESS |
| `mvn test` | PASS | 357 run，0 failures，0 errors，1 skipped |

## 11. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | APPROVED |
