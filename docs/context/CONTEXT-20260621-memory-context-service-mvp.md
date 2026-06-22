# CONTEXT-20260621 MemoryContextService MVP

## 1. 相关记忆与文档

- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/specs/SPEC-20260621-memory-rag-privacy-guard.md`
- `docs/tasks/TASK-20260621-memory-rag-privacy-guard.md`
- `docs/evidence/EVIDENCE-20260621-memory-rag-privacy-guard.md`
- `docs/acceptance/ACCEPT-20260621-memory-rag-privacy-guard.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/harness/TEST_COMMANDS.md`

## 2. 已选技能

- `feature-development-workflow`
- `executing-plans`
- `test-driven-development`
- `spring-ai-agent-backend`
- `educational-rag-pipeline`
- `learner-profile-agent`
- `agent-trace-governance`
- `security-review`

## 3. Subagent 计划

本切片不启用并行实现。原因：

- 当前任务范围为单个后端服务层 MVP。
- 当前工作区已有大量既有未提交改动，单线程实现更利于控制文件边界。
- Agent/RAG 安全要求通过本 Context Pack、focused tests 和 acceptance 约束落地。

## 4. 允许修改文件

- `backend/src/main/java/com/learningos/aiqa/application/AiQaService.java`
- `backend/src/main/java/com/learningos/aiqa/application/memory/*.java`
- `backend/src/main/java/com/learningos/learning/repository/LearningEventRepository.java`
- `backend/src/main/java/com/learningos/learning/repository/MasteryRecordRepository.java`
- `backend/src/test/java/com/learningos/aiqa/application/memory/*.java`
- `backend/src/test/java/com/learningos/aiqa/api/AiQaControllerTest.java`
- `docs/specs/SPEC-20260621-memory-context-service-mvp.md`
- `docs/tasks/TASK-20260621-memory-context-service-mvp.md`
- `docs/context/CONTEXT-20260621-memory-context-service-mvp.md`
- `docs/evidence/EVIDENCE-20260621-memory-context-service-mvp.md`
- `docs/acceptance/ACCEPT-20260621-memory-context-service-mvp.md`
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 5. 禁止修改文件

- `frontend/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/**`
- AI provider 配置、密钥、环境文件。
- 与 P0-2 无关的 Orchestrator、ResourceGeneration、Evaluation 生产代码。

## 6. 测试命令

Focused:

```powershell
cd backend
mvn --% -Dtest=MemoryContextServiceTest test
```

Adjacent:

```powershell
cd backend
mvn --% -Dtest=AiQaControllerTest,RagQueryServiceTest test
```

## 7. 当前边界

只完成 P0-2 `MemoryContextService MVP`。不实现 P0-3 `QaRuntime`，不实现 P0-4 verifier/eval gate，不新增长期记忆生命周期治理。

## 8. 关键约束

- `MemoryContext` 不包含 raw prompt、provider key、teacher note、完整 excerpt。
- `profileRef` 使用 P0-1 的 `MemoryPrivacyPolicy`。
- 列表大小和文本长度必须有硬上限。
- API 响应沿用现有 `toolCalls` 字段暴露摘要，避免扩大公开契约。
