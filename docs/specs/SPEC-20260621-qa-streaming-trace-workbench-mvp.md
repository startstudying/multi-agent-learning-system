# SPEC-20260621 QA streaming and trace workbench MVP

## 1. Skill Selection Report

### Task Type

Frontend + backend API linkage / Agent Trace visibility / SSE streaming.

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 执行 L 级 PRD/REQ/SPEC/PLAN/TASK/CONTEXT、证据和验收流程 |
| spring-ai-agent-backend | 后端 Spring Boot SSE、service/controller 分层、结构化输出边界 |
| agent-trace-governance | 确保 trace、tool call、prompt/schema 版本展示保持低敏和可解释 |
| vue-edu-admin-frontend | Vue 3 学习工作台、API 集成、质量/trace 面板 |
| frontend-design | 保持工作台高密度、可扫描、非营销页风格 |
| test-driven-development | 新 endpoint 和前端消费路径先写失败测试 |
| verification-before-completion | 完成前必须 fresh 验证 |

### Missing Skills

无。

### GitHub Research Needed

No。复用项目已有 `SseEmitter`、`streamRequest`、`QaRuntime`、`RightThoughtPanel` 和 AI QA DTO，不引入新范式或依赖。

### New Project-Specific Skill To Create

无。

## 2. Size Classification

Size: L

Reason:

- 新增公开后端 SSE API。
- 前端生产/预发问答通道从 RAG stream 切到 AI QA stream。
- 涉及后端 API 合同、前端 API helper、学生工作台质量展示和测试。

Required Documents:

- PRD / REQ / SPEC / PLAN / TASK / CONTEXT
- Evidence / Acceptance after implementation

Can Skip:

- DB migration
- dependency review
- subagent reports

Upgrade Trigger:

- 若需要新 DB 表、真实模型 token streaming、教师/管理员独立质量看板或批量 eval runner，另开后续 L 切片。

## 3. Subagent Decision

Use Subagents: No

Reason:

- 当前线程未显式要求创建 subagent；本切片虽为 L，但实现边界窄且可由单 Codex 顺序完成。

Parallelism Level: L1 analysis by main Codex only

Implementation Mode: Single Codex

## 4. API Contract

### `POST /api/ai/qa/stream`

Consumes:

```http
Content-Type: application/json
```

Request:

```json
{
  "question": "Why does SQL JOIN duplicate rows?",
  "answerMode": "THINKING",
  "kbIds": ["kb_sql"],
  "courseId": "course_sql",
  "topK": 5,
  "requestId": "req_optional"
}
```

Produces:

```http
Content-Type: text/event-stream
```

Events:

```text
event:status
data:{"stage":"INTENT_ROUTING"}

event:status
data:{"stage":"RETRIEVING"}

event:status
data:{"stage":"MEMORY_CONTEXT"}

event:status
data:{"stage":"COMPOSING"}

event:status
data:{"stage":"VERIFYING"}

event:token
data:{"text":"..."}

event:done
data:{
  "answer":"...",
  "answerMode":"THINKING",
  "reasoningEffort":"medium",
  "sourceStatus":"COURSE_GROUNDED",
  "sourcePolicy":"COURSE_RAG",
  "sources":[...],
  "citations":[...],
  "verification":{"verdict":"PASS","gatePolicy":"BASIC_QA_VERIFIER_V1"},
  "traceId":"trc_x",
  "toolCalls":[...],
  "latencyMs":120
}
```

Error event:

```text
event:error
data:{"code":"AI_QA_STREAM_FAILED","message":"AI QA stream failed"}
```

## 5. Frontend Contract

新增 `frontend/src/api/aiQa.ts`：

- `queryAiQa(payload)`
- `streamAiQa(payload, handlers)`

新增/扩展类型：

- `AiQaResponse`
- `AiQaVerificationSummary`
- `AiQaVerificationCheck`
- `AiQaToolCallSummary`
- `AiQaLearnerFit`
- `AiQaNextStep`
- `AiQaUncertainty`

学生工作台：

- production/staging uses `streamAiQa`
- dev legacy path can still use `streamChat` + fallback RAG REST
- `applyAiQaResponse(response)` maps `sources/citations` to visible citations and stores quality metadata

## 6. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller handles SSE envelope and delegates answer generation to `AiQaService` / `QaRuntime` |
| Frontend rules | PASS | Uses shared `streamRequest`; no direct LLM/provider call |
| Agent / RAG rules | PASS | Reuses P0 `QaRuntime`, `AnswerVerifier`, RAG citation and traceId |
| Security | PASS | No secrets, no raw prompt, safe fixed stream error |
| API / Database | PASS | New endpoint documented here; no DB schema |

## 7. Implementation Notes

- `status` stages are transport progress hints; they do not expose raw reasoning or chain-of-thought.
- `token` may carry the full answer in this MVP, matching current RAG streaming behavior.
- Future true provider token streaming must be a separate slice with model gateway tracing and token accounting.
