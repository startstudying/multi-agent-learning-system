# RAG 质量评估任务

## Checklist

- [ ] 创建 PRD / REQ / SPEC / PLAN / TASK / Context Pack。
- [ ] 新增 Service RED 测试：expected chunk 命中、citation 命中、groundedness、no-source refusal。
- [ ] 新增 Controller RED 测试：响应包含新增指标、benchmark 汇总、报告字段。
- [ ] 运行定向测试确认 RED。
- [ ] 扩展 `RagEvaluationRequest` 支持 benchmark samples。
- [ ] 扩展 `RagEvaluationResult` 支持 `noSourceRefusalRate`、`benchmarkSummary`、`sampleResults`、`report`。
- [ ] 扩展 `RagEvaluationService` 完成四项指标和报告计算。
- [ ] 运行定向测试确认 GREEN。
- [ ] 创建 Evidence 和 Acceptance。

## Done Criteria

- `RagEvaluationServiceTest` 覆盖：
  - expected chunk topK 命中；
  - citation accuracy 命中；
  - groundedness；
  - no-source refusal。
- `RagEvaluationControllerTest` 覆盖 API 响应包含：
  - `noSourceRefusalRate`
  - `benchmarkSummary`
  - `sampleResults`
  - `report`
- 至少运行：

```bash
cd backend; mvn "-Dtest=RagEvaluationServiceTest,RagEvaluationControllerTest" test
```

- 不新增依赖。
- 不修改 `evaluation` 包和 `V14__evaluation_run_quality_metrics.sql`。
- 不修改共享收口文件：`docs/changelog/CHANGELOG.md`、`docs/memory/*`、`docs/planning/backend-architecture-todolist.md`。
