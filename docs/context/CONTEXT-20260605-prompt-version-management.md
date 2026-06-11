# CONTEXT: Prompt Version 管理基础

## 当前 TASK

`TASK-20260605-prompt-version-management.md`

## 相关记忆和文档

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `backend/src/main/resources/db/migration/V2__learning_agent_loop.sql`

## Selected Skills

- feature-development-workflow
- ai-learning-agent-development
- spring-ai-agent-backend
- agent-trace-governance
- test-driven-development
- Confidence Check
- verification-before-completion

## Subagent Plan

不启用 subagents。用户指定当前身份是后端 worker，且文件边界集中在单一后端模块。

## Files Allowed To Modify

- `backend/src/main/java/com/learningos/agent/domain/PromptVersion.java`
- `backend/src/main/java/com/learningos/agent/repository/PromptVersionRepository.java`
- `backend/src/main/java/com/learningos/agent/application/PromptVersionService.java`
- `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java`
- `backend/src/main/java/com/learningos/agent/dto/PromptVersionUpsertRequest.java`
- `backend/src/main/java/com/learningos/agent/dto/PromptVersionResponse.java`
- `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java`
- `backend/src/test/java/com/learningos/agent/application/PromptVersionServiceTest.java`
- `docs/product/PRD-20260605-prompt-version-management.md`
- `docs/requirements/REQ-20260605-prompt-version-management.md`
- `docs/specs/SPEC-20260605-prompt-version-management.md`
- `docs/plans/PLAN-20260605-prompt-version-management.md`
- `docs/tasks/TASK-20260605-prompt-version-management.md`
- `docs/context/CONTEXT-20260605-prompt-version-management.md`
- `docs/evidence/EVIDENCE-20260605-prompt-version-management.md`
- `docs/acceptance/ACCEPT-20260605-prompt-version-management.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`

## Files Not Allowed To Modify

- `backend/src/main/resources/db/migration/*`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/main/java/com/learningos/agent/domain/ModelCallLog.java`
- `frontend/**`
- `docs/superpowers/**`

## Test Commands

```powershell
cd backend
mvn "-Dtest=PromptVersionControllerTest,PromptVersionServiceTest" test
```

## Current Task Boundary

只补齐 Prompt Version 管理基础。evaluation set、Prompt 与 `model_call_log` 的真实外键/字段绑定、实验指标对比均留作后续任务。
