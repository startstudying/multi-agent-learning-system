# Agent Trace 治理看板后端最小切片 Context Pack

## Current TASK

实现 TODO P2-5「Agent Trace 治理看板」的最小可验收后端切片：

- trace 查询过滤：用户、agent 类型、状态、时间、失败原因。
- 服务层写入/接口展示 `agent_tool_call`。
- trace retention policy 后端最小响应。
- 敏感字段清理。

## Related Memory and Docs

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/agent-trace-design.md`

## Selected Skills

- `spring-boot-architecture`
- `api-contract-design`
- `database-design`
- `agent-trace-design`
- `security-review`
- `test-generator`
- `architecture-drift-check`

## Subagent Plan

当前实例是并行后端 worker。实现方式为单 worker、单任务顺序执行，不启动额外并行实现。其他 worker 可能同时修改 `rag` / `assessment`，本 worker 不触碰这些模块。

## Files Allowed To Modify

- `backend/src/main/java/com/learningos/agent/**` 中 AgentTrace / AgentTask / tool-call / governance 相关文件
- `backend/src/test/java/com/learningos/agent/**` 中 AgentTrace / AgentRunRecorder 相关测试
- `backend/src/main/resources/db/migration/V15__agent_tool_call_trace_governance.sql`
- `docs/product/PRD-20260606-agent-trace-governance-dashboard.md`
- `docs/requirements/REQ-20260606-agent-trace-governance-dashboard.md`
- `docs/specs/SPEC-20260606-agent-trace-governance-dashboard.md`
- `docs/plans/PLAN-20260606-agent-trace-governance-dashboard.md`
- `docs/tasks/TASK-20260606-agent-trace-governance-dashboard.md`
- `docs/context/CONTEXT-20260606-agent-trace-governance-dashboard.md`
- `docs/evidence/EVIDENCE-20260606-agent-trace-governance-dashboard.md`
- `docs/acceptance/ACCEPT-20260606-agent-trace-governance-dashboard.md`
- `docs/subagents/runs/RUN-20260606-agent-trace-governance-dashboard-worker.md`

## Files Not Allowed To Modify

- `docs/changelog/CHANGELOG.md`
- `docs/memory/*`
- `docs/planning/backend-architecture-todolist.md`
- `backend/src/main/java/com/learningos/evaluation/**`
- `backend/src/test/java/com/learningos/evaluation/**`
- Existing migrations V1-V14
- `rag` / `assessment` module files

## Test Commands

```powershell
cd backend
mvn "-Dtest=AgentTraceControllerTest,AgentRunRecorderTest" test
```

## Task Boundary

本轮只做后端最小可验收切片，不实现前端看板、不做物理清理任务、不做全量 SchemaConvergenceMigrationTest 集成。
