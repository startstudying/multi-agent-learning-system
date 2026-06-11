# SPEC - 结构化请求日志

状态：已完成（2026-06-07）

## 1. 目标

在不改变业务 API、数据库和依赖的前提下，为后端 HTTP 请求补齐结构化请求完成日志，覆盖 `traceId`、`userId`、`route`、`status`、`latencyMs`、`errorCode`。

## 2. 追踪

- PRD：`docs/product/PRD-20260607-structured-request-logging.md`
- REQ：`docs/requirements/REQ-20260607-structured-request-logging.md`
- TODO：`docs/planning/backend-architecture-todolist.md` P3-5
- 架构基线：`docs/architecture/ARCHITECTURE_BASELINE.md`

## 3. 设计

### 3.1 请求日志过滤器

新增 `StructuredRequestLoggingFilter`，位置在 `backend/src/main/java/com/learningos/common/trace/`。

排序：

```text
TraceFilter: Ordered.HIGHEST_PRECEDENCE
DevAuthFilter: Ordered.HIGHEST_PRECEDENCE + 1
StructuredRequestLoggingFilter: Ordered.HIGHEST_PRECEDENCE + 2
```

原因：

- `TraceFilter` 先生成并绑定 `traceId`。
- `DevAuthFilter` 再建立 `UserContextHolder`。
- 请求日志过滤器在 chain 返回后的 `finally` 中记录日志，此时 `DevAuthFilter` 的 finally 尚未清理用户上下文，可以读取真实 `userId`。

### 3.1.1 traceId 安全约束

`TraceFilter` 对客户端传入的 `X-Trace-Id` 做最小安全校验：

```text
^[A-Za-z0-9._:-]{1,128}$
```

合法值继续透传；非法、空白、超长或包含控制字符的值丢弃，由服务端生成 UUID。这样结构化日志不会写入换行、控制字符或超长 traceId。

### 3.2 errorCode 传递

在 `GlobalExceptionHandler` 中设置内部 request attribute：

```text
learningos.errorCode = ErrorCode.name()
```

过滤器读取该属性：

- 属性存在：使用属性值。
- 属性不存在且 `status < 400`：使用 `OK`。
- 属性不存在且 `status >= 400`：按 HTTP status 做兜底映射，至少保证 `500` 为 `INTERNAL_ERROR`。

### 3.3 route 字段

优先读取 Spring MVC `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE`。

缺失时使用：

```text
METHOD + " " + requestURI
```

不得包含完整 query string。

### 3.4 日志格式

使用现有 SLF4J/Logback 栈输出单行 key-value：

```text
http_request_completed traceId=... userId=... route="GET /api/..." status=200 latencyMs=12 errorCode=OK
```

不新增 JSON encoder。后续如果引入集中式日志平台，可在单独依赖评审后升级为 JSON 输出。

## 4. 安全规则

日志白名单字段：

- `traceId`
- `userId`
- `route`
- `status`
- `latencyMs`
- `errorCode`

禁止字段：

- 请求体、响应体
- 完整 query string
- `Authorization`、`Cookie`、`Set-Cookie`
- Prompt、模型输出、RAG chunk 原文
- 学生答案原文、教师评语原文
- 原始异常消息和堆栈正文

安全规范化：

- `traceId` 在 `TraceFilter` 入口校验。
- `userId` 在日志输出前按同类安全字符集截断或替换；日志只用于运维关联，不作为审计级身份来源。
- `route` 不包含 query string，并去除控制字符。

## 5. 官方文档参考

- Spring Framework `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE` 是 request attribute，可用于读取最佳匹配路由模式。
- Spring Boot `OutputCaptureExtension` 可用于测试捕获日志输出。

## 6. 测试策略

- `StructuredRequestLoggingFilterTest`
  - 成功请求记录 `traceId/userId/route/status/latencyMs/errorCode=OK`。
  - `ApiException` 请求记录 `errorCode=NOT_FOUND`。
  - 校验异常请求记录 `errorCode=VALIDATION_ERROR`。
  - 未知异常请求记录 `errorCode=INTERNAL_ERROR` 且不包含原始异常消息。
  - 缺失 MVC route pattern 时 fallback 到 `METHOD requestURI`。
  - 非法 `X-Trace-Id` 不进入日志，响应头和日志使用服务端生成的新 traceId。
- `TraceFilterTest`
  - 保持合法 `X-Trace-Id` 透传行为和清理行为不回退。
  - 非法 `X-Trace-Id` 被替换。

## 7. 非目标

- 不实现 Micrometer 指标。
- 不实现告警。
- 不实现深度健康检查。
- 不调整认证体系。
- 不改变业务服务日志。
