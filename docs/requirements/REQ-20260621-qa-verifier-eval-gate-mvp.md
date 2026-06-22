# REQ-20260621 Basic Verifier / Eval Gate MVP

## 1. 功能需求

### FR-1 AnswerVerifier

系统应新增 `AnswerVerifier`，对 `AiQaResponse` 执行基础质量校验：

- schema 必填字段完整。
- `sources` 与 `citations` 一致。
- grounded 回答必须有 citation。
- no-source 回答不得有 citation。
- no-source 回答必须有 `NO_SOURCE_FALLBACK`、`uncertainty.level=MEDIUM` 或更高、`requiresReview=true`。
- 响应和 tool call summary 不得包含 raw chain-of-thought、prompt、provider key、`teacher_note`、未脱敏 profileSnapshot 等敏感标记。

### FR-2 Response verification 字段

`AiQaResponse` 应新增：

- `verification.verdict`
- `verification.checks`
- `verification.qualityFlags`
- `verification.requiresReview`
- `verification.gatePolicy`

`verification` 必须是低敏摘要，不包含 raw prompt 或原始私密上下文。

### FR-3 Runtime 接入

`QaRuntime` 应在 `FinalComposer` 后调用 `AnswerVerifier`，并：

- 将 verifier 结果写入响应。
- 将 verifier 低敏摘要追加到 `toolCalls`。
- 当 verifier 不是 `PASS` 时，将 `requiresReview` 置为 `true`。
- 将 verifier 衍生的质量标记合并进 `qualityFlags`。

### FR-4 QaEvalGate

系统应新增 `QaEvalGate` 纯服务，输入样本数、指标、阈值和策略变更信息，输出：

- `PASS`
- `FAIL`
- `INSUFFICIENT_SAMPLE`

规则：

- 样本数低于最小阈值时返回 `INSUFFICIENT_SAMPLE`。
- 缺少关键指标时返回 `FAIL`。
- 关键指标低于阈值时返回 `FAIL`。
- strategyChanged 为 `true` 时，必须同时提供 baseline 与 candidate 标识，否则返回 `FAIL`。

### FR-5 Evaluation Set / Run 最小复用

现有 Evaluation Set 应支持 `AI_QA_ANSWER` 样本类型。

现有 Evaluation Run 指标白名单应支持 QA gate 关键指标：

- `schemaPassRate`
- `verificationPassRate`
- `privacyLeakRate`

## 2. 非功能需求

- 不新增依赖。
- 不修改 DB schema。
- 不新增 REST API 路径。
- 不让前端直接调用 LLM。
- 不把权限控制写入 prompt。
- verifier / eval gate 输出不得包含敏感上下文或 raw 模型输出。

## 3. 验收约束

- 必须先写 RED 测试。
- 必须运行 focused tests、adjacent tests 和 compile。
- 必须写入 Evidence 和 Acceptance。
