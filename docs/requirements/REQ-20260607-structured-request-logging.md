# REQ - 结构化请求日志

状态：已完成（2026-06-07）

## 1. 需求说明

系统必须在每个 HTTP 请求结束时输出一条结构化访问日志，作为 P3-5 可观测性的最小生产化能力。日志只记录排障必要的白名单元数据，不记录请求内容或敏感信息。

## 2. 功能需求

1. 请求日志必须包含 `traceId`。
2. 请求日志必须包含当前 `userId`。
3. 请求日志必须包含路由标识 `route`，优先使用 Spring MVC best matching pattern，缺失时使用 `METHOD path`。
4. 请求日志必须包含 HTTP `status`。
5. 请求日志必须包含非负 `latencyMs`。
6. 请求日志必须包含 `errorCode`。
7. 成功请求的 `errorCode` 为 `OK`。
8. `ApiException` 请求的 `errorCode` 等于异常中的 `ErrorCode`。
9. 参数校验异常的 `errorCode` 为 `VALIDATION_ERROR`。
10. 未预期异常的 `errorCode` 为 `INTERNAL_ERROR`。
11. `X-Trace-Id` 只接受安全字符集和长度；非法值必须替换为服务端生成的新 `traceId`。

## 3. 安全需求

1. 日志不得记录请求体、响应体、完整 query string。
2. 日志不得记录 `Authorization`、`Cookie`、`Set-Cookie`、API Key 等 Header。
3. 日志不得记录 Prompt、模型输出、RAG chunk 原文、学生答案原文或教师评语原文。
4. 未知异常日志不得包含原始异常消息或堆栈正文。
5. 日志字段必须通过白名单构造。

## 4. 约束

- 不新增依赖。
- 不修改 API 响应契约。
- 不新增数据库表或字段。
- 不改变现有 `TraceFilter` 的 `X-Trace-Id` 行为。
- 保留合法 `X-Trace-Id` 的透传行为；非法 `X-Trace-Id` 不再透传。
- 不改变现有 `DevAuthFilter` 的开发态用户上下文行为。

## 5. 验收条件

- 定向测试覆盖结构化日志字段。
- 全量后端测试通过或明确说明不可运行原因。
- P3-5 TODO 中结构化日志项更新为完成。
- Evidence 和 Acceptance 文档记录测试命令和结果。
