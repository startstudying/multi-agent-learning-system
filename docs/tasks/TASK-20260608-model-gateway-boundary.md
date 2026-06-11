# TASK-20260608 模型网关结构化校验与日志补齐

## Task：P3-3-A 模型网关结构化校验与日志补齐

Status: Done

## Done Criteria

- [x] `AiModelGateway` 对 `agent-resource-v1` structured output 校验 `resources[]` 与资源必填字段。
- [x] 缺 `resources` 或资源 item 缺必填字段时触发 retry；最终失败写入安全失败证据。
- [x] provider raw error / prompt / student answer / RAG chunk / secret 不进入 exception message、recorder 参数、HTTP 响应、task output、trace summary、model_call_log。
- [x] 成功 `model_call_log.model` 与 `latencyMs` 来自 gateway response。
- [x] 成功 token usage/cost 来自 gateway response token usage。
- [x] `ResourceGenerationService` 传入 `agent-resource-v1`，并把成功模型证据绑定到 `step_resource`。
- [x] 不新增依赖、不改 DB schema、不改 frontend、不接真实 provider。
- [x] Evidence / Acceptance / Memory / Changelog / Retrospective / Skill 更新。

## Allowed Files

- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRuntimeConstants.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java`
- `backend/src/test/java/com/learningos/agent/application/AgentRunRecorderTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `docs/subagents/runs/RUN-20260608-backend-architecture-completion-model-boundary.md`
- `docs/subagents/runs/RUN-20260608-backend-architecture-completion-model-security.md`
- `docs/subagents/runs/RUN-20260608-backend-architecture-completion-model-test-integration.md`
- `docs/subagents/runs/RUN-20260608-backend-architecture-completion-model-integration.md`
- `docs/evidence/EVIDENCE-20260608-model-gateway-boundary.md`
- `docs/acceptance/ACCEPT-20260608-model-gateway-boundary.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/model-gateway-boundary.md`
- `docs/retrospectives/RETRO-20260608-model-gateway-boundary.md`

## Test Commands

```powershell
cd backend
mvn --% -Dtest=AiModelGatewayTest test
mvn --% -Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest test
mvn --% -Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,AgentTraceControllerTest,AnalyticsControllerTest test
mvn test
```
