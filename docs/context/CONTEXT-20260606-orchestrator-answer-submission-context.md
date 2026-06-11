# Orchestrator ANSWER_SUBMISSION 上下文收敛 Context Pack

## Current TASK

`docs/tasks/TASK-20260606-orchestrator-answer-submission-context.md`

## Related Memory and Docs

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/subagents/runs/RUN-20260606-orchestrator-answer-submission-context-review.md`
- `docs/subagents/runs/RUN-20260606-orchestrator-answer-submission-test-review.md`
- `docs/subagents/runs/RUN-20260606-orchestrator-answer-submission-security-review.md`

## Selected Skills

- `feature-development-workflow`
- `ai-learning-agent-development`
- `ai-learning-architecture`
- `spring-ai-agent-backend`
- `assessment-feedback-agent`
- `agent-trace-governance`
- `test-driven-development`
- `verification-before-completion`

## Subagent Plan

| Subagent | Mode | Scope |
|---|---|---|
| Architect | Done | 评审 answer submission workflow context 设计。 |
| Test Engineer | Done | 测试覆盖建议。 |
| Security & Quality | Done | 权限、幂等、trace、事务风险评审。 |

代码实现由主 Codex 单线程完成。

## Files Allowed To Modify

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/agent/repository/AgentTaskRepository.java`
- `backend/src/main/java/com/learningos/agent/repository/AgentTraceRepository.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/application/AssessmentServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/changelog/CHANGELOG.md`
- `docs/evidence/EVIDENCE-20260606-orchestrator-answer-submission-context.md`
- `docs/acceptance/ACCEPT-20260606-orchestrator-answer-submission-context.md`
- `docs/retrospectives/RETRO-20260606-orchestrator-answer-submission-context.md`
- `docs/tasks/TASK-20260606-orchestrator-answer-submission-context.md`
- `docs/plans/PLAN-20260606-orchestrator-answer-submission-context.md`
- `docs/subagents/runs/RUN-20260606-orchestrator-answer-submission-test-review.md`
- `docs/subagents/runs/RUN-20260606-orchestrator-answer-submission-security-review.md`

## Files Not Allowed To Modify

- Frontend files.
- Database migrations.
- Build configuration.
- RAG/resource generation implementation files.
- Controller API contract files unless tests prove a contract mismatch.

## Test Commands

```powershell
cd backend; mvn "-Dtest=OrchestratorWorkflowControllerTest,AssessmentControllerTest,AssessmentServiceTest" test
cd backend; mvn test
```

## Task Boundary

本轮不新增 DB 表、不做通用 retry/recovery、不改直接 assessment API。只把 `ANSWER_SUBMISSION` 接入 Orchestrator 上下文，并保持 replay 和 trace 审计一致。

全量验证期间允许修复与本切片无关但阻塞 `mvn test` 的测试 fixture；本次仅涉及 RAG timeout recovery 用例的固定时间戳漂移，不改变生产 RAG 逻辑。
