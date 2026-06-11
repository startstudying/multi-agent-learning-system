# RETRO-20260610 P3-4 子任务：SSE production auth strategy

## 1. Feature Summary

完成 Chat/Tutor SSE production/staging 认证策略回归测试补强：

- 新增 `SseProductionAuthStrategyTest`。
- 覆盖 Chat/Tutor 在 production 下 no Bearer、invalid Bearer、valid Bearer + spoofed `X-User-Id`、Bearer `USER sub=admin` role-confusion。
- 覆盖 Chat/Tutor 在 staging 下 header-only auth 拒绝。
- 未改生产代码；当前后端认证边界已满足 fail-closed。

## 2. What Went Well

- M 级任务边界保持住了：只做后端 fail-closed 策略验证，没有顺手引入 query token / signed stream token / cookie session。
- Security Reviewer 明确指出了 EventSource 生产不可用与 query 泄露风险，帮助把“后端策略”与“前端生产可用流式客户端”拆开。
- Code Reviewer 初次 FAIL 抓到了 Tutor/staging 测试矩阵不完整，补齐后最终 PASS。
- Focused、adjacent、full backend 都有新鲜验证证据。

## 3. What Didn't Go Well

- 初始测试矩阵偏 Chat，Tutor production 与 staging Tutor 对称覆盖不足。
- 初始 SPEC 的测试规格没有及时同步最终 10 项覆盖矩阵，需要收口时补正。
- `@SpringBootTest` nested profile 矩阵启动较慢；这是可靠性换速度，短期可接受。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Production-like auth boundary tests should cover every transport variant, not only ordinary JSON API | Yes | `docs/skills/project-specific/auth-context-boundary.md` |
| SSE/EventSource auth should not be fixed by putting Bearer token in query | Yes | `docs/skills/project-specific/auth-context-boundary.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | Expert review after focused/adjacent test | Keep this; reviewer caught an actual matrix gap. |
| Testing | Started with Chat-heavy matrix | For paired endpoints such as Chat/Tutor, define a symmetric matrix before writing tests. |
| Documentation | SPEC test matrix lagged final implementation | Update SPEC immediately after reviewer-driven scope correction. |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| Implement production frontend streaming client with Bearer, preferably `fetch` / `ReadableStream` | Future Codex | P3-4 follow-up |
| Avoid full `question` in SSE GET URL through POST-created stream session or equivalent protocol | Future Codex | P3-4 / RAG privacy follow-up |
| Continue dev/test legacy fallback cleanup | Future Codex | P3-4 follow-up |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory files
- [x] SKILL_REGISTRY.md not required; no new skill created
- [x] ARCHITECTURE_BASELINE.md not required; no architecture rule changed
