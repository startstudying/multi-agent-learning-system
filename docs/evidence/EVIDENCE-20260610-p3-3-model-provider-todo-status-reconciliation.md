# EVIDENCE-20260610 P3-3 子任务：model provider TODO 状态对账

## Scope

本证据只记录 P3-3 TODO 状态对账，不新增后端功能代码。

## Reconciled TODO Items

| TODO item | Status | Evidence |
|---|---|---|
| 用 Spring AI/Spring AI Alibaba 接入真实 Chat/Embedding 模型 | Done for minimum Spring AI OpenAI-compatible adapter | `ACCEPT-20260608-real-model-provider-adapter.md` 明确 FR-01 / FR-02 已通过 Spring AI OpenAI-compatible starter 接入 Chat provider adapter 和 Embedding provider adapter；DashScope 作为后续增强，不阻塞本项最小验收。 |
| 模型 provider、model name、prompt version、latency、token、error 全量落日志 | Done for current model-call logging boundary | P3-3-A 已记录 gateway response model/latency/token/cost 和安全错误；P3-3-B `model_call_log.provider` schema / entity / recorder / gateway normalization 已验收；P2-1 prompt metadata 已有 `promptCode/promptVersion/temperature/structuredOutputSchema`。 |

## Evidence Read

- `docs/acceptance/ACCEPT-20260608-real-model-provider-adapter.md`
  - Focused tests: 18 tests.
  - Adjacent regression: 53 tests.
  - Full backend: 357 run, 0 failures, 0 errors, 1 skipped.
  - Acceptance conclusion: minimum Spring AI OpenAI-compatible provider adapter completed; DashScope / VectorDB are follow-up enhancements.
- `docs/security/DEPENDENCY-REVIEW-20260608-real-model-provider-adapter.md`
  - `spring-ai-starter-model-openai` reviewed and approved with conditions.
  - Secrets must stay in environment / secret manager, not source/docs/memory.
  - SDK usage is limited to `AiModelGateway` / `EmbeddingService`.
- `docs/acceptance/ACCEPT-20260608-model-call-provider-observability.md`
  - `model_call_log.provider` migration, entity, success/failure recorder paths, provider normalization, and safe error semantics are PASS.
  - Real MySQL smoke was blocked by local credential mismatch, but H2/schema convergence and full backend passed.
- Current code search:
  - `backend/pom.xml` contains Spring AI BOM `1.0.8` and `spring-ai-starter-model-openai`.
  - `AiModelGateway` uses Spring AI `ChatModel`.
  - `EmbeddingService` uses Spring AI `EmbeddingModel`.
  - `ModelCallLog`, `AgentRunRecorder`, and `V18__model_call_provider_observability.sql` include provider persistence.

## Verification

Read-only/document verification command:

```powershell
cd D:\多元agent
rg -n "P3-3|Real Model Provider|model_call_log.provider|Spring AI|EmbeddingModel|ChatModel" docs\memory\PROJECT_MEMORY.md docs\memory\BACKEND_MEMORY.md docs\planning\backend-architecture-todolist.md
```

Focused code verification from existing P3-3-B/C suites:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest,AgentRunRecorderTest,SchemaConvergenceMigrationTest test
```

## Accepted Follow-up

- DashScope / Spring AI Alibaba provider remains an optional provider-family enhancement.
- Real external provider smoke remains environment-dependent and should run only with controlled secrets.
- VectorDB remains a later P3-2/P3-3 enhancement and is not required for closing these two P3-3 TODO lines.

## Acceptance Verdict

Verdict: PASS.

- TASK acceptance criteria 1: PASS. `backend-architecture-todolist.md` 已将 Spring AI OpenAI-compatible Chat/Embedding adapter TODO 标记完成，并保留 DashScope / external provider smoke 为 follow-up。
- TASK acceptance criteria 2: PASS. provider/model/promptVersion/latency/token/error logging TODO 已基于 P3-3-A 与 P3-3-B 证据标记完成。
- TASK acceptance criteria 3: PASS. 本对账任务未修改后端代码、schema、dependency、API、frontend 或 secret 文件。
