# Review Gate 审核权限加固 Context Pack

## Current TASK

`docs/tasks/TASK-20260606-review-gate-authorization-hardening.md`

## Related Memory and Docs

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260606-review-gate-hardening.md`
- `docs/specs/SPEC-20260606-review-gate-state-model.md`
- `docs/specs/SPEC-20260606-review-gate-authorization-hardening.md`

## Selected Skills

- `feature-development-workflow`
- `spring-boot-architecture`
- `api-contract-design`
- `security-review`
- `test-generator`
- `critic-review-agent`
- `test-driven-development`
- `verification-before-completion`

## Subagent Plan

不启用新的 subagent。Worker C 按用户指定后端切片单线实现。

## Files Allowed To Modify

- `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java`
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `backend/src/test/java/com/learningos/agent/application/ReviewGovernanceServiceTest.java`
- `docs/product/PRD-20260606-review-gate-authorization-hardening.md`
- `docs/requirements/REQ-20260606-review-gate-authorization-hardening.md`
- `docs/specs/SPEC-20260606-review-gate-authorization-hardening.md`
- `docs/plans/PLAN-20260606-review-gate-authorization-hardening.md`
- `docs/tasks/TASK-20260606-review-gate-authorization-hardening.md`
- `docs/context/CONTEXT-20260606-review-gate-authorization-hardening.md`
- `docs/evidence/EVIDENCE-20260606-review-gate-authorization-hardening.md`
- `docs/acceptance/ACCEPT-20260606-review-gate-authorization-hardening.md`

## Files Not Allowed To Modify

- Orchestrator production code.
- RAG Document upload production code.
- IndexService.
- shared memory/changelog/backend todo.
- Frontend files.
- Build configuration.

## Test Commands

```powershell
cd backend
mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest" test
```

## Current Task Boundary

仅实现 Review Gate list/decision 的教师/管理员临时权限检查。真实 RBAC、课程级授权、前端按钮隐藏、审核运营看板不在本切片范围内。
