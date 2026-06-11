# 资源生成任务恢复状态 Context Pack

## 允许修改文件

- `backend/src/main/java/com/learningos/agent/domain/ResourceGenerationTask.java`
- `backend/src/main/java/com/learningos/agent/dto/ResourceGenerationResponse.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/resources/db/migration/V11__resource_generation_task_recovery_state.sql`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `docs/product/PRD-20260606-resource-generation-recovery-state.md`
- `docs/requirements/REQ-20260606-resource-generation-recovery-state.md`
- `docs/specs/SPEC-20260606-resource-generation-recovery-state.md`
- `docs/plans/PLAN-20260606-resource-generation-recovery-state.md`
- `docs/tasks/TASK-20260606-resource-generation-recovery-state.md`
- `docs/context/CONTEXT-20260606-resource-generation-recovery-state.md`
- `docs/subagents/runs/RUN-20260606-resource-generation-recovery-state.md`
- `docs/evidence/EVIDENCE-20260606-resource-generation-recovery-state.md`
- `docs/acceptance/ACCEPT-20260606-resource-generation-recovery-state.md`
- `docs/retrospectives/RETRO-20260606-resource-generation-recovery-state.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/api/contract.md`
- `docs/api/reference.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/changelog/CHANGELOG.md`

## 禁止事项

- 不新增依赖。
- 不实现后台自动重试 scheduler。
- 不让 Agent 或 Tool 直接访问 Repository。
- 不在 `lastError` 保存 provider 原始错误全文。

## 验证命令

```bash
cd backend
mvn "-Dtest=ResourceGenerationControllerTest#persistsRetryMetadataWhenResourceGenerationFailsRecoverably,SchemaConvergenceMigrationTest#v11MigrationAddsRecoveryStateColumnsToResourceGenerationTask" test
mvn "-Dtest=ResourceGenerationControllerTest,SchemaConvergenceMigrationTest" test
```
