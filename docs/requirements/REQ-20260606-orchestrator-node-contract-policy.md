# Orchestrator 节点契约与策略显式化需求

## 1. 功能需求

| 编号 | 需求 | 优先级 | 验收方式 |
|---|---|---|---|
| FR-01 | `OrchestratorWorkflowStepResponse` 必须包含节点输入 DTO 名称 | 必须 | API JSON path |
| FR-02 | `OrchestratorWorkflowStepResponse` 必须包含节点输出 DTO 名称 | 必须 | API JSON path |
| FR-03 | `OrchestratorWorkflowStepResponse` 必须包含失败策略 | 必须 | API JSON path |
| FR-04 | `OrchestratorWorkflowStepResponse` 必须包含重试策略 | 必须 | API JSON path |
| FR-05 | `OrchestratorWorkflowStepResponse` 必须包含 `retryable` 布尔值 | 必须 | API JSON path |
| FR-06 | `recentFailedStep` 必须复用同一节点契约字段 | 必须 | 失败 workflow 查询断言 |
| FR-07 | `FAILED RESOURCE_GENERATION` 的 `nextActions` 必须包含 `RETRY_WORKFLOW` | 必须 | 失败资源 workflow 查询断言 |
| FR-08 | `FAILED RAG_QA` 和 `FAILED ANSWER_SUBMISSION` 的 `nextActions` 必须包含 `RESUBMIT_ORIGINAL_REQUEST` | 必须 | 失败 RAG/答题 workflow 查询断言 |

## 2. 约束

- 契约映射保存在后端代码中，作为当前可执行 workflow 的静态策略矩阵。
- 不新增数据库 schema。
- 不把敏感原始输入放入失败 summary 或 workflow envelope。
- Controller 只暴露响应，契约映射逻辑放在 Service 层。

## 3. 安全需求

- RAG question 和 answer 原文不得因为契约字段而出现在响应或持久化失败证据中。
- endpoint retry 仅对可从 envelope 安全重建的 `RESOURCE_GENERATION` 开放。
- 不对前端展示不可用的 retry 动作。
