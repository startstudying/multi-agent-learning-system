# TASK - 结构化请求日志

状态：已完成（2026-06-07）

## Task 1

补齐后端 HTTP 请求完成结构化日志。

### 目标

- 每个请求完成后输出一条白名单字段日志。
- 日志包含 `traceId/userId/route/status/latencyMs/errorCode`。
- 成功、业务异常、校验异常、未知异常均有明确 `errorCode`。
- 不记录请求体、响应体、敏感 Header、完整 query string、Prompt/RAG 原文。
- 非法 `X-Trace-Id` 不得进入日志。

### 交付物

- `StructuredRequestLoggingFilter`
- `StructuredRequestLoggingFilterTest`
- `GlobalExceptionHandler` 内部 errorCode attribute 写入
- `TraceFilter` 合法 traceId 透传、非法 traceId 替换
- 本切片 workflow 文档、证据、验收和记忆更新

### Done Criteria

- [x] 成功请求日志包含 `errorCode=OK`
- [x] `ApiException` 日志包含真实业务 `errorCode`
- [x] 校验异常日志包含 `VALIDATION_ERROR`
- [x] 未知异常日志包含 `INTERNAL_ERROR`
- [x] 日志不包含未知异常原始消息
- [x] 日志不包含 query string 和敏感 header
- [x] 非法 `X-Trace-Id` 被替换且不进入日志
- [x] 定向测试通过
- [x] `mvn test` 通过或记录不可运行原因
