# SPEC - Fusion RAG POC Evaluation Verifier

## 1. 目标

本切片把 POC context 的效果接入 RAG evaluation，并给 AI QA 回答增加轻量 Corrective / Self-RAG 风格 verifier：

- 支持 `baseline topK` vs `poc-context` 的离线 paired comparison。
- 增加核心结论是否被 citation 覆盖的质量指标。
- 增加疑似混入未引用扩展上下文的质量指标。
- 暂缓完整 GraphRAG / Agentic RAG，不做实体关系建模或多轮 agent 检索。

## 2. 非目标

- 不新增 DB schema / Flyway migration。
- 不新增 Maven dependency。
- 不新增 frontend 页面。
- 不改 `POST /api/rag/query` 或 `POST /api/ai/qa` 请求结构。
- 不在 evaluation 内部 live dual-run `RagQueryService`。
- 不为生产 RAG 查询链路新增 POC 开关。
- 不读取或持久化 POC 扩展 chunk 原文作为 verifier 输入。

## 3. API / DTO

`POST /api/rag/evaluations` 保留旧格式，并新增可选 `comparison`：

```json
{
  "comparison": {
    "comparisonId": "fusion-rag-poc-v1",
    "baselineId": "baseline-topk",
    "candidateId": "poc-context",
    "topK": 3,
    "samples": [
      {
        "sampleKey": "rag-001",
        "question": "为什么事务隔离会影响幻读？",
        "expectedChunkIds": ["chunk_isolation"],
        "expectedNoSource": false,
        "baseline": {
          "answer": "事务隔离影响并发可见性。",
          "citations": []
        },
        "candidate": {
          "answer": "事务隔离影响并发可见性。幻读发生在范围查询看见新增行时。",
          "citations": []
        }
      }
    ]
  }
}
```

响应新增字段：

- `coreClaimCitationCoverage`
- `uncitedContextLeakRate`
- `comparisonResult`

`comparisonResult` 包含：

- `baselineMetrics`
- `candidateMetrics`
- `deltas`
- `winnerByMetric`
- `sampleResults`

旧 single / benchmark 响应字段继续保留。

## 4. Citation Coverage Analyzer

新增 `CitationCoverageAnalyzer`：

```text
analyze(answer, citations) -> CoverageResult
```

输入只包括：

- 最终可见 answer。
- `SourceCitation.excerpt`
- `SourceCitation.sectionTitle`
- `SourceCitation.documentName`

输出：

- `coreClaimCount`
- `coveredCoreClaimCount`
- `coreClaimCitationCoverage`
- `uncitedContextLeak`

启发式规则：

- 将 answer 按中英文句末标点拆成核心结论句。
- 用 citation 可见文本构造 coverage corpus。
- 英文使用长度大于等于 4 的词；中文使用相邻双字片段。
- 单句 claim 的可见关键词覆盖率达到阈值则视为已覆盖。
- grounded answer 中存在未覆盖核心 claim 时，标记疑似 `uncitedContextLeak`。

该 analyzer 是质量信号，不作为安全权限判断。

## 5. Verifier

`AnswerVerifier` 新增检查：

- `CORE_CLAIM_CITATION_COVERAGE`
- `UNCITED_CONTEXT_LEAK_GUARD`

行为：

- 已有 BLOCKER 检查仍决定 `verdict=FAIL`。
- 新检查先输出 `WARN`，不直接把 verdict 变为 `FAIL`。
- 出现 WARN 时 `requiresReview=true`，并添加质量标记。

## 6. Evaluation Metrics

RAG benchmark 与 comparison 新增：

- `coreClaimCitationCoverage`: 越高越好。
- `uncitedContextLeakRate`: 越低越好。

`EvaluationRunService` 允许记录这两个 metric，其中 `uncitedContextLeakRate` 纳入 lower-is-better。

## 7. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 继续转发到 Service；指标计算在 application service / analyzer 内 |
| Frontend rules | PASS | 无 frontend 改动 |
| Agent / RAG rules | PASS | RAG citation 边界不变；evaluation 不触发生产检索 |
| Security | PASS | 无 secret、无新依赖、无扩展 chunk 原文持久化 |
| API / Database | PASS | 可选 DTO/响应字段已在本 SPEC 记录；无 DB schema |

## 8. 测试策略

- RED: 新增 `CitationCoverageAnalyzerTest`。
- RED: 扩展 `RagEvaluationServiceTest` 与 `RagEvaluationControllerTest` 覆盖 comparison。
- RED: 扩展 `AnswerVerifierTest` 覆盖 WARN/review 行为。
- RED: 扩展 `EvaluationRunServiceTest` 与 `QaEvalGateTest` 覆盖新指标。
- GREEN: 实现 analyzer、evaluation comparison、verifier 接入、metric whitelist。
- 回归: 运行 focused + adjacent backend tests 与 compile。

## 9. 验收

- [x] `comparison` 能输出 baseline/candidate 指标、delta 与 winner。
- [x] POC candidate 的 coverage/leak 指标可见。
- [x] Verifier 能发现 citation 未覆盖核心结论并要求 review。
- [x] Verifier 能发现疑似未引用 context 混入但不直接 FAIL。
- [x] 旧 single / benchmark evaluation 入参保持兼容。
- [x] 无 DB / dependency / frontend / production query path 改动。

## 10. 实施状态

- 状态：已完成。
- 完成日期：2026-06-22。
- Evidence：`docs/evidence/EVIDENCE-20260622-fusion-rag-poc-evaluation-verifier.md`。
- Acceptance：`docs/acceptance/ACCEPT-20260622-fusion-rag-poc-evaluation-verifier.md`。
