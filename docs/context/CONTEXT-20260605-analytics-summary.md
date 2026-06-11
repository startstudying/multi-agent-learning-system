# Analytics 学习分析扩展 Context Pack

## 当前任务边界

实现 TODO P1-5/P2-4 中 analytics 模块范围内的扩展，不处理教师端 class summary，不修改其他 domain。

## 已读上下文

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/SUBAGENT_REGISTRY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/harness/TEST_COMMANDS.md`

## Selected Skills

- feature-development-workflow
- test-driven-development
- ai-learning-agent-development
- spring-ai-agent-backend
- agent-trace-governance
- Confidence Check

## Subagent Plan

不启用 subagent。当前 worker 是后端 analytics worker，按单模块单任务实现。

## Files Allowed To Modify

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `docs/product/PRD-20260605-analytics-summary.md`
- `docs/requirements/REQ-20260605-analytics-summary.md`
- `docs/specs/SPEC-20260605-analytics-summary.md`
- `docs/plans/PLAN-20260605-analytics-summary.md`
- `docs/tasks/TASK-20260605-analytics-summary.md`
- `docs/context/CONTEXT-20260605-analytics-summary.md`
- `docs/evidence/EVIDENCE-20260605-analytics-summary.md`
- `docs/acceptance/ACCEPT-20260605-analytics-summary.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## Files Not Allowed To Modify

- `backend/src/main/java/com/learningos/agent/**`
- `backend/src/main/java/com/learningos/assessment/**`
- `backend/src/main/java/com/learningos/learning/**`
- `backend/src/main/java/com/learningos/rag/**`
- frontend files
- database migrations

## Test Commands

```bash
cd backend
mvn "-Dtest=AnalyticsControllerTest" test
```

## Architecture Drift Check Before Implementation

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller delegates to Service |
| Frontend rules | PASS | No frontend changes |
| Agent / RAG rules | PASS | Only reads logs; no Agent/RAG execution changes |
| Security | PASS | No secrets or dependencies |
| API / Database | PASS | SPEC documents new endpoint and fields; no schema change |

## Architecture Drift Check After Implementation

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | `AnalyticsController` handles route/current-user check; `AnalyticsService` performs read-only aggregation |
| Frontend rules | PASS | No frontend changes |
| Agent / RAG rules | PASS | Reads existing governance logs only |
| Security | PASS | Student summary rejects cross-learner access |
| API / Database | PASS | No schema or dependency changes |

## Test Result

```bash
cd backend
mvn "-Dtest=AnalyticsControllerTest" test
```

Result: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`.
