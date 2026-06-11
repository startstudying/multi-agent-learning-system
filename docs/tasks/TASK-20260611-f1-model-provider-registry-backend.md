# TASK-20260611 F1 子任务：model provider registry backend

Status: Done.

## Goal

实现 Admin 侧多模型供应商注册后端：DB 持久化、API Key 加密存储、`AiModelGateway` 优先读取 default provider，支持 DeepSeek / MiMo 等 OpenAI-compatible 端点。

## Size

M（单后端模块，含 migration + API + gateway 集成）。

## Context Pack

### Allowed Files

- `backend/src/main/resources/db/migration/V21__model_provider_registry.sql`
- `backend/src/main/java/com/learningos/agent/**`
- `backend/src/main/java/com/learningos/config/ModelProviderProperties.java`
- `backend/src/main/java/com/learningos/LearningOsApplication.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/learningos/agent/**`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `docs/tasks/TASK-20260611-f1-model-provider-registry-backend.md`
- `docs/evidence/EVIDENCE-20260611-f1-model-provider-registry-backend.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## Acceptance

- [x] V21 `model_provider` 表
- [x] Admin CRUD + set-default + test-connection API
- [x] API Key AES-GCM 加密，响应只返回 masked key
- [x] `AiModelGateway` 优先使用 registry default provider
- [x] 测试通过
