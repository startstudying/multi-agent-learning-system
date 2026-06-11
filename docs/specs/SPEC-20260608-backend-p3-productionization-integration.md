# SPEC-20260608 后端 P3 生产化集成总控

## 1. 架构策略

P3 剩余项按“安全前置 -> provider 观测 -> provider 接入 -> RAG 增强”串行集成：

```text
P3-4 权限矩阵
-> model_call_log provider schema
-> Spring AI Chat provider adapter
-> Spring AI Embedding provider adapter
-> VectorDB adapter
-> Parser layout/page hierarchy
-> OCR fallback
```

## 2. Subagent 决策

| Item | Value |
|---|---|
| Use Subagents | Yes |
| Reason | 任务同时涉及 RAG、Agent/model、DB/schema、权限安全、测试与文档。 |
| Parallelism Level | L1/L2：并行分析与设计；实现阶段默认单 TASK 串行。 |
| Selected Subagents | RAG Parser Expert、Model Gateway Expert、Security & Quality Expert、Integration Reviewer |
| Implementation Mode | 主 Codex 集成；后续仅在文件集合无重叠时允许实现 subagent。 |

## 3. 已回收专家结论

- Integration Reviewer：建议先建立总控文档，按切片推进；P3-4 权限矩阵优先。
- Security & Quality：确认当前最高风险是临时 `X-User-Id` 身份模型、课程读取无授权、RAG document course/chapter 归属未校验、学习路径/资源/答题未做完整 course/class scope。

## 4. 本轮当前切片

当前只实现 P3-4-C 的可控最小安全前置：

- `GET /api/courses`
- `GET /api/courses/{courseId}`
- `GET /api/courses/{courseId}/knowledge-graph`
- `POST /api/assessment/grading-evaluations`

真实 JWT、class enrollment 表、answer record detail API 暂不在本切片中实现；它们继续留在后续 P3-4 切片。

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只读取 current user，权限和业务规则下沉 Service。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 当前切片不改 Agent/RAG 执行链路。 |
| Security | PASS | 权限检查在后端代码，不依赖 Prompt。 |
| API / Database | PASS | 当前切片不新增 endpoint，不改 schema。 |
