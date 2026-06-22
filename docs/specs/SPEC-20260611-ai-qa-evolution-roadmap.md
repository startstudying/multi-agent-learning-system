# SPEC - AI 问答能力六阶段演进路线

## 1. Architecture Summary

目标架构保持模块化单体边界：

```text
Vue AI 问答工作台
-> Backend REST / Stream API
-> QA Application Service
-> Orchestrator / Agent Runtime
-> Model Gateway / Tool Gateway / RAG Service
-> Domain Services / Repositories
-> MySQL / MinIO / VectorDB / Optional external tools
```

前端只表达用户意图、显示状态和消费后端事件。后端负责模型、工具、RAG、权限、Trace、成本和安全。

## 1.1 Model Target And Web-Quality Mechanism

本路线的目标模型基线为 OpenAI `gpt-5.5`。目标不是复刻 GPT Web 端会员权益，而是在项目后端中复刻可治理的质量机制：

- 好模型：优先使用 `gpt-5.5` 承载复杂推理、代码、工具和多 Agent 问答。
- 更高 reasoning effort：由后端把 `answerMode` 映射为受控 `reasoning.effort`。
- 更干净上下文：按 system / developer / memory / conversation / retrieved_context / task_contract 分层构造上下文，避免把无关历史、原始工具输出和敏感信息直接塞入 prompt。
- 工具检索：RAG、文件检索、Web search、代码分析都必须走后端 Service/Tool 边界。
- 多轮自检：`EXPERT` 或高风险任务允许 generator -> verifier -> optional revision once。
- 结构化输出：优先要求模型输出符合后端 DTO / JSON schema 的结果，再转换为前端响应。
- 评测迭代：用引用质量、审查通过率、用户反馈、token/cost、失败率持续校准模式策略。

## 2. Version Design

### V1 普通问答

建议新增或收敛到统一入口：

```text
POST /api/ai/qa
```

请求字段建议：

```json
{
  "question": "如何理解 SQL JOIN 重复行？",
  "answerMode": "FAST",
  "kbIds": ["kb_java_backend"],
  "courseId": "course_java_backend",
  "requestId": "req_qa_001"
}
```

响应字段建议：

```json
{
  "answer": "回答正文",
  "answerMode": "FAST",
  "reasoningSummary": null,
  "sources": [],
  "traceId": "trc_001",
  "workflowId": null,
  "toolCalls": []
}
```

### V2 三档模式

`answerMode` 由后端解释为执行策略：

| Mode | 策略 |
|---|---|
| `FAST` | 低延迟，较短上下文，较少工具调用，默认映射 `reasoning.effort=low`，适合快速答疑。 |
| `THINKING` | 中等深度，允许检索和摘要，默认映射 `reasoning.effort=medium`，返回安全思考摘要。 |
| `EXPERT` | 更完整的检索、审查和引用，默认映射 `reasoning.effort=high`，允许受控工具调用与更严格审查。 |

前端不得发送模型供应商密钥、真实模型内部参数或 prompt 文本。后端可把模式映射到 prompt version、temperature、topK、maxToolRounds 等受控策略。

`xhigh` 只能作为后台配置或管理员策略启用，不作为普通前端枚举直接暴露；启用前必须有 token/cost 预算、超时和降级策略。

### V3 思考摘要

字段名：`reasoningSummary`。

规则：

- 只返回简短、面向用户的解释摘要。
- 不返回原始 chain-of-thought。
- 不包含系统提示词、私有文档全文、工具原始输入输出、密钥、内部评分细节。
- 可写入 trace summary，但仍需脱敏。
- 对接 OpenAI reasoning summary 时，后端只取安全可展示摘要并二次过滤；前端字段仍统一为 `reasoningSummary`。

### V4 SSE 流式输出

生产路径优先复用已有正式流式模式：

```text
POST /api/ai/qa/stream
Accept: text/event-stream
```

事件建议：

| Event | Data |
|---|---|
| `status` | `stage`、`message`、可选 `traceId` |
| `token` | `text` |
| `citation` | 可选增量 citation |
| `tool_call` | 脱敏工具调用摘要 |
| `done` | 完整 answer、reasoningSummary、sources、traceId、workflowId、latencyMs |
| `error` | 安全错误码和用户可读消息 |

如果先复用 `POST /api/rag/query/stream`，必须明确它是 RAG 问答流式，不承诺覆盖无资料通用问答和多 Agent 工具流。

### V5 工具调用

工具必须是后端受控能力：

| Tool | Boundary |
|---|---|
| WebSearchTool | 通过后端服务调用外部搜索；需要 allowlist、超时、摘要脱敏和依赖评审。 |
| FileRetrievalTool | 通过 RAG/Document Service 检索用户有权限的文件和课程资料。 |
| CodeAnalysisTool | 只分析授权范围内的代码/作业文件；禁止任意文件系统读取。 |

所有工具调用必须：

- 由 Agent Runtime 调度。
- 经 Service 层执行。
- 不直接访问 Mapper/Repository。
- 记录 `agent_tool_call`。
- 有超时、最大调用次数和失败降级。

### V6 多 Agent

建议角色：

| Agent | Responsibility | Output |
|---|---|---|
| Planner Agent | 判断问题类型、选择模式、规划工具与检索步骤 | `QaPlan` |
| Retrieval Agent | 执行 RAG/文件检索/可选联网搜索 | `RetrievalBundle` |
| Reviewer Agent | 检查来源、权限、安全、幻觉和答案质量 | `ReviewDecision` |
| Generator Agent | 生成最终答案、思考摘要和引用说明 | `QaAnswer` |

工作流：

```text
qa_request
-> workflow_start
-> planner
-> retrieval
-> generator
-> reviewer
-> optional_revision_once
-> done / failed
```

最大循环建议：

- `maxAgentRounds = 2`
- `maxToolCalls = 5`
- `maxRevisionRounds = 1`

## 3. Data and Trace

优先复用既有治理记录：

- `agent_task`
- `agent_trace`
- `agent_tool_call`
- `model_call_log`
- `token_usage_log`
- `kb_query_log`
- `source_citation`

如未来发现现有 schema 无法表达统一 QA workflow，再单独提出 DB migration 设计，不在本轮路线规划中预设迁移。

## 4. Frontend Spec

学生端问答区应包含：

- 模式选择：`Fast`、`Thinking`、`Expert`。
- 输入框和发送按钮。
- 流式答案正文。
- 思考摘要区域。
- 引用来源区域。
- 工具调用摘要时间线。
- Trace / workflow 状态入口。
- 错误、空结果、无来源、取消中、完成状态。

教师/管理端后续可复用 Agent Trace 详情展示工具调用和审查结果。

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS planned | Controller 只做 HTTP adapter；业务和工具在 Service/Agent 层。 |
| Frontend rules | PASS planned | 前端不直接访问 LLM、搜索、文件系统或代码分析工具。 |
| Agent / RAG rules | PASS planned | 引用、Trace、max rounds、工具记录均为硬要求。 |
| Security | PASS planned | 工具能力需要权限、脱敏、超时和依赖评审。 |
| API / Database | PASS planned | 本轮不改 API/DB；后续实现切片必须更新 SPEC。 |
