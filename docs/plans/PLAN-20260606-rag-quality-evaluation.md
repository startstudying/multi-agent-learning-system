# RAG 质量评估开发计划

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求 raw requirement 必须走 PRD/REQ/SPEC/PLAN/TASK/Context/Evidence/Acceptance |
| `rag-project-review` | 当前任务是 RAG 检索质量、引用质量和拒答质量评估 |
| `spring-boot-architecture` | 后端 Service / Controller / DTO 最小扩展 |
| `api-contract-design` | `/api/rag/evaluations` 响应新增 benchmark 和 report 字段 |
| `test-generator` | 需要 Service 和 Controller 测试覆盖新增指标 |
| `architecture-drift-check` | 需要确认不偏离 RAG、Service、Controller 边界 |

## Missing Skills

无。现有项目技能和本地 Codex 技能足够覆盖本任务。

## GitHub Research Needed

否。本轮不引入新依赖、不复制外部实现，指标规则为本地确定性计算。

## New Project-Specific Skill To Create

本轮不创建新 skill。RAG 质量指标仍可沉淀在现有 `rag-project-review` 能力范围内。

## Subagent Decision

- Use Subagents: Yes
- Reason: 用户明确指定“并行后端 worker”，且任务涉及 RAG 质量评估；本 worker 作为隔离实现者产出 subagent run 报告。
- Parallelism Level: L1 Parallel Analysis / Single Worker Implementation
- Selected Subagents:
  - RAG Backend Worker: 当前会话，负责 `rag` 包最小实现和测试。
- Implementation Mode: 单 worker，在允许写入范围内实现。

## Confidence Check

| Check | Result |
|---|---|
| Duplicate implementation | 仅发现 `RagEvaluationService` 的基础指标实现，缺少 benchmark、no-source refusal 和 report |
| Architecture compliance | 沿用现有 Spring Boot Controller -> Service 和 record DTO 模式 |
| Official docs | 不新增 SDK/API/依赖，使用项目内现有 Java record、MockMvc、JUnit 模式 |
| OSS references | 不需要；指标为本地确定性实现 |
| Root cause | P2-2 TODO 中 benchmark 固化、四项指标和归档报告未补齐 |

Confidence: 0.95

## 实施步骤

1. 创建本任务 PRD/REQ/SPEC/PLAN/TASK/Context Pack。
2. 修改 `RagEvaluationServiceTest`，先新增 RED 测试覆盖 expected chunk 命中、citation 命中、groundedness、no-source refusal。
3. 修改 `RagEvaluationControllerTest`，先新增 RED 测试覆盖新增响应字段。
4. 运行 `cd backend; mvn "-Dtest=RagEvaluationServiceTest,RagEvaluationControllerTest" test` 观察 RED。
5. 最小扩展 `RagEvaluationRequest`、`RagEvaluationResult`、`RagEvaluationService`。
6. 运行同一测试命令观察 GREEN。
7. 写 Evidence 和 Acceptance。

## 架构漂移预检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只调用 Service |
| Frontend rules | N/A | 不涉及前端 |
| Agent / RAG rules | PASS | 仅离线评估已给定 citations，不改 RAG retrieval |
| Security | PASS | 不新增依赖，不保存 secrets |
| API / Database | PASS | API 字段在 SPEC 中声明，不改 DB |
