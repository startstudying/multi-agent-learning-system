# CONTEXT-20260608 模型网关结构化校验与日志补齐

## Status

Done。

## Current Task Boundary

只实现模型网关边界硬化：

- `AiModelGateway` 对资源生成 structured output 做轻量 schema 校验。
- 成功模型调用日志改用 gateway response 的 model / latency / token usage。
- provider / schema 失败只暴露安全错误码。

不实现真实 provider，不新增依赖，不改 DB schema，不改 frontend。

## Related Memory and Docs

- `AGENTS.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/specs/SPEC-20260608-model-gateway-boundary.md`

## Selected Skills

- feature-development-workflow
- ai-learning-agent-development
- spring-ai-agent-backend
- agent-trace-governance
- multi-agent-coder
- test-driven-development
- systematic-debugging
- verification-before-completion
- Confidence Check

## Subagent Plan

- Model Boundary Expert：已完成只读分析并落盘。
- Security & Quality Expert：已完成只读分析并落盘。
- Test/Integration Expert：已完成只读分析并落盘。
- Integration Reviewer：主 Codex 汇总报告并串行实现；不并行修改同一文件。

## Files Allowed To Modify

- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRuntimeConstants.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java`
- `backend/src/test/java/com/learningos/agent/application/AgentRunRecorderTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- 本任务 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / Evidence / Acceptance / Memory / Changelog / Retro / Skill 文件。

## Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- RAG parser/indexer/vector/retrieval implementation files
- `docs/superpowers/**`

## Test Commands

```powershell
cd backend
mvn --% -Dtest=AiModelGatewayTest test
mvn --% -Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest test
mvn --% -Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,AgentTraceControllerTest,AnalyticsControllerTest test
mvn test
```

## Confidence Check

- No duplicate implementations：已检查，当前只有 `AiModelGateway` 作为结构化模型生成入口。
- Architecture compliance：符合 backend owns AI calls、Service -> Gateway、Agent Trace governance。
- Official docs：本切片不新增 provider / dependency / SDK API，官方文档不适用。
- OSS reference：本切片为项目内轻量 validator，不引入外部实现。
- Root cause：结构化输出未校验、成功日志事实源来自 trace 占位、gateway 异常 message 含 raw provider error。

Confidence：0.95，可进入 TDD 实现。

## Completion Evidence

- `docs/evidence/EVIDENCE-20260608-model-gateway-boundary.md`
- `docs/acceptance/ACCEPT-20260608-model-gateway-boundary.md`
- `docs/retrospectives/RETRO-20260608-model-gateway-boundary.md`
- `docs/skills/project-specific/model-gateway-boundary.md`

最终验证：

- `mvn --% -Dtest=AiModelGatewayTest test`：8 tests，0 failures，0 errors。
- `mvn --% -Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest test`：24 tests，0 failures，0 errors。
- `mvn --% -Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,AgentTraceControllerTest,AnalyticsControllerTest test`：75 tests，0 failures，0 errors。
- `mvn test`：275 tests，0 failures，0 errors，1 skipped。
