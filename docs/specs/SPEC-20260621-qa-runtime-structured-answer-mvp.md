# SPEC-20260621 QaRuntime structured answer MVP

## 1. 当前状态

P0-1 已完成隐私守门；P0-2 已完成 `MemoryContextService MVP`。当前 `AiQaService` 仍直接执行：

```text
resolve answerMode
-> RagQueryService.query(...)
-> no-source fallback
-> MemoryContextService.build(...)
-> new AiQaResponse(...)
```

这让 `/api/ai/qa` 仍像 RAG wrapper，缺少显式运行时步骤和结构化回答 schema。

## 2. 目标架构

```text
AiQaController
-> AiQaService
-> QaRuntime
   -> IntentRouter
   -> RagQueryService
   -> MemoryContextService
   -> ContextOrchestrator
   -> FinalComposer
```

说明：

- 本 MVP 不新增真实 `AiModelGateway` 调用；仍使用当前 RAG answer / general fallback 作为 draft answer。
- `AnswerVerifier` / Eval Gate 进入 P0-4。
- `QaRuntime` 先固化运行时边界、tool call 摘要和结构化输出字段。

## 3. 新增组件

### 3.1 `QaRuntime`

入口：

```java
AiQaResponse run(String userId, boolean admin, boolean teacher, AiQaRequest request)
```

职责：

1. 解析 `QaModePolicy`。
2. 调用 `IntentRouter.classify(...)`。
3. 调用 `RagQueryService`。
4. 调用 `MemoryContextService.build(...)`。
5. 调用 `ContextOrchestrator.build(...)`。
6. 调用 `FinalComposer.compose(...)`。
7. 返回 `AiQaResponse`。

### 3.2 `IntentRouter`

输出 record：

```java
record QaIntent(
    String taskType,
    String complexity,
    boolean needsRag,
    boolean needsMemory,
    String qualityPolicy,
    String riskLevel
) {}
```

MVP 规则：

- `FAST`: `complexity=LOW`，`qualityPolicy=BASIC_SOURCE_POLICY`。
- `THINKING`: `complexity=MEDIUM`，`qualityPolicy=STANDARD_SOURCE_POLICY`。
- `EXPERT`: `complexity=HIGH`，`qualityPolicy=STRICT_SOURCE_POLICY`。
- `kbIds` 非空时 `needsRag=true`。
- 默认 `needsMemory=true`。

### 3.3 `ContextOrchestrator`

输出 record：

```java
record QaContextEnvelope(
    String contextSummary,
    int contextItems,
    int citationCount,
    int estimatedTokens,
    boolean memoryTruncated
) {}
```

所有字段为低敏摘要，不包含 raw question / prompt。

### 3.4 `FinalComposer`

根据 draft answer、sources、intent、memory context、source status 生成结构化字段：

- `learnerFit`: 说明本回答使用哪些低敏上下文来适配学习者。
- `nextSteps`: 2-3 条下一步建议。
- `uncertainty`: no-source 或引用不足时提升不确定性。
- `qualityFlags`: 例如 `COURSE_GROUNDED`、`NO_SOURCE_FALLBACK`、`MEMORY_CONTEXT_USED`。
- `requiresReview`: MVP 中 no-source 或高风险时为 `true`，否则 `false`。

## 4. DTO 变更

`AiQaResponse` 新增字段：

```java
List<SourceCitation> citations;
LearnerFit learnerFit;
List<NextStep> nextSteps;
Uncertainty uncertainty;
List<String> qualityFlags;
boolean requiresReview;
```

保留现有 `sources` 字段，并让 `citations == sources` 的语义一致。

## 5. 安全与隐私

- 不返回 raw chain-of-thought。
- 不返回 prompt、provider key、未脱敏 profileSnapshot。
- tool call summary 只返回运行时步骤和数量/策略摘要。
- RAG 权限过滤仍由 `RagQueryService` 负责。
- 不新增持久化，因此不改变长期记忆策略。

## 6. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller -> Service -> Runtime/Service |
| Frontend rules | PASS | 不改前端，不新增前端 LLM 调用 |
| Agent / RAG rules | PASS | RAG 仍通过 `RagQueryService`，无新增 unbounded loop |
| Security | PASS | 不新增依赖/密钥，响应摘要低敏 |
| API / Database | PASS | 响应 DTO 仅新增向后兼容字段；DB 不变 |
