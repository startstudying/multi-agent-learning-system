# AI QA 无资料降级回答规格

## API 合同

接口：`POST /api/ai/qa`

请求不变。

响应新增字段：

```json
{
  "sourceStatus": "COURSE_GROUNDED | GENERAL_FALLBACK",
  "sourcePolicy": "COURSE_RAG | NO_COURSE_SOURCE_FALLBACK"
}
```

字段语义：

- `COURSE_GROUNDED`：答案来自课程 RAG 资料，`sources` 至少包含一个引用。
- `GENERAL_FALLBACK`：RAG 未检索到可靠课程资料，答案为后端受控通用回答，`sources=[]`。
- `COURSE_RAG`：严格课程资料路径。
- `NO_COURSE_SOURCE_FALLBACK`：先尝试 RAG，失败后退回普通问答路径。

## 后端逻辑

`AiQaService.answer` 保持先调用 `RagQueryService`。

判断 no-source：

- `ragResponse.retrieval() != null && ragResponse.retrieval().noSource()`；或
- `ragResponse.sources()` 为空。

no-source 时：

- 使用 `AiQaService` 内部的通用回答构造器生成可展示答案。
- 该构造器当前不接新模型供应商，只提供规则化解释文本，作为后续 model gateway 接入点。
- 保留 `traceId`，让本次 RAG 尝试仍可追踪。
- `sources` 返回空列表。

有来源时：

- 直接返回 RAG answer 和 sources。

## 前端逻辑

`StudentDashboard.vue`：

- 存储 `sourceStatus` 和 `sourcePolicy`。
- `GENERAL_FALLBACK` 时展示通用回答提示。
- `COURSE_GROUNDED` 时展示课程资料回答提示。
- 默认空状态文案更新为“优先基于课程资料；没有资料时给出通用回答并标明来源状态”。

## 架构漂移检查

- Backend layering：Controller 仍只委托 Service，业务逻辑在 `AiQaService`。
- Frontend：仍通过 shared `apiRequest` 调用后端，无直接 LLM API。
- Agent/RAG：纯 RAG 回答仍要求 sources；AI QA 降级明确无课程来源，不伪造引用。
- Security：无新依赖、无密钥、无 prompt 权限控制。
- API/Database：仅新增响应字段，无 DB 变更。
