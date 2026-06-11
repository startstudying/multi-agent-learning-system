# 证据文档 - 结构化请求日志

## 1. 追踪

- TASK：`docs/tasks/TASK-20260607-structured-request-logging.md`
- SPEC：`docs/specs/SPEC-20260607-structured-request-logging.md`
- 日期：2026-06-07

## 2. 实现内容

本切片完成 P3-5 最小可观测性能力：后端 HTTP 请求完成后输出结构化白名单日志。新增独立 `StructuredRequestLoggingFilter`，在 `DevAuthFilter` 之后运行，记录 `traceId/userId/route/status/latencyMs/errorCode`。`GlobalExceptionHandler` 写入内部 `errorCode` request attribute，避免日志过滤器按 HTTP status 猜测业务错误码。`TraceFilter` 对客户端 `X-Trace-Id` 增加安全字符集和长度校验，非法值替换为服务端生成的 UUID。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/common/trace/StructuredRequestLoggingFilter.java` | 新增 | 请求完成结构化日志过滤器 |
| `backend/src/main/java/com/learningos/common/trace/TraceFilter.java` | 修改 | 校验 `X-Trace-Id`，非法值替换为 UUID |
| `backend/src/main/java/com/learningos/common/exception/GlobalExceptionHandler.java` | 修改 | 将 `ErrorCode` 写入内部 request attribute |
| `backend/src/test/java/com/learningos/common/trace/StructuredRequestLoggingFilterTest.java` | 新增 | 覆盖成功、业务异常、校验异常、未知异常、route fallback、恶意 traceId |
| `backend/src/test/java/com/learningos/common/trace/TraceFilterTest.java` | 修改 | 覆盖非法 traceId 替换 |
| `docs/product/PRD-20260607-structured-request-logging.md` | 新增 | 产品目标与范围 |
| `docs/requirements/REQ-20260607-structured-request-logging.md` | 新增 | 功能和安全需求 |
| `docs/specs/SPEC-20260607-structured-request-logging.md` | 新增 | 设计、字段白名单、测试策略 |
| `docs/plans/PLAN-20260607-structured-request-logging.md` | 新增 | Skill、Subagent、实施计划 |
| `docs/tasks/TASK-20260607-structured-request-logging.md` | 新增 | 单任务定义和 Done Criteria |
| `docs/context/CONTEXT-20260607-structured-request-logging.md` | 新增 | 允许修改文件和任务边界 |
| `docs/subagents/runs/RUN-20260607-structured-request-logging.md` | 新增 | 后端与安全子代理分析集成 |

## 4. 测试结果

### 执行的命令

```bash
cd backend && mvn --% -Dtest=StructuredRequestLoggingFilterTest,TraceFilterTest,GlobalExceptionHandlerTest test
cd backend && mvn test
```

### 结果

| 命令 | 结果 | 备注 |
|---|---|---|
| `mvn --% -Dtest=StructuredRequestLoggingFilterTest,TraceFilterTest,GlobalExceptionHandlerTest test` | 通过 | 11 tests, 0 failures, 0 errors, 0 skipped |
| `mvn test` | 通过 | 244 tests, 0 failures, 0 errors, 1 skipped |

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 变更限定在 `common` 横切层，未向业务服务散落日志逻辑 |
| Frontend rules | PASS | 未修改前端 |
| Agent / RAG rules | PASS | 未改变 Agent/RAG 执行与引用规则 |
| Security | PASS | 日志采用字段白名单，不记录 body/query/header/prompt/raw exception |
| API / Database | PASS | 未修改 API 响应契约，未修改数据库 schema，未新增依赖 |

## 6. 日志证据摘要

定向测试输出中可观察到类似结构：

```text
http_request_completed traceId=trace-123 userId=alice route=/test/structured-log status=200 latencyMs=1 errorCode=OK
http_request_completed traceId=trace-404 userId=alice route=/test/structured-log/not-found status=404 latencyMs=2 errorCode=NOT_FOUND
```

测试同时断言：

- 未知异常原始消息 `boom` 不进入日志。
- query string 中的 `secret-learning-question` 不进入日志。
- 非法 `trace\ninjected` 不进入日志或响应头。

## 7. 已知限制

- 当前 `userId` 仍来自开发态 `X-User-Id`，不可作为正式审计级身份来源；真实 JWT/RBAC 仍属于后续 P3 权限任务。
- 日志当前为现有 SLF4J key-value 文本输出，不引入 JSON encoder；集中日志平台接入需单独依赖评审。
- Micrometer 指标、深度健康检查和告警仍是 P3-5 后续切片。

## 8. 评审备注

Backend Expert 和 Security & Quality 子代理均建议独立过滤器、错误码 request attribute、字段白名单和 traceId 校验；实现已按该集成结论落地。
