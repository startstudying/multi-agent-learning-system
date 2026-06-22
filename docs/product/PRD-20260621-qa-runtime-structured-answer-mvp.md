# PRD-20260621 QaRuntime structured answer MVP

## 1. 背景

当前 `/api/ai/qa` 已具备统一入口、`FAST / THINKING / EXPERT` 模式、RAG no-source fallback、P0-1 隐私守门和 P0-2 `MemoryContextService`。但 `AiQaService` 仍直接编排 RAG、fallback、MemoryContext 和响应 DTO，整体仍像 RAG wrapper。

P0-3 的产品目标是把 AI QA 升级为可解释的回答运行时 MVP：后端显式经过意图识别、检索工具、记忆上下文、上下文编排和结构化回答组合，输出稳定的结构化字段，给 P0-4 verifier/eval gate 留出清晰接口。

## 2. 用户价值

- 学生能看到更稳定的结构化回答：答案、引用、个性化适配、下一步建议和不确定性。
- 教师和管理员能从响应字段和 tool call 摘要中理解一次回答为何这样生成。
- 开发者可以在后续 P0-4 中对 schema、citation/no-source/privacy 做规则 verifier，而不是解析自然语言。

## 3. MVP 范围

本切片完成：

- 新增 `QaRuntime`，作为 `/api/ai/qa` 的服务层运行时。
- 新增最小 `IntentRouter`，输出任务类型、复杂度、是否需要 RAG/Memory、质量策略。
- 新增 `ContextOrchestrator`，把 P0-2 `MemoryContext` 和 RAG 元数据压缩为安全上下文摘要。
- 新增结构化回答组合逻辑，输出 `citations`、`learnerFit`、`nextSteps`、`uncertainty`、`qualityFlags`、`requiresReview`。
- `AiQaService` 降为薄门面，委托 `QaRuntime`。

本切片不完成：

- 不调用新的真实模型 provider。
- 不新增 DB schema、依赖、前端改造或流式接口。
- 不实现 P0-4 `AnswerVerifier` / eval gate 的完整规则库。
- 不实现长期 memory write pipeline 或 chat session 生命周期。

## 4. 成功标准

- `/api/ai/qa` 响应包含结构化字段：`answer`、`reasoningSummary`、`citations`、`learnerFit`、`nextSteps`、`uncertainty`、`traceId`。
- 现有 `sources` 字段保留，避免破坏当前前端调用。
- `toolCalls` 至少展示 `IntentRouter`、`RagQueryService`、`MemoryContextService`、`ContextOrchestrator`、`FinalComposer`。
- no-source 场景明确 `uncertainty.level` 和 `qualityFlags`，不伪造 citation。
- 不暴露 raw chain-of-thought、prompt、provider key、未脱敏 profileSnapshot 或完整 teacher note。
