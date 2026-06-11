# ACCEPT - Micrometer 运行指标
状态：已通过（2026-06-07）

## 验收结论

P3-5 Micrometer 业务指标切片已完成，验收通过。

## 验收项

- [x] HTTP 请求完成时写入 `learningos.http.server.requests` timer。
- [x] HTTP `status >= 400` 时写入 `learningos.http.server.failures` counter。
- [x] RAG fresh query 写入 `learningos.rag.query.duration`，replay 不写 duration。
- [x] RAG query 写入 `learningos.rag.query.count`，并区分 `success/no_source/replay/error`。
- [x] RAG retrieval/citation 数量写入 `learningos.rag.retrieval.count` 与 `learningos.rag.citation.count`。
- [x] 模型网关写入 `learningos.model.call.duration` 与 `learningos.model.call.failures`。
- [x] token 使用写入 `learningos.token.usage`，cost 写入 `learningos.token.cost`。
- [x] 所有 metrics tags 仅包含低基数非敏感字段。
- [x] `application.yml` 仅新增 `metrics` 暴露。
- [x] 定向测试通过。
- [x] 全量 `cd backend && mvn test` 通过，且仅有 1 个 opt-in skipped。

## 证据关联

- 见 [EVIDENCE-20260607-micrometer-indicators.md](../evidence/EVIDENCE-20260607-micrometer-indicators.md)
- 见 [TASK-20260607-micrometer-indicators.md](../tasks/TASK-20260607-micrometer-indicators.md)

