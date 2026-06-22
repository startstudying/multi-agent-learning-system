# REQ - AI QA Slice 1 统一问答合同

## 1. Skill Selection Report

### Task Type

新功能实现切片 / Frontend + Backend API linkage / 模型策略合同 / 安全 reasoningSummary。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 用户要求执行路线计划，必须按 M 级开发工作流推进。 |
| spring-boot-architecture | 后端新增统一 QA Controller / Service / DTO，需遵守分层。 |
| api-contract-design | 本切片核心是统一请求/响应合同。 |
| model-gateway-boundary | `gpt-5.5`、`reasoning.effort` 和安全模型策略必须收敛在后端网关/策略边界。 |
| agent-trace-governance | 响应必须保留 `traceId`，模型/策略证据需可审计。 |
| security-review | 不暴露原始 chain-of-thought、prompt、模型密钥或任意模型参数。 |
| vue3-component-design | 前端需要模式选择和安全摘要展示。 |
| ai-streaming-ui | 本切片复用现有问答体验，为后续流式统一保留状态展示。 |
| test-generator | 需要后端 API/策略测试和前端交互测试。 |

### Missing Skills

无。

### GitHub Research Needed

No。父路线已确认官方 OpenAI `gpt-5.5` 指南；本切片不新增 SDK、依赖或外部工具。

### New Project-Specific Skill To Create

暂不创建。若后续形成可复用的 QA 策略网关模式，再沉淀项目技能。

## 2. Size Classification

Size: M

Reason:

- 影响后端统一 QA API/DTO/Service 和前端学生问答入口。
- 改变前后端可见行为，但不新增 DB schema、依赖、外部工具、WebSearch 或多 Agent runtime。
- 可用 focused backend/frontend tests 验证。

Required Documents:

- `docs/requirements/REQ-20260611-ai-qa-unified-contract.md`
- `docs/specs/SPEC-20260611-ai-qa-unified-contract.md`
- `docs/plans/PLAN-20260611-ai-qa-unified-contract.md`
- `docs/tasks/TASK-20260611-ai-qa-unified-contract.md`
- `docs/context/CONTEXT-20260611-ai-qa-unified-contract.md`

Can Skip:

- 不新建 PRD，沿用父路线 `PRD-20260611-ai-qa-evolution-roadmap.md`。
- 不做依赖评审、DB migration、WebSearch/Tool Gateway 设计。

Upgrade Trigger:

- 如果实现过程中需要新增 DB 表、真实 Responses API SDK、外部工具、流式统一端点或多 Agent 编排，立即停止并升级为 L 或新的 M 子任务。

## 3. Subagent Decision

Use Subagents: Yes, documented L1/L2 analysis only.

Reason:

- 本切片涉及前端 + 后端合同，并触及模型策略/安全摘要。
- 文件边界仍较小，代码实现由单 Codex 集成，避免并行修改同一文件。

Parallelism Level: L1/L2 documented analysis.

Selected Subagents:

- Backend Expert
- Frontend Expert
- Security & Quality
- Integration Reviewer

Implementation Mode: Single Codex implementation after integrated documentation.

Report:

- `docs/subagents/runs/RUN-20260611-ai-qa-unified-contract.md`

## 4. Functional Requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | 后端提供 `POST /api/ai/qa` 统一问答入口。 | Must |
| FR-02 | 请求支持 `question`、`answerMode`、`kbIds`、`courseId`、`topK`、`requestId`。 | Must |
| FR-03 | `answerMode` 枚举为 `FAST`、`THINKING`、`EXPERT`，默认 `THINKING`。 | Must |
| FR-04 | 后端将 `FAST=low`、`THINKING=medium`、`EXPERT=high` 映射为受控 `reasoning.effort` 策略。 | Must |
| FR-05 | 响应返回 `answer`、`answerMode`、`reasoningEffort`、`reasoningSummary`、`sources`、`traceId`、`workflowId`、`toolCalls`。 | Must |
| FR-06 | `reasoningSummary` 只能是安全、短摘要，不包含原始 chain-of-thought、prompt、工具原始输出、密钥或私有上下文全文。 | Must |
| FR-07 | 本切片先复用现有 RAG 查询能力生成答案和 citations，不新增工具调用。 | Must |
| FR-08 | 无来源时沿用 RAG no-source 降级语义，不编造课程依据。 | Must |
| FR-09 | 前端学生问答区增加 `FAST` / `THINKING` / `EXPERT` 模式选择。 | Must |
| FR-10 | 前端展示 `reasoningSummary`、`answerMode`、`traceId` 和引用来源。 | Must |
| FR-11 | 前端不得发送模型供应商、API key、prompt 文本或任意内部模型参数。 | Must |

## 5. Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-01 | 不新增依赖。 |
| NFR-02 | 不新增 DB schema。 |
| NFR-03 | Controller 只处理 HTTP、鉴权上下文和 envelope，业务策略在 Service 层。 |
| NFR-04 | API 响应继续使用既有 `ApiResponse<T>` envelope。 |
| NFR-05 | 所有权限仍由后端 RAG service / course scope 逻辑执行，不依赖 prompt。 |
| NFR-06 | 本切片不要求真实外部模型调用；`gpt-5.5` 作为目标模型配置与策略合同进入后续模型网关兼容层。 |

## 6. Open Questions

- 当前 Spring AI `ChatModel` 适配是否能完整表达 Responses API 的 `reasoning.effort` 和 summary，留给后续模型网关增强任务确认。
- 统一 QA stream endpoint 是否在 Slice 2 实现；本切片只做非流式统一合同，并让前端当前提问可使用该合同。
