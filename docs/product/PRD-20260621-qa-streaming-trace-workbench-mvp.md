# PRD-20260621 QA streaming and trace workbench MVP

## 1. 背景

P0 已完成 AI QA 的隐私保护、低敏记忆上下文、结构化回答 runtime 和基础 verifier/eval gate。当前缺口是：学生提问时前端仍主要消费 RAG streaming，而不是 `/api/ai/qa` 的结构化质量结果；教师/管理员也无法从同一个工作台直接看到 answer mode、verification、source policy、tool calls 与 trace 线索。

## 2. 目标

提供 P1 最小可用版本：

- 后端新增 `/api/ai/qa/stream`，用现有 `QaRuntime` 返回流式事件。
- 前端生产/预发安全流式通道改用 AI QA stream，而不是只用 RAG stream。
- 学生工作台展示 `verification`、source policy、uncertainty、quality flags、tool call 数量和 traceId。
- 保持前端不接触 LLM API、API key 或 provider 参数。

## 3. 用户价值

- 学生能在同一问答界面看到回答是否通过基础质量校验。
- 教师/管理员后续可基于同一结构化字段建设质量工作台。
- 工程上把 P0 的 verifier 结果真正接入用户可见路径，避免只停留在后端 DTO。

## 4. 非目标

- 不新增 DB schema。
- 不新增依赖。
- 不实现真实 token-by-token 模型流式输出；本 MVP 与现有 RAG stream 一样，使用 transport streaming，最终答案由 runtime 一次生成后发送。
- 不实现完整教师/管理员独立质量看板。
- 不实现批量 eval runner 或 baseline vs candidate UI。
- 不实现 P2 memory lifecycle。

## 5. 成功标准

- `POST /api/ai/qa/stream` 返回 `text/event-stream`，包含 `status`、`token`、`done`、`error` 事件。
- `done` 事件包含完整低敏 `AiQaResponse` 关键字段：answer、sources/citations、traceId、verification、qualityFlags、toolCalls。
- 前端生产/预发模式使用 `/api/ai/qa/stream`，请求体携带 question/kbIds/topK/courseId/answerMode，不把 question/kbIds 放到 URL。
- 学生工作台能展示 QA verification verdict 与 gate policy。
- focused backend、frontend regression、build 验证通过。
