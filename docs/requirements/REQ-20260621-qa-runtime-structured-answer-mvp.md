# REQ-20260621 QaRuntime structured answer MVP

## 1. 功能需求

### FR-1 QaRuntime 入口

`AiQaService.answer(...)` 应委托 `QaRuntime.run(...)`，不再直接组合完整 RAG wrapper 响应。

### FR-2 IntentRouter

系统应根据 question、answerMode、kbIds、courseId 输出最小意图：

- `taskType`
- `complexity`
- `needsRag`
- `needsMemory`
- `qualityPolicy`
- `riskLevel`

`FAST / THINKING / EXPERT` 必须影响可观测策略，例如复杂度、检索预算或质量策略摘要。

### FR-3 ContextOrchestrator

系统应把 `MemoryContext`、RAG retrieval metadata 和 source citation 汇总成安全上下文摘要，供结构化回答组合使用。

摘要不得包含：

- raw prompt
- provider key
- raw chain-of-thought
- `teacher_note`
- 未脱敏 profileSnapshot
- 完整 citation excerpt

### FR-4 Structured answer schema

`AiQaResponse` 应新增向后兼容字段：

- `citations`
- `learnerFit`
- `nextSteps`
- `uncertainty`
- `qualityFlags`
- `requiresReview`

现有 `sources` 字段必须保留，并与 `citations` 保持一致。

### FR-5 no-source 策略

当 RAG 无可靠来源时：

- `citations` 和 `sources` 必须为空。
- `sourceStatus` 为 `GENERAL_FALLBACK`。
- `uncertainty.level` 至少为 `MEDIUM`。
- `qualityFlags` 必须包含 no-source 相关标记。

### FR-6 可解释 tool calls

响应的 `toolCalls` 至少包含：

- `IntentRouter`
- `RagQueryService`
- `MemoryContextService`
- `ContextOrchestrator`
- `FinalComposer`

每条 summary 必须是低敏摘要，不包含 raw question 或 prompt。

## 2. 非功能需求

- 不新增依赖。
- 不修改 DB schema。
- 不让前端直接调用 LLM API。
- 不把权限控制写进 prompt。
- 所有 RAG 检索继续通过 `RagQueryService`，复用已有权限过滤。
- 响应字段必须可由后续 P0-4 verifier/eval gate 直接消费。

## 3. 验收约束

- 必须按 TDD 写 RED 测试。
- 必须运行 focused test 和 adjacent API/RAG 回归。
- 必须记录 Evidence 和 Acceptance。
- 当前 subagent 工具要求用户显式授权才可 spawn，因此本切片不启用 subagent，只在文档中记录该约束。
