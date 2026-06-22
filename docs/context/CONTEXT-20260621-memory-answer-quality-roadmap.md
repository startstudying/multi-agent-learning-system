# CONTEXT-20260621 记忆系统与回答质量路线图 Context Pack

## 1. Related Memory And Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DECISION_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/SUBAGENT_REGISTRY.md`

## 2. User Inputs

- Agent 记忆系统框架：Memory Store、Memory Retrieval、Context Orchestration、Memory Write Pipeline、Policy/Governance。
- Answer Quality System 框架：Intent Router、Context Builder、Memory Retriever、Task Planner、Tool/Knowledge Executor、Draft Generator、Verifier/Critic、Final Composer、Feedback/Eval Log。

## 3. Local Evidence Files

- `backend/src/main/java/com/learningos/aiqa/application/AiQaService.java`
- `backend/src/main/java/com/learningos/aiqa/application/QaModePolicy.java`
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/rag/domain/KbChatSession.java`
- `backend/src/main/java/com/learningos/rag/domain/KbChatMessage.java`
- `backend/src/main/resources/db/migration/V1__rag_foundation.sql`

## 4. Selected Skills

- `feature-development-workflow`
- `analyze`
- `openai-docs`
- `ai-learning-architecture`
- `educational-rag-pipeline`
- `learner-profile-agent`
- `agent-trace-governance`
- `spring-ai-agent-backend`

## 5. Subagent Plan

已使用 L1 并行分析：

- Agent/RAG expert：分析根因与本地架构缺口，结论由主线程整合。
- External Research expert：`docs/subagents/runs/RUN-20260621-memory-chatgpt-quality-external-research.md`
- Security & Quality expert：`docs/subagents/runs/RUN-20260621-memory-chatgpt-quality-security-quality.md`

## 6. Current Task Boundary

本任务只生成路线图文档，不实现代码。

后续实现必须拆分为独立 slice：

1. Memory/RAG Privacy Guard。
2. MemoryContextService MVP。
3. QaRuntime / Answer Quality MVP。
4. Basic Verifier / Eval Gate。

## 7. Allowed Files

仅允许修改本任务 workflow 文档、evidence/acceptance、changelog、memory。

## 8. Disallowed Files

禁止修改后端、前端、数据库迁移、依赖、环境配置。

## 9. Test Commands

本轮 docs-only，不执行测试。

后续代码 slice 可按风险选择：

```powershell
cd backend; mvn --% -Dtest=AiQaControllerTest,QaModePolicyTest,RagQueryServiceTest test
cd backend; mvn test
cd frontend; pnpm test -- --run
cd frontend; pnpm build
```

## 10. Assumptions

- 当前 OpenAI 官方 API/SDK 能力变化较快，具体实现前需要再确认官方文档。
- 项目仍以 Java 21 + Spring Boot + Spring AI 为主运行时。
- 新依赖、新数据库或外部 SDK 均需单独评审。
