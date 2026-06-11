# RUN - P3-4-V Security Reviewer

## Scope

`ResourceGeneration` task detail / learner resources、`AgentTrace` detail、`AgentTraceGovernance` search、auth boundary。

## Risk Level

HIGH

## High Issue 1 - Bearer `USER sub=admin` is treated as admin for detail reads

Affected paths:

- `GET /api/resources/generation-tasks/{taskId}`
- `GET /api/agent/tasks/{taskId}/trace`

Root cause:

- Controller passes only `currentUserId()`.
- Service infers admin using `private boolean isAdmin(String userId) { return "admin".equals(userId); }`.

Impact:

- A valid Bearer token carrying `roles=["USER"]` and `sub="admin"` can read foreign resource generation task details and agent trace details under the legacy service logic.

Required fix:

- Controller must pass explicit `currentUserAdmin` derived from `UserContext.roles()`.
- Service must make detail authorization decisions from that explicit boolean for HTTP roles-first paths.

## High Issue 2 - Bearer `USER sub=admin` can search trace governance data

Affected path:

- `GET /api/agent/traces`

Root cause:

- `AgentTraceGovernanceService.search(...)` checks `if (!"admin".equals(currentUserId))`.

Impact:

- A non-admin Bearer identity with `sub=admin` can search global trace inventory.

Required fix:

- Add `search(currentUserId, currentUserAdmin, ...)`.
- Reject unless `currentUserAdmin == true`.

## Medium Issue - Real admin subjects are denied

Bearer `ADMIN sub=ops_admin` is denied on detail/search because legacy checks only accept `sub == "admin"`.

Required fix:

- Use explicit `ADMIN` role fact for admin semantics.

## Test Requirements

- Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` succeeds for admin detail/search.
- Bearer `USER sub=admin` is denied for foreign detail/search.
- Bearer admin missing task/trace returns `NOT_FOUND`.
- Non-admin missing/foreign paths remain safe `FORBIDDEN`.

## Status

Read-only report. No files were modified by the subagent.
