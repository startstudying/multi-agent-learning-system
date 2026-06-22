# TASK - Fusion RAG POC Evaluation Verifier

## 1. 任务目标

完成 Fusion RAG 强化：

- 将 POC context 效果接入 evaluation 对比：`baseline topK` vs `poc-context`。
- 为 AI QA answer verifier 增加引用覆盖核心结论与未引用 context 混入检查。
- 明确暂缓完整 GraphRAG / Agentic RAG。

## 2. Skill Selection

| Skill | Why |
|---|---|
| feature-development-workflow | 本任务是 M 级后端 RAG/evaluation 功能切片 |
| educational-rag-pipeline | 约束 RAG citation、evaluation、no-source 边界 |
| test-driven-development | 先写失败测试，再写实现 |
| verification-before-completion | 完成前必须有新鲜测试证据 |
| Confidence Check | 实施前确认方向、重复实现、架构与根因 |

Missing skills: 无。

GitHub research: 不新增；复用 `docs/research/github-references/GITHUB-20260621-wiki-rag-reference.md`。

## 3. Size Classification

Size: M。

Reason:

- 涉及 RAG evaluation、AI QA verifier、evaluation run metrics 三个后端相关点。
- 有可见 API 响应扩展和质量指标扩展。
- 无 DB、依赖、frontend、生产查询路径和权限架构变化。

Required documents:

- SPEC
- TASK
- CONTEXT
- Evidence
- Acceptance

Can skip:

- PRD: 不改变产品工作流。
- REQ: 需求边界清晰。
- PLAN: 当前计划已经由用户确认，且实现路径单切片。

Upgrade trigger:

- 若引入 live dual-run、生产 POC on/off 开关、DB schema、新 endpoint、frontend workbench，则升级 L。

## 4. Subagent Decision

- Use Subagents: No spawned agent.
- Reason: 项目规则要求 Agent/RAG Expert 与 Security & Quality 分析；当前 Codex 工具策略不允许用户未显式要求时 spawn sub-agent。
- Record: `docs/subagents/runs/RUN-20260622-fusion-rag-poc-evaluation-verifier.md`。
- Implementation Mode: Single Codex。

## 5. Context Pack

Standalone: `docs/context/CONTEXT-20260622-fusion-rag-poc-evaluation-verifier.md`。

## 6. TDD Plan

1. `CitationCoverageAnalyzerTest`
2. `RagEvaluationServiceTest`
3. `RagEvaluationControllerTest`
4. `AnswerVerifierTest`
5. `EvaluationRunServiceTest`
6. `QaEvalGateTest`

## 7. Acceptance Criteria

- [ ] comparison 模式按 paired sample 计算 baseline/candidate。
- [ ] candidate 顶层指标可用于旧消费者读取。
- [ ] coverage/leak 指标在 benchmark 和 comparison 可用。
- [ ] verifier 新检查 WARN 时要求 review，但不破坏旧 PASS/FAIL 语义。
- [ ] 新指标可被 Evaluation Run 记录和比较。
- [ ] focused + adjacent tests 和 compile 通过，或限制被记录。

## 8. Evidence

- RED：`mvn test "-Dtest=CitationCoverageAnalyzerTest,RagEvaluationServiceTest,RagEvaluationControllerTest,AnswerVerifierTest,QaEvalGateTest,EvaluationRunServiceTest"` 首次失败在缺少 `CitationCoverageAnalyzer`、`comparisonResult` 和 `coreClaimCitationCoverage`，证明测试覆盖新行为。
- GREEN focused：同一命令通过，33 tests, 0 failures, 0 errors。
- Adjacent：`mvn test "-Dtest=RagQueryServiceTest,QaRuntimeTest,AiQaControllerTest"` 通过，30 tests, 0 failures, 0 errors。
- Compile：`mvn compile -q` 通过。

## 9. Acceptance Verdict

PASS。

- comparison 模式已输出 baseline/candidate metrics、delta、winnerByMetric 与 paired sample results。
- benchmark 与 comparison 均输出 `coreClaimCitationCoverage` / `uncitedContextLeakRate`。
- `AnswerVerifier` 新增 `CORE_CLAIM_CITATION_COVERAGE` 与 `UNCITED_CONTEXT_LEAK_GUARD`，WARN 时要求 review 但不直接 FAIL。
- `EvaluationRunService` 允许记录新指标，并把 `uncitedContextLeakRate` 作为 lower-is-better。
- 未新增 DB schema、依赖、frontend 或生产 RAG query 开关。
