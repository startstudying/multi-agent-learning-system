# TASK-20260610 P3-3 子任务：model provider TODO 状态对账

Status: Done.

## Goal

对账 `docs/planning/backend-architecture-todolist.md` 中 P3-3 两个未勾选项，确认它们是否已由 P3-3-B / P3-3-C 现有实现、证据和验收闭环完成，并最小更新计划状态。

## Task Type

文档 / 计划状态对账，不新增功能代码。

## Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 继续执行后端架构 TODO 总计划 |
| spring-ai-agent-backend | P3-3 涉及 Spring AI Chat/Embedding provider adapter |
| agent-trace-governance | P3-3-B 涉及 `model_call_log.provider`、model evidence 和 token/cost 日志 |
| verification-before-completion | 勾选 TODO 前必须有当前证据 |
| subagent-driven-development | 用户要求专家 subagent 并行审查 |

## Size Decision

Size: S.

Reason: 只回填计划状态和记录证据；不改后端代码、API、DB schema、依赖或前端。

## Context Pack

### Related Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260608-real-model-provider-adapter.md`
- `docs/evidence/EVIDENCE-20260608-real-model-provider-adapter.md`
- `docs/acceptance/ACCEPT-20260608-real-model-provider-adapter.md`
- `docs/security/DEPENDENCY-REVIEW-20260608-real-model-provider-adapter.md`
- `docs/specs/SPEC-20260608-model-call-provider-observability.md`
- `docs/evidence/EVIDENCE-20260608-model-call-provider-observability.md`
- `docs/acceptance/ACCEPT-20260608-model-call-provider-observability.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`

### Subagent Plan

| Expert | Scope |
|---|---|
| Documentation Expert | 判断 P3-3 TODO 是否可按现有证据勾选 |
| Security / Dependency Expert | 判断 Spring AI 依赖、secret、SDK 边界是否满足计划状态回填 |
| Test Evidence Expert | 判断已有测试证据是否覆盖 P3-3-B / P3-3-C |

### Files Allowed To Modify

- `docs/planning/backend-architecture-todolist.md`
- `docs/tasks/TASK-20260610-p3-3-model-provider-todo-status-reconciliation.md`
- `docs/evidence/EVIDENCE-20260610-p3-3-model-provider-todo-status-reconciliation.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`

### Files Not Allowed To Modify

- `backend/src/main/**`
- `backend/src/test/**`
- `backend/pom.xml`
- `frontend/**`
- `backend/src/main/resources/db/migration/**`
- secrets / `.env`

## Checklist

- [x] Read P3-3 TODO current state.
- [x] Verify P3-3-C real model provider adapter evidence exists.
- [x] Verify P3-3-B provider observability evidence exists.
- [x] Verify dependency review exists for Spring AI OpenAI-compatible starter.
- [x] Update TODO without overstating DashScope / VectorDB / external smoke completion.
- [x] Create evidence.
- [x] Update changelog and memory.

## Test / Verification Commands

```powershell
cd D:\多元agent
rg -n "P3-3|Real Model Provider|model_call_log.provider|Spring AI|EmbeddingModel|ChatModel" docs\memory\PROJECT_MEMORY.md docs\memory\BACKEND_MEMORY.md docs\planning\backend-architecture-todolist.md
```

Optional code verification from accepted P3-3-B/C evidence:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest,AgentRunRecorderTest,SchemaConvergenceMigrationTest test
```

## Acceptance Criteria

- P3-3 real Spring AI OpenAI-compatible Chat/Embedding adapter TODO is marked completed with explicit note that DashScope and external provider smoke remain follow-up enhancements.
- P3-3 provider/model/promptVersion/latency/token/error logging TODO is marked completed with explicit note that provider DB persistence was completed by P3-3-B.
- No code, schema, dependency, API, or frontend files are modified by this reconciliation task.
