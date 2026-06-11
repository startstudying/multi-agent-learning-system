# CONTEXT-20260611 后端架构后续增强计划收口

## 当前边界

本任务只做后端架构后续增强计划收口和 opt-in smoke 实测化。不新增生产功能、不新增依赖、不改 API/DB/frontend。

## 已确认事实

- `EmbeddingService` 已读取 registry provider。
- `TokenBudgetGateService` 已接入 `AiModelGateway` 调用前降级路径。
- `OpsAlertPersistenceService` 与 V22 `ops_alert_record` 已存在。
- `QdrantVectorHealthProbe` 已接入 `/api/health`，支持 collection 存在性和 expected dimension 检查。
- `QdrantVectorExternalSmokeTest` 和 `ModelProviderExternalSmokeTest` 原先是 placeholder，本轮升级为真实 opt-in smoke。

## 测试策略

- 默认测试验证编译、单元路径、RBAC、默认跳过。
- 真实外部 smoke 必须人工设置环境变量后运行，不进入默认测试。

## 后续不在本轮解决

- 工业级 PDF layout/table/TOC。
- native/cloud OCR。
- DashScope / Spring AI Alibaba 专用 provider。
- 出站 URL allowlist 生产策略。
- user/course/agent 维度 token budget gate。
