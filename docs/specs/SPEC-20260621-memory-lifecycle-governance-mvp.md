# SPEC-20260621 Memory lifecycle governance MVP

## 1. Skill Selection Report

### Task Type

Backend memory lifecycle / DB schema / AI QA integration / privacy governance.

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 执行 L 级完整流程 |
| spring-ai-agent-backend | Spring Boot service/controller/repository 分层 |
| agent-trace-governance | 记忆与 trace/QA runtime 低敏治理一致 |
| test-driven-development | schema/service/context 行为先写失败测试 |
| verification-before-completion | 完成前 fresh 验证 |

### Missing Skills

无。

### GitHub Research Needed

No。复用本项目已有 JPA/Flyway/API envelope/MemoryPrivacyPolicy 模式。

### New Project-Specific Skill To Create

无。

## 2. Size Classification

Size: L

Reason:

- 涉及 DB migration。
- 涉及 AI QA 生产路径写入 memory lifecycle。
- 新增 backend API。
- 修改 MemoryContextService 注入来源。

Required Documents:

- PRD / REQ / SPEC / PLAN / TASK / CONTEXT
- Evidence / Acceptance after implementation

## 3. Subagent Decision

Use Subagents: No

Reason:

- 当前线程未显式要求创建 subagent；P2 MVP 实现边界清晰，单 Codex 顺序实现。

## 4. Data Model

### `kb_chat_session`

| Column | Type | Notes |
|---|---|---|
| `learner_id` | varchar(120) | owner |
| `course_id` | varchar(120) | optional course scope |
| `title` | varchar(255) | low-sensitive generated title |
| `status` | varchar(40) | `ACTIVE` / `ARCHIVED` |
| `salience_score` | double | 0..1 |
| `decay_at` | datetime(6) | lifecycle expiry |
| `created_at` | datetime(6) | created |
| `updated_at` | datetime(6) | updated |
| `deleted_at` | datetime(6) | soft delete |

### `kb_chat_message`

| Column | Type | Notes |
|---|---|---|
| `session_id` | varchar(80) | session id |
| `learner_id` | varchar(120) | owner denormalization |
| `role` | varchar(40) | `AI_QA_SUMMARY` |
| `content_summary` | varchar(2000) | low-sensitive only |
| `source_policy` | varchar(80) | e.g. `COURSE_RAG` |
| `salience_score` | double | 0..1 |
| `decay_at` | datetime(6) | lifecycle expiry |
| `editable` | boolean | user-editable |
| `created_at` | datetime(6) | created |
| `updated_at` | datetime(6) | updated |
| `deleted_at` | datetime(6) | soft delete |

## 5. API Contract

### `GET /api/ai/memory/sessions`

Response:

```json
[
  {
    "id": "mems_x",
    "courseId": "course_sql",
    "title": "AI QA COURSE_RAG",
    "status": "ACTIVE",
    "salienceScore": 0.82,
    "decayAt": "2026-09-21T00:00:00Z",
    "messages": [
      {
        "id": "memm_x",
        "contentSummary": "questionHash=...;questionLength=...;answerLength=...",
        "sourcePolicy": "COURSE_RAG",
        "editable": true
      }
    ]
  }
]
```

### `PATCH /api/ai/memory/messages/{messageId}`

Request:

```json
{ "summary": "low-sensitive learner correction" }
```

Response: updated `MemoryMessageResponse`.

### `DELETE /api/ai/memory/messages/{messageId}`

Response: deleted `MemoryMessageResponse` with `deleted=true`.

## 6. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller delegates to service; repositories only persistence |
| Frontend rules | PASS | No frontend change in P2 MVP |
| Agent/RAG rules | PASS | Memory writes use low-sensitive P0 privacy policy |
| Security | PASS | owner-only edit/delete, no secrets/raw prompt |
| API/Database | PASS | V23 documents schema change |

## 7. Implementation Notes

- `AiQaService.answer(...)` records memory after successful runtime response.
- `MemoryContextService` uses active session summaries first and learning events as fallback.
- User edits are sanitized with the same sensitive marker guard.
- Soft-deleted and decayed messages are excluded from list and context.
