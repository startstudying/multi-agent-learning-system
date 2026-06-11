# Orchestrator 运行期失败证据持久化需求

## 1. 追溯

- PRD：`docs/product/PRD-20260606-orchestrator-runtime-failure-evidence.md`
- 需求编号：REQ-20260606-orchestrator-runtime-failure-evidence

## 2. 功能需求

| 编号 | 需求 | 优先级 | 验收方式 |
|---|---|---|---|
| FR-01 | 当 Orchestrator task 创建后，下游服务抛出 `ApiException` 时，系统必须保留 `agent_task`。 | 必须 | RAG_QA 权限失败后 repository 可查 task |
| FR-02 | 运行期失败必须将 `agent_task.status` 置为 `FAILED`。 | 必须 | 断言 task status |
| FR-03 | 运行期失败必须追加一个 `FAILED agent_trace`，且保留原 `workflow_start`。 | 必须 | 断言 trace step 顺序 |
| FR-04 | 失败后 `GET /api/orchestrator/workflows/{workflowId}` 必须能返回失败状态、失败 step、失败统计和 `INSPECT_TRACE/RETRY_WORKFLOW`。 | 必须 | MockMvc 查询断言 |
| FR-05 | 运行期失败必须原样抛出业务错误码，不得转成成功响应。 | 必须 | RAG_QA 未授权 KB 返回 403 |
| FR-06 | 前置校验失败不得创建 durable workflow task。 | 必须 | 现有 invalid payload 测试继续通过 |
| FR-07 | 失败摘要必须脱敏，不写入完整学生答案、完整问题原文或私有文档内容。 | 必须 | 测试断言失败摘要不包含问题原文 |

## 3. 运行期失败定义

本轮只处理 task 创建后发生的 `ApiException`：

- RAG 权限失败。
- RAG 内容安全失败。
- Assessment 内容安全失败。
- 下游服务业务冲突。

不处理 task 创建前的校验失败：

- workflowType 不合法。
- learnerId 与当前用户不一致。
- payloadJson 无效。
- payload 缺必填字段。

## 4. 状态规则

```text
RUNNING -> FAILED
```

失败 workflow 查询结果必须满足：

```text
status = FAILED
recentFailedStep.status = FAILED
traceSummary.failedSteps >= 1
nextActions contains INSPECT_TRACE and RETRY_WORKFLOW
```

## 5. 安全需求

- 失败 step summary 使用错误类别和错误码，不记录完整用户输入。
- 权限失败不得写入 `kb_query_log` 或 `source_citation` 成功证据。
- 原始 `ApiException.errorCode` 必须保留。
