# PLAN-20260611 后端后续增强 Epic

Status: In Progress。

## 1. 背景

`docs/planning/backend-architecture-todolist.md` MVP 主计划已于 2026-06-11 收口。用户要求 **全做** 文末「后续增强」全部项，并补充 **多模型供应商管理**（DeepSeek、Xiaomi MiMo 等 OpenAI-compatible 端点）。

## 2. 范围

| 序号 | 域 | 目标 | 体量 |
|---|---|---|---|
| F1 | P3-3 多 Provider 管理 | Admin 可配置供应商（名称/备注/官网/baseUrl/API Key），运行时经 `AiModelGateway` 路由；支持 DeepSeek、MiMo、DashScope/OpenAI-compatible | L |
| F2 | P3-3 Provider smoke | 真实外部 provider smoke、MySQL V18 `model_call_log.provider` smoke（opt-in） | M |
| F3 | P3-2 VectorDB 运维 | Qdrant 真实 smoke、collection dimension validation、health/ops、gRPC/Netty 风险 | M |
| F4 | P3-2 工业级 Parser | PDF layout/table/TOC、native/cloud OCR、confidence pipeline、真实渲染页码 | L |
| F5 | P3-4 权限扩展 | 更广 teacher-side 数据域 RBAC + 持续业务矩阵抽样 | M |
| F6 | P3-5 告警深化 | 告警外部推送、通知渠道、持久化 | M |
| F7 | P2-4 预算门禁 | 调用前 token 预算 gate（analytics 治理视图已有） | M |

## 3. 执行顺序（依赖优先）

```text
F1 多 Provider 管理（用户已提供 DeepSeek/MiMo 配置样例）
→ F2 Provider smoke
→ F7 调用前预算门禁（依赖 model call 路径）
→ F3 Qdrant 运维
→ F5 权限矩阵扩展
→ F6 告警持久化/推送
→ F4 工业级 Parser（最大、可最后）
```

## 4. F1 设计要点

### 4.1 数据模型（V21）

表 `model_provider`：

- `id`, `provider_code`（deepseek/mimo/dashscope/openai/custom）
- `display_name`, `remark`, `website_url`
- `base_url`, `chat_model`, `embedding_model`
- `api_key_ciphertext`（仅后端存储，响应用 `***` 掩码）
- `enabled`, `is_default`
- `created_by`, `created_at`, `updated_at`

### 4.2 API（admin-only）

- `GET /api/admin/model-providers`
- `POST /api/admin/model-providers`
- `PUT /api/admin/model-providers/{id}`
- `POST /api/admin/model-providers/{id}/test-connection`（smoke，不落库 raw error）

### 4.3 安全

- API Key 只存后端；前端不回显完整 key
- 禁止写入 docs/memory/changelog
- 使用环境变量 `MODEL_PROVIDER_ENCRYPTION_KEY` 或 dev-only 占位加密
- 业务服务仍只经 `AiModelGateway` / `EmbeddingService`

### 4.4 前端

- Admin 页面：供应商名称、备注、官网、API Key（掩码 + 更新）
- 与截图 UI 对齐，但不硬编码任何真实 key

## 5. 风险

| 风险 | 缓解 |
|---|---|
| 用户在聊天中暴露 API Key | 立即轮换；实现只用 env/DB 加密，不引用聊天内容 |
| DashScope 专用 SDK vs OpenAI-compatible | 优先 OpenAI-compatible baseUrl；DashScope 专用增强作为 F1 子切片 |
| Epic 跨 frontend/backend/DB | 一次只做一个 TASK；F1 先 backend 再 frontend |
| 工业级 Parser 引入大依赖 | F4 独立 dependency review |

## 6. 测试策略

- F1：Service 单测 + Admin Controller MockMvc RBAC + gateway 路由单测
- F2：opt-in `@Tag("external-smoke")` 测试
- F3：Testcontainers Qdrant 或 opt-in integration
- F5：MockMvc 矩阵扩展
- F6/F7：Service 单测 + Controller 测试

## 7. 首个 TASK

`docs/tasks/TASK-20260611-f1-model-provider-registry-backend.md`（待创建）

边界：V21 migration + entity/repository/service + admin API + `AiModelGateway` 读取 default provider；不含前端页面。
