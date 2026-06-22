# PLAN-20260621 Basic Verifier / Eval Gate MVP

## 1. Skill Selection Report

### Task Type

后端 Agent/RAG 回答质量校验与评测 gate。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 执行 L 级文档、实现、证据、验收和 memory 更新 |
| `executing-plans` | 继续执行既有落地计划 |
| `test-driven-development` | verifier / eval gate 必须先 RED 后实现 |
| `spring-ai-agent-backend` | 保持 Controller / Service / Runtime / Quality 分层 |
| `educational-rag-pipeline` | 校验 citation/no-source/RAG grounding |
| `agent-trace-governance` | 保持 toolCalls 为低敏可解释摘要 |
| `security-review` | 检查 prompt/provider key/teacher note 泄露风险 |
| `Confidence Check` | 实施前确认无重复 verifier/gate 和架构偏离 |

### Missing Skills

无。

### GitHub Research Needed

No。项目已有 Evaluation Set/Run、RAG Evaluation、P0-1/P0-2/P0-3 基础；本切片不引入新依赖或外部代码。

### New Project-Specific Skill To Create

暂无。

## 2. Size Classification

Size: L

Reason:

- 新增回答质量 gate。
- 修改 `/api/ai/qa` 响应 DTO。
- 涉及 Agent/RAG 质量治理和 Evaluation Set/Run 复用。

Required Documents:

- PRD
- REQ
- SPEC
- PLAN
- TASK
- CONTEXT

Can Skip:

- Dependency review：不新增依赖。
- DB schema review：不修改 DB schema。
- Frontend implementation：本切片后端向后兼容，前端后续可选择消费 `verification`。

Upgrade Trigger:

- 若新增持久化表、管理 API、前端工作台、真实模型 reviewer 或批量 eval runner，则拆成后续 L 切片。

## 3. Subagent Decision

Use Subagents: No

Reason:

- 当前可用 `spawn_agent` 工具明确要求用户显式要求 subagents / parallel agent work 才可使用。
- 本切片通过主线程文档、TDD、focused/adjacent tests 和后置 evidence 控制风险。

Parallelism Level: N/A

Implementation Mode: Single Codex。

## 4. Confidence Check

| Check | Result | Evidence |
|---|---|---|
| No duplicate implementations | PASS | `rg` 未发现现有 `AnswerVerifier` / `QaEvalGate` |
| Architecture compliance | PASS | 复用 `QaRuntime`、`AiQaDtos`、Evaluation Set/Run，不新增依赖 |
| Official docs needed | PASS | 不调用新的外部 SDK/API |
| OSS references | PASS | 复用既有 roadmap/RAG evaluation，不复制外部代码 |
| Root cause identified | PASS | P0-3 已结构化回答，但缺少可回归 verifier/eval gate |

Confidence: 0.95

## 5. Implementation Steps

1. 写 `AnswerVerifierTest` RED，覆盖 grounded pass、no-source pass、privacy/schema fail。
2. 写 `QaEvalGateTest` RED，覆盖 low sample、missing metrics、strategy baseline/candidate、pass。
3. 更新 `QaRuntimeTest` / `AiQaControllerTest` RED，要求 `verification` 字段和 `AnswerVerifier` tool call。
4. 新增 `AnswerVerifier` 和 `QaEvalGate`。
5. 更新 `AiQaDtos`、`QaRuntime`、`FinalComposer` 接入 verifier。
6. 扩展 `EvaluationSetService` 支持 `AI_QA_ANSWER`。
7. 扩展 `EvaluationRunService` QA gate 指标白名单。
8. 运行 focused 和 adjacent tests。
9. 记录 Evidence / Acceptance，更新落地计划、memory、changelog。

## 6. Test Plan

Focused:

```powershell
cd backend
mvn --% -Dtest=AnswerVerifierTest,QaEvalGateTest,QaRuntimeTest test
```

Adjacent:

```powershell
cd backend
mvn --% -Dtest=AiQaControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,RagEvaluationServiceTest,RagQueryServiceTest test
```

Compile:

```powershell
cd backend
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```
