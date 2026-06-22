# EVIDENCE-20260621 QaRuntime structured answer MVP

## 1. 范围

本证据对应 P0-3 `QaRuntime structured answer MVP`。

完成内容：

- 新增 `QaRuntime`，让 `/api/ai/qa` 通过显式运行时链路返回答案。
- 新增 `IntentRouter`、`ContextOrchestrator`、`FinalComposer`。
- `AiQaService` 降为薄门面，委托 `QaRuntime.run(...)`。
- `AiQaResponse` 保留 `sources`，新增 `citations`、`learnerFit`、`nextSteps`、`uncertainty`、`qualityFlags`、`requiresReview`。
- `toolCalls` 展示 `IntentRouter`、`RagQueryService`、`MemoryContextService`、`ContextOrchestrator`、`FinalComposer` 的低敏摘要。

## 2. RED 证据

接续上下文记录中，先写入并执行：

```powershell
mvn --% -Dtest=QaRuntimeTest test
```

结果：失败，符合预期。

关键失败：

- `QaRuntime` 不存在。
- `IntentRouter` 不存在。
- `ContextOrchestrator` 不存在。
- `FinalComposer` 不存在。
- `AiQaResponse` 结构化字段访问器尚不存在。

该失败证明 P0-3 的 runtime/schema 测试先于生产实现。

## 3. GREEN 证据

本轮接续后的首次 focused 验证：

```powershell
mvn --% -Dtest=QaRuntimeTest test
```

结果：失败，编译错误：

- `FinalComposer.java:125` 未结束的字符串文字。

处理：

- 修复 no-source fallback 文案，恢复 `"通用解释"` 等中文输出，并保持不伪造 citation 的语义。

修复后重新执行：

```powershell
mvn --% -Dtest=QaRuntimeTest test
```

结果：

- `QaRuntimeTest`: 2 run, 0 failures, 0 errors
- Build: SUCCESS

覆盖点：

- grounded 场景返回 `COURSE_GROUNDED / COURSE_RAG`。
- no-source 场景返回 `GENERAL_FALLBACK / NO_COURSE_SOURCE_FALLBACK`。
- `sources` 与 `citations` 保持一致。
- no-source 不伪造 citation。
- `learnerFit`、`nextSteps`、`uncertainty`、`qualityFlags`、`requiresReview` 符合结构化 schema。
- `toolCalls` 包含五个运行时步骤且不包含 raw question、chain-of-thought、provider key、teacher note。

## 4. Adjacent 回归

命令：

```powershell
mvn --% -Dtest=AiQaControllerTest,QaModePolicyTest,MemoryContextServiceTest,RagQueryServiceTest test
```

首次结果：

- `AiQaControllerTest`: 4 run, 0 failures, 0 errors
- `MemoryContextServiceTest`: 1 run, 0 failures, 0 errors
- `QaModePolicyTest`: 5 run, 0 failures, 0 errors
- `RagQueryServiceTest`: 22 run, 1 failure

失败点：

- `RagQueryServiceTest.rerankerProviderErrorIsSanitizedAndFallsBackToFusedCandidates`
- 期望 `ERROR_FALLBACK`，实际 `TIMEOUT_FALLBACK`。

调试复核：

```powershell
mvn --% -Dtest=RagQueryServiceTest#rerankerProviderErrorIsSanitizedAndFallsBackToFusedCandidates test
```

结果：

- 1 run, 0 failures, 0 errors
- Build: SUCCESS

判断：

- 该失败不在 P0-3 修改范围内。
- `RagQueryServiceTest` 的测试配置使用 `learning-os.rag.reranker-timeout-ms=10`，provider error 分支依赖异步任务在 10ms 内启动并抛错；组合测试高负载下可能先触发 timeout fallback。
- 未修改 RAG 生产代码或测试代码。

重新执行整组相邻回归：

```powershell
mvn --% -Dtest=AiQaControllerTest,QaModePolicyTest,MemoryContextServiceTest,RagQueryServiceTest test
```

结果：

- Total: 32 run, 0 failures, 0 errors
- Build: SUCCESS

## 5. Compile 验证

命令：

```powershell
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```

结果：

- Build: SUCCESS

## 6. 非阻塞说明

- 未运行全量 `mvn test`；本切片按 L 级计划执行 focused + adjacent + compile 验证，覆盖 runtime、API、mode policy、P0-2 memory context 和 RAG 相邻回归。
- 本切片未新增 DB schema、依赖、前端消费、真实模型 provider、SSE streaming 或 P0-4 verifier/eval gate。
- RAG reranker 10ms 异步测试存在一次可复现为环境抖动的失败；单测复核和整组复跑均通过，未在 P0-3 中越界修改。
