# 模型调用 Prompt 元数据 Context Pack

## 允许修改文件

- `backend/src/main/java/com/learningos/agent/domain/ModelCallLog.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/main/resources/db/migration/V12__model_call_prompt_metadata.sql`
- `backend/src/test/java/com/learningos/agent/application/AgentRunRecorderTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `docs/product/PRD-20260606-model-call-prompt-metadata.md`
- `docs/requirements/REQ-20260606-model-call-prompt-metadata.md`
- `docs/specs/SPEC-20260606-model-call-prompt-metadata.md`
- `docs/plans/PLAN-20260606-model-call-prompt-metadata.md`
- `docs/tasks/TASK-20260606-model-call-prompt-metadata.md`
- `docs/context/CONTEXT-20260606-model-call-prompt-metadata.md`
- `docs/subagents/runs/RUN-20260606-model-call-prompt-metadata.md`
- `docs/evidence/EVIDENCE-20260606-model-call-prompt-metadata.md`
- `docs/acceptance/ACCEPT-20260606-model-call-prompt-metadata.md`
- `docs/retrospectives/RETRO-20260606-model-call-prompt-metadata.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/changelog/CHANGELOG.md`

## 禁止事项

- 不保存 raw prompt。
- 不保存 raw model output。
- 不新增外部依赖。
- 不改变 PromptVersion API。
- 不让 Controller 直接写模型日志。

## 验证命令

```powershell
cd backend
mvn "-Dtest=AgentRunRecorderTest,SchemaConvergenceMigrationTest" test
mvn "-Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest,SchemaConvergenceMigrationTest,PromptVersionServiceTest,PromptVersionControllerTest,AiModelGatewayTest" test
```
