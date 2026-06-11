# EVIDENCE-20260611 F1 子任务：model provider registry backend

## Verdict: PASS

## 实现摘要

- 新增 `model_provider` 表（Flyway V21）
- 新增 `ModelProviderService` / `ModelProviderController`（`/api/admin/model-providers`）
- API Key 使用 AES-GCM 加密；列表/详情只返回 `apiKeyMasked`
- `AiModelGateway` 在 env provider 之前优先读取 enabled + default registry provider
- 支持 provider code：`deepseek` / `mimo` / `dashscope` / `openai` / `custom`

## 测试

Focused:

```text
ModelProviderSecretCodecTest: 2/2
ModelProviderServiceTest: 5/5
ModelProviderControllerTest: 2/2
AiModelGatewayTest: 14/14 (含 registry 优先路径)
SchemaConvergenceMigrationTest: +1 v21 case
```

Full backend:

```text
mvn test
Tests run: 613, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 安全说明

- 不在代码/文档/memory 中存储用户聊天截图里的真实 API Key
- production/staging 需配置 `MODEL_PROVIDER_ENCRYPTION_KEY`

## 后续

- F1 前端 Admin 配置页（对齐截图 UI）
- Embedding 路径读取 registry provider
- F2 provider smoke / F3 Qdrant / F4 parser / F5 权限 / F6 告警 / F7 预算门禁
