# REQ-20260621 QA streaming and trace workbench MVP

## 1. 功能需求

### R1 后端 QA stream

- 新增 `POST /api/ai/qa/stream`。
- 请求体复用 `AiQaRequest`。
- 鉴权与 `/api/ai/qa` 一致，使用 `CurrentUserService` 和 Bearer role facts。
- 响应类型为 `text/event-stream`。
- 事件：
  - `status`：阶段状态，至少包含 `INTENT_ROUTING`、`RETRIEVING`、`MEMORY_CONTEXT`、`COMPOSING`、`VERIFYING`。
  - `token`：发送本次最终答案文本。
  - `done`：发送完整低敏 QA 响应字段，并包含 `latencyMs`。
  - `error`：发送固定安全错误码和消息。

### R2 前端 QA stream API

- 新增 frontend API helper，走 shared `streamRequest`。
- 生产/预发模式学生问答使用 `/api/ai/qa/stream`。
- 请求体不得通过 URL 泄露 `question` 或 `kbIds`。
- dev legacy EventSource RAG stream 可以保留，避免扩大迁移面。

### R3 学生工作台质量视图

- 学生工作台保存并展示：
  - `verification.verdict`
  - `verification.gatePolicy`
  - `sourcePolicy`
  - `uncertainty.level`
  - `qualityFlags`
  - `toolCalls.length`
- 质量字段必须有加载/空状态，不得造成文字重叠或横向溢出。

### R4 回归兼容

- 保留 `/api/ai/qa` REST 端点。
- 保留既有 RAG stream / REST fallback 测试语义，除生产/预发安全通道切换到 QA stream 的预期外，不扩大修改。

## 2. 安全需求

- 前端不得直接调用 LLM/provider API。
- 前端不得携带 API key 或 provider model 参数。
- 后端 stream error 不返回 exception message、prompt、provider key、teacher note 或 raw profile snapshot。
- `done` 事件不得包含 raw chain-of-thought。

## 3. 验收需求

- 后端测试覆盖 `/api/ai/qa/stream` SSE 事件与 Bearer role facts。
- 前端测试覆盖生产/预发使用 `/api/ai/qa/stream`，URL 不泄露 question/kbIds，并展示 verification。
- 编译/build 通过。
