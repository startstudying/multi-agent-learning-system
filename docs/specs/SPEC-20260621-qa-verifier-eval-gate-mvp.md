# SPEC-20260621 Basic Verifier / Eval Gate MVP

## 1. 目标架构

```text
AiQaController
-> AiQaService
-> QaRuntime
   -> IntentRouter
   -> RagQueryService
   -> MemoryContextService
   -> ContextOrchestrator
   -> FinalComposer
   -> AnswerVerifier
-> AiQaResponse.verification
```

Eval gate 复用现有评测基础：

```text
EvaluationSetService
-> AI_QA_ANSWER samples

EvaluationRunService
-> QA gate metric whitelist

QaEvalGate
-> PASS / FAIL / INSUFFICIENT_SAMPLE
```

## 2. `AnswerVerifier`

新增包：

```text
com.learningos.aiqa.application.quality
```

建议入口：

```java
VerificationSummary verify(AiQaResponse response)
```

检查项：

| Check | PASS 条件 | FAIL 条件 |
|---|---|---|
| `SCHEMA_REQUIRED_FIELDS` | answer/sourceStatus/sourcePolicy/traceId/qualityFlags 存在 | 缺关键字段 |
| `CITATION_CONSISTENCY` | `sources` 与 `citations` 数量和内容一致 | 不一致 |
| `GROUNDED_CITATION_REQUIRED` | `COURSE_GROUNDED` 有 citation | grounded 但无 citation |
| `NO_SOURCE_POLICY` | fallback 无 citation、标记 no-source、requiresReview | fallback 仍有 citation 或缺标记 |
| `PRIVACY_LEAK_GUARD` | 输出摘要不含敏感标记 | 包含敏感标记 |

Verdict：

- `PASS`：全部必需检查通过。
- `FAIL`：schema、citation consistency、privacy 任一失败。
- `REVIEW`：非阻断但需要人工 review 的策略状态。

## 3. DTO 变更

`AiQaResponse` 新增：

```java
VerificationSummary verification;
```

新增 record：

```java
record VerificationSummary(
    String verdict,
    List<VerificationCheck> checks,
    List<String> qualityFlags,
    boolean requiresReview,
    String gatePolicy
) {}

record VerificationCheck(
    String name,
    String status,
    String severity,
    String message
) {}
```

## 4. `QaEvalGate`

新增 record：

```java
record QaEvalGateRequest(
    int sampleCount,
    int minimumSampleCount,
    boolean strategyChanged,
    String baselineId,
    String candidateId,
    List<MetricThreshold> thresholds,
    Map<String, Double> metrics
) {}
```

结果：

```java
record QaEvalGateResult(
    String verdict,
    int sampleCount,
    int minimumSampleCount,
    List<MetricCheck> checks,
    List<String> missingMetrics,
    String reason
) {}
```

默认关键指标：

- `groundedness >= 0.80`
- `citationAccuracy >= 0.80`
- `noSourceRefusalRate >= 0.90`
- `schemaPassRate >= 0.95`
- `verificationPassRate >= 0.95`
- `privacyLeakRate <= 0.00`

## 5. Evaluation Set / Run 复用

`EvaluationSetService`：

- 新增允许类型 `AI_QA_ANSWER`。
- 样本至少需要 `question` 和 `qualityCriteria`。
- 若需要 source-required 样本，可继续使用 `expectedSourceIds`。

`EvaluationRunService`：

- 允许 `schemaPassRate`、`verificationPassRate`、`privacyLeakRate`。
- `privacyLeakRate` 是 lower-is-better 指标。

## 6. 安全与隐私

- verifier 不输出 raw prompt、raw answer 内部思考、provider key、teacher note、未脱敏 profileSnapshot。
- eval gate 只处理聚合指标，不处理原始回答内容。
- 不新增长期存储字段，因此不改变 P0-1 的隐私策略。

## 7. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller -> Service -> Runtime/Quality |
| Frontend rules | PASS | 不改前端，不新增前端 LLM 调用 |
| Agent/RAG rules | PASS | RAG 仍通过 `RagQueryService`，verifier 无工具循环 |
| Security | PASS | 不新增密钥/依赖，输出低敏摘要 |
| DB schema | PASS | 不新增表或字段 |
| API contract | PASS with change | 响应 DTO 新增向后兼容 `verification` |
