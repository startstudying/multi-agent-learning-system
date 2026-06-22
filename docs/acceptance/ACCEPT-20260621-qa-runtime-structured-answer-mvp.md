# ACCEPT-20260621 QaRuntime structured answer MVP

## 1. 验收结论

Verdict: PASS

P0-3 `QaRuntime structured answer MVP` 已完成。`/api/ai/qa` 不再只是直接 RAG wrapper，而是经过 `QaRuntime -> IntentRouter -> RagQueryService -> MemoryContextService -> ContextOrchestrator -> FinalComposer`，并返回向后兼容的结构化回答字段。

## 2. 验收项

| 验收项 | 结论 | 证据 |
|---|---|---|
| `QaRuntimeTest` 覆盖 grounded 和 no-source 场景 | PASS | `QaRuntimeTest`: 2 run, 0 failures |
| `AiQaControllerTest` 覆盖新增响应字段 | PASS | `AiQaControllerTest`: 4 run, 0 failures |
| `sources` 保留，`citations` 与 `sources` 一致 | PASS | `QaRuntimeTest` / `AiQaControllerTest` 断言 citation/source |
| `toolCalls` 包含五个运行时步骤 | PASS | 断言 `IntentRouter`、`RagQueryService`、`MemoryContextService`、`ContextOrchestrator`、`FinalComposer` |
| no-source 不伪造 citation | PASS | no-source 测试断言 `sources/citations` 为空 |
| no-source 标记不确定性和 review | PASS | `uncertainty.level=MEDIUM`，`requiresReview=true` |
| grounded 场景低不确定性 | PASS | `uncertainty.level=LOW`，`requiresReview=false` |
| 不暴露 raw chain-of-thought / prompt / provider key / teacher note | PASS | runtime 测试断言 tool call summary 不包含敏感标记；实现未返回 prompt 或 provider secret |
| P0-2 memory context 兼容 | PASS | `MemoryContextServiceTest`: 1 run, 0 failures |
| RAG 相邻回归通过 | PASS | 整组复跑 `RagQueryServiceTest`: 22 run, 0 failures |

## 3. 剩余风险

- `FinalComposer` 仍使用当前 RAG answer / general fallback 作为 draft answer；真实 `AiModelGateway` 组合和 reviewer 不在本切片。
- `AnswerVerifier` / Eval Gate 尚未实现，P0-4 需要继续补 citation/no-source/privacy/schema 规则校验。
- 前端尚未消费 `learnerFit`、`nextSteps`、`uncertainty`、`qualityFlags`、`requiresReview`；本切片保持后端向后兼容。
- RAG reranker 相邻测试的 10ms timeout 分支有一次环境抖动，已单测复核并整组复跑通过。

## 4. 下一步

继续 P0-4 `Basic Verifier / Eval Gate`：为 `AiQaResponse` 增加可回归的 citation/no-source/privacy/schema gate，并建设最小 QA eval set。
