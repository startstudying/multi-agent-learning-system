# 复盘 - 结构化请求日志

日期：2026-06-07

## 1. 做了什么

完成 P3-5 最小可观测性切片：后端请求完成结构化日志。实现采用独立 `StructuredRequestLoggingFilter`，保留 `TraceFilter` 的 trace 生命周期职责，异常错误码通过 `GlobalExceptionHandler` 内部 request attribute 传递。

## 2. 有效做法

- 先由 Backend Expert 和 Security & Quality 并行分析，提前识别过滤器顺序和日志注入风险。
- 使用 TDD 先写缺失过滤器测试，确认红灯后再实现。
- 将日志字段限制为白名单，避免为了排障方便引入请求体、query、异常消息等敏感内容。
- 全量测试验证了新过滤器不会破坏已有 MVC slice 和集成测试。

## 3. 风险与教训

- 横切过滤器会被 `@WebMvcTest` 自动加载，构造器依赖必须兼容测试切片。最终通过无参构造保持兼容。
- `X-Trace-Id` 过去直接透传，进入日志后需要从“相关性字段”升级为“不可信输入”处理。
- 当前文本 key-value 日志够用，但正式日志平台接入仍需单独依赖和格式评审。

## 4. 后续建议

- P3-5 下一步做 Micrometer 指标：请求延迟、RAG 延迟、模型延迟、失败率、token 成本。
- P3-5 后续告警切片再处理慢查询、慢模型调用、无引用回答、审核积压。
- P3-4 后续正式 RBAC 完成后，重新确认请求日志中的 `userId` 来源语义。

## 5. 可复用模式

沉淀项目技能：`docs/skills/project-specific/structured-request-logging.md`，用于后续日志字段白名单、过滤器顺序和安全脱敏。
