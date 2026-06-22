# PLAN-20260621 记忆系统与回答质量路线图执行计划

## 1. Skill Selection Report

### Task Type

L-size docs/research roadmap：记忆系统、AI QA、Agent/RAG、模型网关、评测、Trace、安全治理的跨模块优化规划。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目强制 spec-first、size gate、文档与证据闭环 |
| `analyze` | 对现有代码和文档做只读差距分析 |
| `openai-docs` | 回答质量、Responses API、tools、structured outputs、evals 等需要官方资料 |
| `ai-learning-architecture` | 设计学习系统内 Agent/QA/上下文编排边界 |
| `educational-rag-pipeline` | RAG、citation、检索质量和 no-source 策略 |
| `learner-profile-agent` | 学习者画像进入记忆上下文的最小化策略 |
| `agent-trace-governance` | trace、tool call、model call、eval 观测闭环 |
| `spring-ai-agent-backend` | Java/Spring AI 后端模型网关和工具边界 |

### Missing Skills

建议后续沉淀项目技能：`memory-privacy-governance`，覆盖 memory 写入、脱敏、TTL、删除、用户可见治理和评测门禁。

### GitHub Research Needed

No。当前已有用户框架、项目内技能、OpenAI 官方资料和本地代码证据足够支撑路线图；不需要复制 GitHub 实现。

## 2. Size Classification

Size: L

Reason:

- 涉及记忆系统、AI QA、RAG、Agent、模型网关、评测、Trace、安全、前后端展示语义。
- 是新产品能力和架构路线图，不是单文件修补。
- 需要 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / EVIDENCE / ACCEPT。

Required Documents:

- `docs/product/PRD-20260621-memory-answer-quality-roadmap.md`
- `docs/requirements/REQ-20260621-memory-answer-quality-roadmap.md`
- `docs/specs/SPEC-20260621-memory-answer-quality-roadmap.md`
- `docs/plans/PLAN-20260621-memory-answer-quality-roadmap.md`
- `docs/tasks/TASK-20260621-memory-answer-quality-roadmap.md`
- `docs/context/CONTEXT-20260621-memory-answer-quality-roadmap.md`
- `docs/evidence/EVIDENCE-20260621-memory-answer-quality-roadmap.md`
- `docs/acceptance/ACCEPT-20260621-memory-answer-quality-roadmap.md`

## 3. Subagent Decision

Use Subagents: Yes

Reason:

- Agent/RAG、外部研究、安全质量是独立分析面。
- 本任务影响 3+ 模块，且包含 Agent/RAG 与安全治理。

Parallelism Level: L1 Parallel Analysis

Reports:

- `docs/subagents/runs/RUN-20260621-memory-chatgpt-quality-external-research.md`
- `docs/subagents/runs/RUN-20260621-memory-chatgpt-quality-security-quality.md`
- Agent/RAG expert 结论由主线程整合记录：当前根因是基础能力分散，缺少统一 QaRuntime / Context Composer / MemoryContextService。

Implementation Mode:

- 本轮仅文档与路线图。
- 后续每个 slice 单独走 workflow，不并行修改同一文件。

## 4. 推荐执行顺序

### P0-1 Memory/RAG Privacy Guard

目标：在扩大记忆前先收紧敏感数据持久化。

范围：

- RAG query/replay/citation 的 raw question、answer、excerpt 脱敏或降级。
- profileSnapshot 字段最小化，默认排除 `teacher_note`。
- 设计 memory 写入前 sensitivity policy。

### P0-2 MemoryContextService MVP

目标：形成统一、可解释、可裁剪的上下文对象。

范围：

- 低敏 learner profile summary。
- learning events / mastery / wrong-question summary。
- recent session summary。
- RAG citation context。
- token budget 与注入理由记录。

### P0-3 QaRuntime / Answer Quality MVP

目标：`/api/ai/qa` 不再只是 RAG 包装，而是统一运行时。

范围：

- IntentRouter。
- ContextOrchestrator。
- AiModelGateway structured QA schema。
- `FAST / THINKING / EXPERT` 策略真实化。

### P0-4 Basic Verifier / Eval Gate

目标：把质量从主观感受变成可回归指标。

范围：

- citation / no-source / privacy / schema / instruction-following verifier。
- QA eval dataset 和 gate verdict。
- trace 中记录 verifier 结果。

### P1

- `/api/ai/qa/stream`。
- prompt/schema versions：`qa-answer-v1`、`qa-review-v1`。
- tool call trace：memory retrieval、RAG retrieval、model generation、reviewer。
- answer quality workbench。

### P2

- 记忆生命周期治理：salience、decay、confidence merge、用户可见编辑/删除、retention sweep。
- 扩展 `kb_chat_session` / `kb_chat_message`。
- GraphMemory / procedural memory 探索。

## 5. Architecture Drift Check

本路线图保持现有架构规则：

- 后端拥有 AI API 调用。
- 工具走 Service 层。
- RAG 必须有 citation。
- AI workflow 必须有 traceId。
- 资源生成默认走 review。

潜在 drift：

- 若未来引入 PostgreSQL + pgvector 或 OpenAI Agents SDK，需要 ADR 与依赖评审。
- 若使用 OpenAI conversation state，只能作为外部调用状态引用，不能替代项目 DB 中的业务记忆。

## 6. 验证计划

本轮只读调研与文档产出，不运行后端/前端测试。

后续 P0 slice 的验证应包含：

- focused unit tests。
- adjacent integration tests。
- privacy persistence assertions。
- eval gate fixture。
- trace coverage assertions。
