# PLAN-20260621 QaRuntime structured answer MVP

## 1. Skill Selection Report

### Task Type

后端 Agent/RAG 运行时与 API 响应契约增强。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 执行 L 级文档、实现、证据、验收和 memory 更新 |
| `executing-plans` | 继续执行既有落地计划 |
| `test-driven-development` | 新运行时和响应字段必须先 RED 后实现 |
| `spring-ai-agent-backend` | 保持 Controller / Service / Runtime 分层和结构化输出 DTO |
| `educational-rag-pipeline` | 维护 citation/no-source/RAG permission 边界 |
| `agent-trace-governance` | toolCalls 输出低敏运行时步骤摘要 |
| `learner-profile-agent` | 复用低敏学习者上下文，不泄露教师笔记 |
| `security-review` | 检查 prompt/provider key/teacher note 泄露风险 |
| `Confidence Check` | 实施前确认没有重复运行时和架构偏离 |

### Missing Skills

无。

### GitHub Research Needed

No。已有 `docs/subagents/runs/RUN-20260621-memory-chatgpt-quality-external-research.md` 和 `docs/research/github-references/GITHUB-20260621-wiki-rag-reference.md`，本切片不新增外部依赖。

### New Project-Specific Skill To Create

暂无。

## 2. Size Classification

Size: L

Reason:

- 新增 `QaRuntime` 运行时边界。
- 修改 `/api/ai/qa` 响应 DTO，新增结构化字段。
- 涉及 Agent/RAG workflow 和 API contract。

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
- Frontend implementation：本切片后端向后兼容，前端后续可选择消费新字段。

Upgrade Trigger:

- 若接入真实新模型 provider、DB 持久化、流式接口、前端联动或 verifier/eval gate，则拆分为后续 L 切片。

## 3. Subagent Decision

Use Subagents: No

Reason:

- 当前可用 subagent 工具要求用户显式要求 subagents / parallel agent work 才可 spawn。
- 本切片通过主线程文档、TDD、focused/adjacent tests 和后置 evidence 控制风险。

Parallelism Level: N/A

Implementation Mode: Single Codex。

## 4. Confidence Check

| Check | Result | Evidence |
|---|---|---|
| No duplicate implementations | PASS | `rg` 未发现现有 `QaRuntime` / `IntentRouter` |
| Architecture compliance | PASS | 沿用 Spring Service 和 `AiQaDtos`，不新增依赖 |
| Official docs needed | PASS | 不调用新的外部 SDK/API |
| OSS references | PASS | 复用既有 roadmap/external research，不复制代码 |
| Root cause identified | PASS | `AiQaService` 仍直接包装 RAG 与响应组合 |

Confidence: 0.95

## 5. Implementation Steps

1. 写 `QaRuntimeTest` RED，要求结构化字段和 tool calls。
2. 写 `AiQaControllerTest` RED，要求 API 返回 `citations`、`learnerFit`、`nextSteps`、`uncertainty`。
3. 新增 runtime records/classes。
4. 将 `AiQaService` 改成委托 `QaRuntime`。
5. 保留 `sources` 并新增 `citations`，保证兼容。
6. 运行 focused 和 adjacent tests。
7. 记录 Evidence / Acceptance，更新落地计划、memory、changelog。

## 6. Test Plan

Focused:

```powershell
cd backend
mvn --% -Dtest=QaRuntimeTest test
```

Adjacent:

```powershell
cd backend
mvn --% -Dtest=AiQaControllerTest,QaModePolicyTest,MemoryContextServiceTest,RagQueryServiceTest test
```

Compile:

```powershell
cd backend
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```
