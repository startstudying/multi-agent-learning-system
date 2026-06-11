# Structured Request Logging

## 使用场景

当实现或审查后端 HTTP 请求日志、访问日志、过滤器日志、错误码日志、可观测性字段、日志脱敏时使用。

## 核心规则

1. 请求日志应位于 `common` 横切边界，不散落到各 Controller 或 Service。
2. 先由 trace filter 建立 `traceId`，再由 auth filter 建立用户上下文，最后由 request logging filter 在请求完成时记录日志。
3. 日志字段必须白名单构造，默认只允许：
   - `traceId`
   - `userId`
   - `route`
   - `status`
   - `latencyMs`
   - `errorCode`
4. `route` 优先使用框架 route pattern，缺失时使用 `METHOD path`，不得包含 query string。
5. `errorCode` 应由统一异常处理写入内部 request attribute，日志过滤器只读取，不在过滤器里猜业务错误码。
6. `X-Trace-Id` 属于不可信输入，进入日志前必须限制长度、字符集和控制字符。

## 禁止记录

- request body / response body
- 完整 query string
- `Authorization`、`Cookie`、`Set-Cookie`
- Prompt、模型输出、RAG chunk 原文
- 学生答案原文、教师评语原文
- 原始异常消息、堆栈正文、SQL/provider 错误原文

## 测试建议

- 成功请求记录 `OK`。
- `ApiException` 记录真实 `ErrorCode`。
- validation 记录 `VALIDATION_ERROR`。
- unexpected exception 记录 `INTERNAL_ERROR` 且不含原始异常消息。
- GET query 参数中的敏感文本不进入日志。
- 非法 `X-Trace-Id` 被替换，且不进入响应头或日志。
- 全量测试确认新过滤器不破坏 `@WebMvcTest`。
