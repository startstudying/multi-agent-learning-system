# Agent 状态机收敛 Context Pack

## 允许修改

- `backend/src/main/java/com/learningos/agent/application/AgentRuntimeConstants.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/agent/api/AgentTraceController.java`
- `backend/src/main/java/com/learningos/agent/dto/*`
- `backend/src/test/java/com/learningos/agent/application/AgentRunRecorderTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- 本轮 `docs/product`、`docs/requirements`、`docs/specs`、`docs/plans`、`docs/tasks`、`docs/context`、`docs/evidence`、`docs/acceptance`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/changelog/CHANGELOG.md`
- `docs/subagents/runs/RUN-20260606-agent-state-machine.md`

## 不允许修改

- 前端代码。
- RAG、assessment、analytics 主业务代码。
- 依赖配置。
- 数据库迁移。

## 选定技能

- `ai-learning-agent-development`
- `ai-learning-architecture`
- `agent-trace-governance`
- `spring-ai-agent-backend`
- `test-driven-development`
- `verification-before-completion`

## 参考项目落地方式

- LMS 参考只影响业务底座理解：课程、任务、进度、反馈都需要稳定状态。
- Spring AI / Spring AI Alibaba 参考只影响运行时治理：Agent 编排、观测、评估和工具调用必须通过后端服务层记录状态。
- 本轮不复制外部代码，不新增依赖。

## 测试命令

```bash
cd backend
mvn "-Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest" test
mvn test
```
