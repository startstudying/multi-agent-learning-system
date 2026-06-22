# TASK-20260621 Basic Verifier / Eval Gate MVP

## 1. 目标

完成 P0-4：为 `/api/ai/qa` 增加基础回答 verifier 和最小 eval gate，让 citation/no-source/privacy/schema 质量可回归。

## 2. 范围

包含：

- `AnswerVerifier`
- `QaEvalGate`
- `AiQaResponse.verification`
- `QaRuntime` 接入 `AnswerVerifier`
- `EvaluationSetService` 支持 `AI_QA_ANSWER`
- `EvaluationRunService` 支持 QA gate 指标白名单
- focused / adjacent tests

不包含：

- DB schema
- 新 REST API 路径
- 前端消费
- 真实模型 reviewer
- 批量 eval runner
- P1 streaming / workbench
- P2 memory lifecycle

## 3. 允许修改文件

- `backend/src/main/java/com/learningos/aiqa/api/dto/AiQaDtos.java`
- `backend/src/main/java/com/learningos/aiqa/application/runtime/QaRuntime.java`
- `backend/src/main/java/com/learningos/aiqa/application/runtime/FinalComposer.java`
- `backend/src/main/java/com/learningos/aiqa/application/quality/*.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationSetService.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java`
- `backend/src/test/java/com/learningos/aiqa/application/quality/*.java`
- `backend/src/test/java/com/learningos/aiqa/application/runtime/QaRuntimeTest.java`
- `backend/src/test/java/com/learningos/aiqa/api/AiQaControllerTest.java`
- `backend/src/test/java/com/learningos/evaluation/application/EvaluationSetServiceTest.java`
- `backend/src/test/java/com/learningos/evaluation/application/EvaluationRunServiceTest.java`
- 本任务 P0-4 docs / evidence / acceptance
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`

## 4. 禁止修改文件

- `frontend/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/**`
- AI provider 配置和密钥
- RAG parser/vector/index worker
- ResourceGeneration / ReviewGate 生产代码

## 5. 验收标准

- `AnswerVerifierTest` 覆盖 pass/fail。
- `QaEvalGateTest` 覆盖 `PASS` / `FAIL` / `INSUFFICIENT_SAMPLE`。
- `QaRuntimeTest` 覆盖 `verification` 和 `AnswerVerifier` tool call。
- `AiQaControllerTest` 覆盖 API JSON 中的 `verification` 字段。
- Evaluation Set 支持 `AI_QA_ANSWER`。
- Evaluation Run 支持 QA gate 指标白名单。
- Evidence / Acceptance 写入完成。

## 6. Evidence

已创建：

- `docs/evidence/EVIDENCE-20260621-qa-verifier-eval-gate-mvp.md`

验证结果：

- Focused：`mvn --% -Dtest=AnswerVerifierTest,QaEvalGateTest,QaRuntimeTest test`，10 run, 0 failures, 0 errors。
- Adjacent：`mvn --% -Dtest=AiQaControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,RagEvaluationServiceTest,RagQueryServiceTest test`，50 run, 0 failures, 0 errors。
- Compile：`mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile`，Build SUCCESS。
- Privacy grep：生产命中仅为敏感标记过滤/校验规则，测试命中为故意样本。

## 7. Acceptance Verdict

已创建：

- `docs/acceptance/ACCEPT-20260621-qa-verifier-eval-gate-mvp.md`

Verdict: PASS

P0-4 已完成：`/api/ai/qa` 返回 `verification`，runtime 追加 `AnswerVerifier` tool call，Evaluation Set 支持 `AI_QA_ANSWER`，Evaluation Run 支持 QA gate 指标白名单。本任务未实现 P1 streaming/workbench 或 P2 memory lifecycle。
