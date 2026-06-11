# RUN - P3-5 结构化请求日志 Subagent 分析

状态：已完成（2026-06-07）

## 1. Subagent Decision

Use Subagents: Yes

Reason: 本切片涉及过滤器顺序、错误码传递和日志脱敏，虽然实现只落在后端 `common` 模块，但安全边界需要并行审查。

Parallelism Level: L1

Implementation Mode: Single Codex

## 2. Backend Expert Report 摘要

Backend Expert 结论：

- 当前 `TraceFilter` 只负责 `X-Trace-Id` 和 `TraceContext` 生命周期，没有请求完成日志。
- `DevAuthFilter` 在 `TraceFilter` 之后运行并在 finally 清理用户上下文，因此不能直接在 `TraceFilter` finally 里取 `userId`。
- 推荐新增独立 `StructuredRequestLoggingFilter`，排序在 `DevAuthFilter` 之后。
- `GlobalExceptionHandler` 应写内部 request attribute，把真实 `ErrorCode` 传给日志过滤器。
- 日志字段限定为 `traceId/userId/route/status/latencyMs/errorCode`。
- 不建议新增 JSON encoder、日志表、Micrometer、深度健康检查或告警。

关键风险：

- 过滤器顺序错误会把真实用户记录成 `dev_user`。
- 过滤器按 HTTP status 猜测 `errorCode` 会与业务错误码漂移。
- 日志扩展到 body/query/header 会引入敏感信息风险。

## 3. Security & Quality Report 摘要

Security & Quality 结论：

- `X-Trace-Id` 直接接收并回写，若进入日志需要限制长度、字符集和控制字符。
- `X-User-Id` 是开发态不可信 header，日志可记录规范化后的 `userId`，但不能作为审计级身份来源。
- 日志只能记录稳定 `ErrorCode`，不能记录 exception message、stack trace、SQL/provider 原文。
- GET RAG query 可能包含 `question`，日志 route 不能包含 query string。
- 需要测试 body/query/header/异常消息不进入日志。

实现前最低安全策略已经收敛为字段白名单：

- 允许：`traceId`、`userId`、`route`、`status`、`latencyMs`、`errorCode`
- 禁止：请求体、响应体、完整 query string、敏感 Header、Prompt、模型输出、RAG chunk 原文、学生答案原文、原始异常消息和堆栈正文
- `X-Trace-Id` 仅接受 `^[A-Za-z0-9._:-]{1,128}$`；非法值由服务端生成替代值。

## 4. 集成结论

采用 Backend Expert 建议的最小实现：

```text
TraceFilter
-> DevAuthFilter
-> StructuredRequestLoggingFilter
-> Controller / GlobalExceptionHandler
```

主 Codex 单线程实现，避免多代理修改同一 `common` 文件。
