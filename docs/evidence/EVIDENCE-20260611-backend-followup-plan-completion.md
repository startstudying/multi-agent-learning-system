# EVIDENCE-20260611 后端架构后续增强计划收口

## 变更摘要

- 保存 Agent/RAG、Backend、Security & Quality 三份专家 subagent 报告和集成评审。
- 将 `QdrantVectorExternalSmokeTest` 从占位测试升级为真实 opt-in Qdrant health / collection / expected dimension 检查。
- 将 `ModelProviderExternalSmokeTest` 从占位测试升级为真实 opt-in OpenAI-compatible chat endpoint 检查。
- 将 `MysqlMigrationSmokeTest` 的 latest migration 从 V20/20 对齐到 V22/22，并增加 `model_provider`、`ops_alert_record` 对象断言。
- 更新 `backend-architecture-todolist.md` 后续增强收口状态。

## 验证命令

```powershell
cd backend
mvn --% -Dtest=SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest,QdrantVectorExternalSmokeTest,ModelProviderExternalSmokeTest,QdrantVectorIndexAdapterTest,RagVectorConfigurationTest,EmbeddingServiceTest,AiModelGatewayTest,TokenBudgetGateServiceTest,BusinessPermissionMatrixRegressionTest test
```

## 验证结果

- PASS：56 run, 0 failures, 0 errors, 3 skipped。
- Skipped 项为 opt-in smoke：
  - `MysqlMigrationSmokeTest`
  - `QdrantVectorExternalSmokeTest`
  - `ModelProviderExternalSmokeTest`

## Opt-in Smoke 命令

```powershell
cd backend
$env:QDRANT_SMOKE_ENABLED='true'
$env:RAG_VECTOR_QDRANT_HOST='127.0.0.1'
$env:RAG_VECTOR_QDRANT_PORT='6334'
$env:RAG_VECTOR_QDRANT_COLLECTION='learning_os_chunks'
$env:RAG_VECTOR_QDRANT_EXPECTED_DIMENSION='1536'
mvn --% -Dtest=QdrantVectorExternalSmokeTest test
```

```powershell
cd backend
$env:MODEL_PROVIDER_SMOKE_ENABLED='true'
$env:MODEL_PROVIDER_BASE_URL='https://example-compatible-endpoint/v1'
$env:MODEL_PROVIDER_API_KEY='***'
$env:MODEL_PROVIDER_CHAT_MODEL='model-name'
mvn --% -Dtest=ModelProviderExternalSmokeTest test
```

```powershell
cd backend
mvn --% -Dlearningos.mysql.smoke=true -Dtest=MysqlMigrationSmokeTest test
```

## 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 仅测试和文档变更 |
| Frontend rules | PASS | 本轮未修改前端 |
| Agent / RAG rules | PASS | VectorDB 仍经 `VectorIndexAdapter`，默认不外连 |
| Security | PASS with follow-up | external smoke opt-in；outbound URL allowlist 后续独立任务 |
| API / Database | PASS | 未新增 API / migration；MySQL smoke 跟进 V22 |

## 剩余风险

- 真实 Qdrant/provider/MySQL smoke 依赖本地或 CI 环境。
- 外部 provider test 和 webhook 的统一 outbound URL allowlist 仍需后续硬化。
- DashScope 专用 SDK、native/cloud OCR、分域 token budget gate 不在本轮实现。
