# ACCEPT-20260611 P3-4 子任务：formal production streaming design

## 1. Acceptance Verdict

Accepted。

本子任务已完成 M Done Definition：REQ / SPEC / PLAN / TASK / CONTEXT 已存在，代码已实现，focused / adjacent / full 验证已运行并通过，Evidence 已记录，Changelog / Memory / TODO 已更新。

## 2. Acceptance Criteria

| Criteria | Verdict | Evidence |
|---|---|---|
| `POST /api/rag/query/stream` returns `text/event-stream` events | PASS | `ChatController.java:78-83`；`ChatControllerTest.postRagQueryStreamUsesRequestBodyAndEmitsStatusTokenAndDoneEvents` |
| Backend stream endpoint uses JWT-derived `userId/admin/teacher` role facts | PASS | `ChatController.java:84-107`；`SseProductionAuthStrategyTest` valid Bearer + spoofed header matrix |
| Missing Bearer in production returns 401 before async work and no `RagQueryService` call | PASS | `SseProductionAuthStrategyTest.postRagStreamInProductionRejectsMissingBearerBeforeStartingAsyncWork` |
| Invalid Bearer in production returns 401 before async work and no `RagQueryService` call | PASS | `SseProductionAuthStrategyTest.postRagStreamInProductionRejectsInvalidBearerBeforeStartingAsyncWork` |
| Valid Bearer + spoofed `X-User-Id` uses JWT subject/roles | PASS | `SseProductionAuthStrategyTest.postRagStreamInProductionUsesBearerRolesAndIgnoresSpoofedUserIdHeader` |
| `USER sub=admin` does not infer admin role | PASS | `SseProductionAuthStrategyTest.postRagStreamInProductionDoesNotInferAdminFromBearerSubjectName` |
| Frontend production/staging uses fetch stream, not native `EventSource` | PASS | `StudentDashboard.vue:391-404`；`App.spec.ts:973-1037` |
| Stream URL contains no `question`, `kbIds`, JWT, or token | PASS with note | Tests assert no `question=` / `kbIds=` in URL；implementation uses POST body and no query token. Token-string URL assertions remain follow-up hardening. |
| `status` / `token` / `done` update UI state | PASS | `StudentDashboard.vue:457-484`；`App.spec.ts:973-1037` |
| Dev/test legacy SSE behavior remains compatible | PASS | `App.spec.ts:919-971` |
| Verification evidence is recorded | PASS | `docs/evidence/EVIDENCE-20260611-p3-4-formal-production-streaming-design.md` |

## 3. Verification Summary

- Backend adjacent: `36 run, 0 failures, 0 errors, 0 skipped`
- Backend full: `601 run, 0 failures, 0 errors, 1 skipped`
- Frontend focused: `31 passed`
- Frontend typecheck: exit code `0`
- Frontend build: exit code `0`

## 4. Accepted Limitations

- 本任务实现的是 transport streaming，不承诺真实模型 token 增量 streaming。
- Legacy GET SSE endpoint 仍存在，仅作为 dev/test/demo compatibility；production/staging 客户端不得使用。
- Bearer token 在前端仅提供 `setApiBearerToken(...)` 注入能力；真实登录态接入不在本任务范围。
- 独立 `citation` stream event 未实现；引用在 `done.sources` 中返回。

## 5. Parent Epic Status

`P3-4 权限与安全加固` 父项不标记完成。

保持 open 的主要后续：

- 更完整业务权限矩阵抽样复核。
- 其他教师端数据访问范围继续扩展。
- 可选 hardening：production profile 下限制 legacy GET SSE、补 token 字符串 URL 断言、统一 Chat/Tutor GET stream error event 合同。
