# REQ-20260621 Memory lifecycle governance MVP

## 1. 功能需求

### R1 Session schema

- 扩展 `kb_chat_session`：
  - `learner_id`
  - `course_id`
  - `title`
  - `status`
  - `salience_score`
  - `decay_at`
  - `created_at`
  - `updated_at`
  - `deleted_at`
- 扩展 `kb_chat_message`：
  - `session_id`
  - `learner_id`
  - `role`
  - `content_summary`
  - `source_policy`
  - `salience_score`
  - `decay_at`
  - `editable`
  - `created_at`
  - `updated_at`
  - `deleted_at`

### R2 Low-sensitive QA memory write

- AI QA 完成后写入 session memory。
- content summary 不保存 raw question、完整 answer、prompt、provider key、teacher note、raw profileSnapshot。
- summary 至少包含：
  - question hash/length
  - answer length
  - sourcePolicy
  - verification verdict
  - citation count

### R3 Lifecycle policy

- salience 基于 citation、verification、requiresReview、sourcePolicy 计算。
- decayAt 基于 salience 计算。
- 低 salience 或 no-source/review-needed 记忆生命周期较短。

### R4 User governance API

- `GET /api/ai/memory/sessions`：列出当前用户未删除 session 和未过期 message。
- `PATCH /api/ai/memory/messages/{messageId}`：当前用户编辑 editable message 的低敏 summary。
- `DELETE /api/ai/memory/messages/{messageId}`：当前用户软删除 message。

### R5 Memory context consumption

- `MemoryContextService` 的 recent session summary 优先读取 active session messages。
- 已删除、已过期 message 不得进入 `MemoryContext`。
- 若没有 session message，保留学习事件 fallback。

## 2. 安全需求

- 后端按当前用户隔离 memory message。
- 非 owner 不得编辑/删除。
- API response 不返回 raw question、完整 answer 或 hidden context。
- 不新增前端 LLM/provider 参数。

## 3. 验收需求

- Service tests 覆盖 record/edit/delete/list。
- MemoryContextService tests 覆盖未删除未过期 memory 注入和 deleted/expired 排除。
- Migration tests 覆盖 V23 schema 文本。
- Backend focused/adjacent/compile 通过。
