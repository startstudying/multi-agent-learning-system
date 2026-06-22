# EVIDENCE-20260621 Basic Verifier / Eval Gate MVP

## 1. 范围

本证据对应 P0-4 `Basic Verifier / Eval Gate MVP`。

完成内容：

- 新增 `AnswerVerifier`，对 `/api/ai/qa` 的结构化回答执行 schema、citation、no-source、privacy 基础校验。
- 新增 `QaEvalGate`，支持最小 QA gate verdict：`PASS` / `FAIL` / `INSUFFICIENT_SAMPLE`。
- `AiQaResponse` 新增 `verification` 字段，包含 verdict、gate policy、check 明细、summary、requiresReview。
- `QaRuntime` 在 `FinalComposer` 之后执行 verifier，并追加低敏 `AnswerVerifier` tool call。
- `EvaluationSetService` 支持 `AI_QA_ANSWER` 类型，要求 question 与非空 quality criteria。
- `EvaluationRunService` 支持 QA gate 指标白名单：`schemaPassRate`、`verificationPassRate`、`privacyLeakRate`。

## 2. RED 证据

接续上下文记录中，先写入并执行 P0-4 测试：

```powershell
mvn --% -Dtest=AnswerVerifierTest,QaEvalGateTest,QaRuntimeTest test
```

结果：失败，符合预期。

关键失败：

- `AnswerVerifier` 不存在。
- `QaEvalGate` 不存在。
- `AiQaResponse.verification` 与 `VerificationSummary` DTO 尚不存在。
- `QaRuntime` 尚未追加 verifier tool call。

该失败证明 P0-4 的 verifier / eval gate 测试先于生产实现。

## 3. GREEN 证据

本轮 fresh focused 验证：

```powershell
mvn --% -Dtest=AnswerVerifierTest,QaEvalGateTest,QaRuntimeTest test
```

结果：

- `AnswerVerifierTest`: 4 run, 0 failures, 0 errors
- `QaEvalGateTest`: 4 run, 0 failures, 0 errors
- `QaRuntimeTest`: 2 run, 0 failures, 0 errors
- Total: 10 run, 0 failures, 0 errors
- Build: SUCCESS

覆盖点：

- grounded 场景 verifier verdict 为 `PASS`。
- 可检测隐私泄露标记并返回 `FAIL`。
- no-source 场景不伪造 citation，但返回 review requirement。
- runtime 返回 `verification` 并追加 `AnswerVerifier` tool call。
- eval gate 覆盖 `PASS`、`FAIL`、`INSUFFICIENT_SAMPLE`。

## 4. Adjacent 回归

命令：

```powershell
mvn --% -Dtest=AiQaControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,RagEvaluationServiceTest,RagQueryServiceTest test
```

结果：

- `AiQaControllerTest`: 4 run, 0 failures, 0 errors
- `EvaluationSetServiceTest`: 8 run, 0 failures, 0 errors
- `RagEvaluationServiceTest`: 3 run, 0 failures, 0 errors
- `RagQueryServiceTest`: 22 run, 0 failures, 0 errors
- Total: 50 run, 0 failures, 0 errors
- Build: SUCCESS

覆盖点：

- `/api/ai/qa` JSON 响应包含 `verification.verdict` 与 `gatePolicy`。
- Evaluation Set 接受 `AI_QA_ANSWER` 样本并校验必要字段。
- Evaluation Run 接受 QA gate 指标白名单。
- 既有 RAG evaluation / query 回归不受 P0-4 影响。

## 5. Compile 验证

命令：

```powershell
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```

结果：

- Build: SUCCESS
- 输出显示 `Nothing to compile - all classes are up to date.`

## 6. 隐私关键词检查

命令：

```powershell
rg -n "teacher_note|provider key|provider_key|apiKey|api_key|sk-|chain-of-thought|rawPrompt|profileSnapshot" backend/src/main/java/com/learningos/aiqa backend/src/test/java/com/learningos/aiqa
```

结果说明：

- 生产代码命中仅出现在 `MemoryContextService` 的敏感标记过滤规则和 `AnswerVerifier` 的敏感标记列表。
- 测试代码命中为故意构造的泄露样本或断言。
- 未发现 AI QA 生产响应路径新增 raw prompt、provider key、teacher note、profileSnapshot 或 chain-of-thought 暴露点。

## 7. 非阻塞说明

- 未运行全量 `mvn test`；本切片按 L 级计划执行 focused + adjacent + compile + privacy grep。
- 本切片未新增 DB schema、依赖、前端消费、真实模型 reviewer、批量 eval runner、P1 streaming/workbench 或 P2 memory lifecycle。
- Mockito 动态 agent warning 为现有测试运行环境提示，不影响本次测试结论。
