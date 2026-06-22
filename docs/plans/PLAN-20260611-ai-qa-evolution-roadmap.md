# PLAN - AI 问答能力六阶段演进路线

## 1. Current State

项目已有能力：

- Vue 学生端 RAG 问答和正式 `POST /api/rag/query/stream` 流式路径。
- Spring Boot RAG 查询、引用、权限过滤、query replay。
- `AiModelGateway`、模型调用日志、token/cost 日志。
- `agent_task`、`agent_trace`、`agent_tool_call` 治理基础。
- Orchestrator 已覆盖部分 `RAG_QA`、`RESOURCE_GENERATION`、`ANSWER_SUBMISSION` 工作流上下文。

缺口：

- 尚未形成统一 `AI QA` 产品入口承载普通问答、模式、思考摘要和多 Agent 版本路线。
- 三档模式的后端策略映射尚未明确。
- `reasoningSummary` 安全摘要合同尚未统一。
- 工具调用虽有治理基础，但联网搜索、代码分析等具体工具未进入安全边界。
- 多 Agent 问答需要新的编排合同，不能直接用自由循环。
- `gpt-5.5` 已作为目标主模型写入路线，但 Slice 1 实现时仍需确认当前 Spring AI / OpenAI-compatible adapter 是否能完整表达 Responses API 的 `reasoning.effort` 和 `reasoning.summary`；不能表达时应在模型网关内做兼容层或显式记录限制。

## 2. Integrated Expert Result

| Expert | Result |
|---|---|
| Product Analyst | 六个版本应作为路线拆分，不应一次实现。V1-V3 是第一可交付闭环。 |
| Frontend Expert | 模式选择、流式状态、思考摘要、引用和工具摘要应成为同一问答工作台能力。 |
| Backend Expert | 统一 QA API 应复用模型网关、RAG Service、Trace 和现有响应 envelope。 |
| Agent/RAG Expert | V5/V6 必须走受控 Tool Gateway 和 Orchestrator；RAG/文件检索必须有 citations。 |
| Security & Quality | 联网搜索和代码分析是高风险工具，必须拆依赖评审、allowlist、权限和脱敏。 |
| Integration Reviewer | 先做文档路线，再拆首个 M 级实现切片：V1-V3 统一 QA 合同。 |

## 3. Recommended Delivery Slices

### Slice 1: V1-V3 统一问答合同

Size: M

Scope:

- 后端新增或收敛统一 QA 请求/响应 DTO。
- 支持 `answerMode` 和 `reasoningSummary`。
- 将 OpenAI 主模型目标设为 `gpt-5.5`，并在后端策略层定义 `FAST=low`、`THINKING=medium`、`EXPERT=high` 的 `reasoning.effort` 映射。
- 优先设计 Responses API 兼容合同；如现有模型网关暂只支持 ChatModel，则先以受控配置记录降级行为，不让前端感知内部差异。
- 前端问答区增加模式选择和思考摘要展示。
- 复用已有模型网关/RAG 能力，不新增工具。

Verification:

- 后端 DTO/service/controller 测试。
- 前端模式选择和摘要渲染测试。
- Trace/model log 基础断言。
- 模型策略单元测试：不同 `answerMode` 映射到正确 effort / summary 策略，且前端不能传入任意模型参数。

### Slice 2: V4 统一流式问答

Size: M

Scope:

- 基于 `POST` streaming，不把问题放入 URL。
- 输出 `status/token/done/error`。
- 前端用 shared `streamRequest` 消费。

Verification:

- SSE/ReadableStream 事件解析测试。
- 生产认证 fail-closed 测试复用现有策略。

### Slice 3: V5 File Retrieval Tool

Size: M

Scope:

- 先做文件检索工具，不先做联网搜索和代码分析。
- 复用 RAG permission filter、source citations、`agent_tool_call`。

Verification:

- 权限矩阵、无来源、工具调用日志、Trace 测试。

### Slice 4: V5 Web Search Tool

Size: L or M after dependency review

Scope:

- 依赖评审、搜索 provider allowlist、超时、缓存、脱敏。
- 搜索结果作为辅助来源，不覆盖课程 RAG 优先级。

Verification:

- mock provider、超时、错误脱敏、敏感数据不入 prompt。

### Slice 5: V5 Code Analysis Tool

Size: L

Scope:

- 明确代码来源和授权边界。
- 禁止任意工作区文件读取。
- 分析结果进入工具摘要和 Trace。

Verification:

- 路径穿越、越权文件、超大文件、敏感内容脱敏测试。

### Slice 6: V6 Multi-Agent QA Workflow

Size: L

Scope:

- Planner / Retrieval / Reviewer / Generator。
- 有状态 Orchestrator workflow。
- max rounds、失败降级、审查不通过最多一次修订。

Verification:

- 状态机测试。
- 工具调用上限测试。
- 审查失败/无来源/模型失败/取消测试。

## 4. Risk Controls

- 新依赖必须先写 `docs/security/` dependency review。
- 工具默认关闭，通过配置和权限显式开启。
- 多 Agent 默认串行编排，先不做并行实现。
- 不返回原始 chain-of-thought，只返回安全摘要。
- 不允许前端直接调用模型、搜索、文件系统或代码分析工具。

## 5. Implementation Order

1. 完成本轮路线规划文档和上下文包。
2. 用户确认后，创建 Slice 1 的 M 级 REQ/SPEC/PLAN/TASK/CONTEXT。
3. 实现 V1-V3。
4. 再按 Slice 2 到 Slice 6 逐步推进。

## 6. Test Strategy

- Backend focused tests: QA service/controller、mode mapping、summary sanitization、trace/model log。
- Backend adjacent tests: RAG query、orchestrator、agent trace、security auth。
- Frontend tests: mode selector、stream state、summary/citation/tool timeline rendering。
- Security tests: no frontend key、no URL leakage、tool permission、tool-call redaction。

## 7. Architecture Drift Result

本轮为文档规划，不改运行时代码。根据基线检查，无实际架构漂移；后续每个实现切片必须重新执行 drift check。
