# 资源生成引用与幻觉治理开发计划

## Skill Selection Report

### Task Type

后端资源生成、RAG citation、Critic review governance 和测试。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `ai-learning-agent-development` | 约束 AI 学习系统后端边界和 Agent Trace 规则。 |
| `educational-rag-pipeline` | 本切片涉及 `source_citation` 和引用治理。 |
| `learning-resource-agent` | 资源生成必须携带引用、审核和安全状态。 |
| `critic-review-agent` | Critic review 需要 citation check 和发布门禁。 |
| `test-driven-development` | 新行为先写 RED 测试。 |
| `verification-before-completion` | 完成前必须用测试输出证明。 |

### Missing Skills

无。

### GitHub Research Needed

否。当前是复用本项目已有 `source_citation` 和 review governance，不引入外部 SDK。

## Subagent Decision

Use Subagents: Yes

Parallelism Level: L1

Selected Subagents:

- James / Tesla：RAG citation / schema 影响分析。
- Arendt：Review governance 风险分析。
- Plato：TDD RED 测试设计。

Implementation Mode：主 Codex 串行实现，subagent 只读分析。

## Confidence Check

- 无重复实现：资源生成此前只写 `citationSummary`，未写 `source_citation`。
- 架构合规：复用 Service + JPA Repository，不新增依赖。
- 官方文档：不涉及外部 SDK/API。
- OSS 参考：不需要。
- 根因明确：RAG 问答已有 citation 持久化，资源生成缺同等证据链。

Confidence: 0.93。

## 实施步骤

1. 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / subagent run。
2. 增加 ResourceGeneration 和 ReviewGovernance RED 测试。
3. 在 `ResourceGenerationService` 注入 `SourceCitationRepository`，生成任务级 citation。
4. 初始 review 写入 citation check；无来源路径写入 `NO_SOURCE` summary。
5. `ReviewGovernanceService` 拦截 `NO_SOURCE` approval，并在 learner release 中兜底。
6. 运行最小验证、相关回归和 Orchestrator 回归。
7. 更新 evidence / acceptance / changelog / memory / TODO / retrospective。

## Architecture Drift Check Result

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller unchanged; citation persistence and release rules stay in Service layer. |
| Frontend rules | PASS | No frontend change and no direct model/API-key exposure. |
| Agent / RAG rules | PASS | Resource generation keeps traceId and review status; citations are persisted under the task traceId. |
| Security | PASS with known limitation | No new secrets or dependencies; temporary `teacher/admin` reviewer boundary remains a known P3 RBAC limitation. |
| API / Database | PASS with known limitation | API semantics documented; no schema change. Citation is task-level via `traceId`, not resource-level. |
