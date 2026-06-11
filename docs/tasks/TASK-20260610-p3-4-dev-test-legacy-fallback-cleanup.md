# TASK-20260610 P3-4 子任务：dev/test legacy fallback cleanup

## 目标

收紧 dev/test legacy subject-name fallback 的边界：`X-User-Id` 无 Bearer 兼容路径可继续在 dev/test 使用，但 Spring Security `JwtAuthenticationToken` 建立的 Bearer 身份不得再因为 `sub = "admin"` 或 `sub = "teacher_*"` 被 `CurrentUserService.isAdmin()` / `isTeacherUser()` 提权。

## Task Type

Security cleanup / auth-context boundary hardening。

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 原始 P3-4 TODO 推进，必须走项目 S/M/L 工作流。 |
| `auth-context-boundary` | 本任务直接处理 Bearer JWT、dev/test header fallback、subject-name role confusion 边界。 |
| `test-driven-development` | 先补回归测试证明 dev/test JWT subject-name 不得提权，再做最小生产修正。 |
| `security-review` | 识别兼容保留与可删除/需绕开的安全入口。 |

Missing Skills: 无。

GitHub Research Needed: No。该任务是项目内部 auth-context 兼容边界收口，不需要外部参考。

New Project-Specific Skill To Create: 不需要，已有 `auth-context-boundary` 覆盖。

## Size Classification

- Size: S - Small Slice / Fast Lane
- Reason: 仅触及 `CurrentUserService` 的角色推断边界和 auth 单测；不改 REST API、DTO、数据库 schema、依赖、前端/后端合同或 Spring Security 配置。
- Required Documents: 本 mini TASK，内嵌 Context Pack。
- Can Skip: 独立 PRD/REQ/SPEC/PLAN/CONTEXT、独立 retrospective。
- Upgrade Trigger: 若需要移除 dev/test header fallback、调整 `SecurityFilterChain`、改变生产认证策略、或批量改业务 Controller 权限路径，则升级为 M。

## Subagent Decision

- Use Subagents: No
- Reason: 单 auth 模块小切片，边界清晰。
- Parallelism Level: N/A
- Implementation Mode: Single Codex

## Context Pack

### Related Memory and Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/skills/project-specific/auth-context-boundary.md`

### Existing Real Entrances

- 兼容保留：`DevAuthFilter` 在 dev/test 且无 Bearer 时使用 `X-User-Id`，并为 `admin` / `teacher_*` 派生 legacy roles。
- 可删除/收紧：`CurrentUserService.isAdmin()` / `isTeacherUser()` 对 Spring Security JWT 用户在 dev/test 下继续按 subject-name 推断角色。
- 需 controller 绕开的风险：业务 Controller 若仍直接使用 `CurrentUserService.isAdmin()` / `isTeacherUser()`，在 dev/test Bearer `roles=["USER"], sub="admin"` 或 `sub="teacher_*"` 时可能被误判提权。

### Allowed Files

- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
- `backend/src/test/java/com/learningos/common/auth/SecurityJwtAuthenticationTest.java`
- `docs/tasks/TASK-20260610-p3-4-dev-test-legacy-fallback-cleanup.md`

### Disallowed Files

- `frontend/**`
- `backend/pom.xml`
- database migration files
- business Controller / Service modules outside `common/auth`
- changelog / memory / planning / evidence files,由主线统一集成

## Planned Test Commands

RED / focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SecurityJwtAuthenticationTest test
```

Adjacent auth:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest,SecurityFilterChainTest,SseProductionAuthStrategyTest test
```

## Acceptance Criteria

- [x] Dev/test 无 Bearer `X-User-Id` fallback 保持兼容。
- [x] Dev/test Spring Security JWT `sub=admin, roles=["USER"]` 不再让 `isAdmin()` 为 true。
- [x] Dev/test Spring Security JWT `sub=teacher_1, roles=["USER"]` 不再让 `isTeacherUser()` 为 true。
- [x] Production/staging 既有 Bearer/header fail-closed 语义不回退。
- [x] Focused auth/security tests pass，或说明限制。

## Completion Evidence

- RED: `mvn --% -Dtest=SecurityJwtAuthenticationTest test` -> `Tests run: 6, Failures: 1`，失败点证明 dev/test JWT `sub=admin` 会被 legacy `isAdmin()` 提权。
- GREEN focused: `mvn --% -Dtest=SecurityJwtAuthenticationTest test` -> `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- Adjacent auth: `mvn --% -Dtest=CurrentUserServiceTest,DevAuthFilterTest,SecurityJwtAuthenticationTest,SecurityFilterChainTest,SseProductionAuthStrategyTest test` -> `Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`。
