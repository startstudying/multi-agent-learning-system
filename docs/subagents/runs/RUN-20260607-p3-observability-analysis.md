# RUN-20260607 P3-5 可观测性与运维分析

## 结论

P3-5 不是缺少基础业务数据，而是缺少统一的请求级结构化日志、Micrometer 业务指标、深度健康探测和运维告警摘要。Actuator/Micrometer 已由 `spring-boot-starter-actuator` 引入，可以优先做无新增依赖切片。

## 证据

- `TraceFilter` 只传播 `X-Trace-Id`，没有 route/status/latency/errorCode 结构化日志。
- `GlobalExceptionHandler` 返回统一 envelope，但没有把 errorCode 暴露给请求观测层。
- `HealthService` 当前只返回配置态，不做真实 dependency probe。
- `KbQueryLog`、`ModelCallLog`、`TokenUsageLog`、`ResourceReview` 已提供慢 RAG、慢模型、无引用、审核积压的数据基础。

## 推荐切片

最小切片可做 common 请求观测：在过滤器中记录 `traceId/userId/route/status/latency/errorCode`，并用已有 Micrometer 依赖记录基础 request timer/counter。

## 建议测试

```bash
cd backend && mvn "-Dtest=TraceFilterTest" test
cd backend && mvn test
```

