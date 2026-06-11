# TASK-20260611 P3-4 子任务：formal production streaming design

## 1. Objective

实现 production/staging 学生端 RAG 正式流式传输：前端通过 POST stream 发送 `question` / `kbIds`，后端返回 `text/event-stream`，不再依赖敏感 GET SSE URL。

## 2. Boundary

本任务只完成学生 RAG production streaming。Tutor production stream client、真实模型 token streaming、stream session persistence、signed stream token 不在范围内。

## 3. Required Docs

- `docs/requirements/REQ-20260611-p3-4-formal-production-streaming-design.md`
- `docs/specs/SPEC-20260611-p3-4-formal-production-streaming-design.md`
- `docs/plans/PLAN-20260611-p3-4-formal-production-streaming-design.md`
- `docs/context/CONTEXT-20260611-p3-4-formal-production-streaming-design.md`

## 4. Work Items

1. RED backend stream endpoint test。
2. GREEN backend `POST /api/rag/query/stream`。
3. RED/GREEN production Bearer/JWT matrix for new POST stream。
4. RED frontend production/staging stream transport test。
5. GREEN frontend shared stream helper + `streamRagQuery` + student page integration。
6. Focused/adjacent/full verification。
7. Evidence / Acceptance / Changelog / Memory / TODO update。

## 5. Context Pack Summary

See `docs/context/CONTEXT-20260611-p3-4-formal-production-streaming-design.md`。

## 6. Acceptance Criteria

- [x] `POST /api/rag/query/stream` returns `text/event-stream` events.
- [x] Backend stream endpoint uses JWT-derived `userId/admin/teacher` role facts.
- [x] Missing/invalid Bearer in production returns 401 before async work and no `RagQueryService` call.
- [x] Frontend production/staging uses fetch stream, not native `EventSource`.
- [x] Stream URL contains no `question`, `kbIds`, JWT, or token.
- [x] `status` / `token` / `done` update UI state.
- [x] Dev/test legacy SSE behavior remains compatible.
- [x] Verification evidence is recorded.

## 7. Done Definition

M Done Definition:

- REQ / SPEC / PLAN / TASK / CONTEXT exist.
- Code implemented within Context Pack.
- Focused and adjacent tests run; full tests run when feasible.
- Evidence and Acceptance exist.
- Changelog and memory updated.
- P3-4 parent remains open unless every remaining P3-4 child is complete.
