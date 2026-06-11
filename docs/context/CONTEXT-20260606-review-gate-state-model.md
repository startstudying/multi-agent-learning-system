# Review Gate 状态模型加固 Context Pack

## Current TASK

`docs/tasks/TASK-20260606-review-gate-state-model.md`

## Related Memory and Docs

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260606-review-gate-hardening.md`

## Selected Skills

- `feature-development-workflow`
- `ai-learning-agent-development`
- `critic-review-agent`
- `spring-ai-agent-backend`
- `test-driven-development`
- `verification-before-completion`

## Subagent Plan

现有 6 个子代理已并行派发只读审查任务；本切片由主 Codex 单线程实现。

## Files Allowed To Modify

- `backend/src/main/java/com/learningos/agent/application/AgentRuntimeConstants.java`
- `backend/src/main/java/com/learningos/agent/domain/ResourceReview.java`
- `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java`
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`
- `backend/src/main/resources/db/migration/V6__resource_review_governance.sql`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `backend/src/test/java/com/learningos/agent/application/ReviewGovernanceServiceTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `docs/product/PRD-20260606-review-gate-state-model.md`
- `docs/requirements/REQ-20260606-review-gate-state-model.md`
- `docs/specs/SPEC-20260606-review-gate-state-model.md`
- `docs/plans/PLAN-20260606-review-gate-state-model.md`
- `docs/tasks/TASK-20260606-review-gate-state-model.md`
- `docs/context/CONTEXT-20260606-review-gate-state-model.md`
- 后续 evidence / acceptance / retro / changelog / memory / planning TODO

## Files Not Allowed To Modify

- Frontend files.
- RAG production files.
- Orchestrator production files.
- Build configuration.

## Test Commands

```powershell
cd backend
mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,SchemaConvergenceMigrationTest" test
mvn "-Dtest=ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest" test
mvn test
```

## Task Boundary

本轮只加固 Review Gate 状态和结构化审核日志，不实现真实 Critic Agent、不改前端、不扩教师权限模型。
