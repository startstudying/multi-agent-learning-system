# RUN - Fusion RAG POC Evaluation Verifier

## 1. Subagent Gate

- Task type: RAG / evaluation / QA verifier.
- Size: M.
- Required experts by project rule: Agent/RAG Expert, Security & Quality.
- Execution note: 当前 Codex 工具策略不允许在用户未显式要求 subagent 时实际 spawn sub-agent，因此本文件记录主 Codex 执行的 L1 专家分析结论；不进行并行实现。

## 2. Agent/RAG Expert Analysis

- 现有 `PocContextBuilder` 已经把 parent / adjacent / child context 接入 `RagQueryService` answer composition。
- 现有 citation 边界正确：`sources` 与 `source_citation` 仍只锚定原始 retrieved/reranked chunks。
- 新能力应接入 evaluation，而不是生产 query dual-run，避免为了评估新增 POC on/off 开关。
- 对比方式采用 captured output：baseline topK 与 POC context candidate 都由调用方提供 answer/citations。
- 引用覆盖判断只能使用可见 answer 与 citation excerpt/sectionTitle/documentName，不能读取或持久化扩展 chunk 原文。

## 3. Security & Quality Analysis

- No DB schema change.
- No dependency change.
- No frontend direct LLM/API change.
- No production RAG permission or retrieval-path change.
- `uncitedContextLeakRate` 是启发式质量指标，不作为权限控制或安全保证。
- Verifier 新增检查应先作为 review signal，避免启发式误判导致可用回答被硬失败。

## 4. Integration Decision

- 新增共享 `CitationCoverageAnalyzer`，供 RAG evaluation 与 `AnswerVerifier` 复用。
- `POST /api/rag/evaluations` 新增可选 `comparison` 请求体字段；旧 single / benchmark 入参继续兼容。
- `RagEvaluationResult` 新增 comparison 与 coverage/leak 指标字段；旧字段继续保留。
- `EvaluationRunService` metric whitelist 新增 `coreClaimCitationCoverage` 与 `uncitedContextLeakRate`。
