# ACCEPT-20260610 P3-4 子任务：SSE production auth strategy

## Acceptance Summary

Status: ACCEPTED.

P3-4 `SSE production auth strategy` 子任务已完成。Chat/Tutor SSE 在 production/staging 下被回归测试固定为 Bearer/JWT fail-closed：无 Bearer、invalid Bearer、header-only spoofing 不会启动 SSE async，也不会调用 `RagQueryService`；valid Bearer 使用 JWT subject 与 roles，忽略 spoofed `X-User-Id`；Bearer `USER sub=admin` 不会获得 admin facts。

## Acceptance Criteria

| Criteria | Status | Evidence |
|---|---|---|
| production Chat SSE no Bearer returns `UNAUTHORIZED` and does not start async | PASS | `SseProductionAuthStrategyTest.chatStreamInProductionRejectsMissingBearerBeforeStartingAsyncWork`。 |
| production Chat SSE invalid Bearer returns `UNAUTHORIZED` and does not fallback | PASS | `SseProductionAuthStrategyTest.chatStreamInProductionRejectsInvalidBearerBeforeStartingAsyncWork`。 |
| production Tutor SSE no Bearer returns `UNAUTHORIZED` and does not start async | PASS | `SseProductionAuthStrategyTest.tutorStreamInProductionRejectsMissingBearerBeforeStartingAsyncWork`。 |
| production Tutor SSE invalid Bearer returns `UNAUTHORIZED` and does not fallback | PASS | `SseProductionAuthStrategyTest.tutorStreamInProductionRejectsInvalidBearerBeforeStartingAsyncWork`。 |
| staging Chat/Tutor header-only auth is rejected | PASS | `chatStreamInStagingRejectsHeaderOnlyAuthBeforeStartingAsyncWork` and `tutorStreamInStagingRejectsHeaderOnlyAuthBeforeStartingAsyncWork`。 |
| valid Bearer Chat/Tutor SSE uses JWT subject and roles, ignoring spoofed `X-User-Id` | PASS | `chatStreamInProductionUsesBearerRolesAndIgnoresSpoofedUserIdHeader` and `tutorStreamInProductionUsesBearerRolesAndIgnoresSpoofedUserIdHeader`。 |
| Bearer `USER sub=admin` does not gain admin facts | PASS | `chatStreamInProductionDoesNotInferAdminFromBearerSubjectName` and `tutorStreamInProductionDoesNotInferAdminFromBearerSubjectName`。 |
| No new query token / signed token / cookie session / dependency / DB schema | PASS | Only test/docs changed; production code unchanged. |
| Expert subagent parallel development/review | PASS | Architect / Test / Security / Code Review expert outputs recorded under `docs/subagents/runs/` and integrated. |
| Evidence / Acceptance / Changelog / Memory updated | PASS | Evidence, this Acceptance, Retro, Changelog, Project/Backend/RAG memory, and backend TODO updated. |

## Verification

- Focused: `mvn --% -Dtest=SseProductionAuthStrategyTest test` -> `10 run, 0 failures, 0 errors`.
- Adjacent: `mvn --% -Dtest=SseProductionAuthStrategyTest,ChatControllerTest,TutorControllerTest,SecurityFilterChainTest,DevAuthFilterTest,SecurityJwtAuthenticationTest test` -> `38 run, 0 failures, 0 errors`.
- Full backend: `mvn test` -> `530 run, 0 failures, 0 errors, 1 skipped`.
- Code review expert: initial FAIL for Tutor/staging matrix gaps; final PASS after matrix completion.

## Accepted Limitations / Follow-up

- P3-4 parent is not complete.
- 本轮只证明后端生产 SSE fail-closed；没有解决生产前端原生 `EventSource` 不能携带 Bearer header 的可用性问题。
- 后续建议独立任务实现 `fetch` / `ReadableStream` + Bearer，或设计短 TTL 一次性 signed stream token。
- 后续应避免把完整 `question` 放在 SSE GET URL 中。
- Dev/test legacy fallback cleanup remains open.
