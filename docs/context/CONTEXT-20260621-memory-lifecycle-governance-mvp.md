# CONTEXT-20260621 Memory lifecycle governance MVP

## 1. 相关记忆与文档

- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/specs/SPEC-20260621-memory-rag-privacy-guard.md`
- `docs/specs/SPEC-20260621-memory-context-service-mvp.md`
- `docs/specs/SPEC-20260621-qa-runtime-structured-answer-mvp.md`
- `docs/specs/SPEC-20260621-qa-verifier-eval-gate-mvp.md`
- `docs/specs/SPEC-20260621-qa-streaming-trace-workbench-mvp.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 2. Selected Skills

- feature-development-workflow
- spring-ai-agent-backend
- agent-trace-governance
- test-driven-development
- verification-before-completion

## 3. Subagent Plan

No subagents. 单 Codex 顺序执行。

## 4. Files Allowed To Modify

- `backend/src/main/resources/db/migration/V23__memory_lifecycle_governance.sql`
- `backend/src/main/java/com/learningos/rag/domain/KbChatSession.java`
- `backend/src/main/java/com/learningos/rag/domain/KbChatMessage.java`
- `backend/src/main/java/com/learningos/rag/repository/KbChatSessionRepository.java`
- `backend/src/main/java/com/learningos/rag/repository/KbChatMessageRepository.java`
- `backend/src/main/java/com/learningos/aiqa/application/AiQaService.java`
- `backend/src/main/java/com/learningos/aiqa/application/memory/MemoryLifecycleService.java`
- `backend/src/main/java/com/learningos/aiqa/application/memory/MemoryContextService.java`
- `backend/src/main/java/com/learningos/aiqa/api/AiQaMemoryController.java`
- `backend/src/main/java/com/learningos/aiqa/api/dto/AiQaDtos.java`
- `backend/src/test/java/com/learningos/aiqa/application/memory/MemoryLifecycleServiceTest.java`
- `backend/src/test/java/com/learningos/aiqa/application/memory/MemoryContextServiceTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- P2 docs / evidence / acceptance
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`

## 5. Files Not Allowed To Modify

- `frontend/**`
- `backend/pom.xml`
- provider credentials / env files
- RAG parser/vector/index worker
- ResourceGeneration / ReviewGate production code

## 6. Test Commands

```powershell
cd backend
mvn --% -Dtest=MemoryLifecycleServiceTest,MemoryContextServiceTest,SchemaConvergenceMigrationTest test
mvn --% -Dtest=AiQaControllerTest,QaRuntimeTest test
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```

## 7. Current Task Boundary

只实现 P2 MVP：session memory schema、低敏 QA memory summary、salience/decay、owner-only edit/delete、MemoryContext active session summary。不得实现前端 UI、向量长期记忆或复杂冲突合并。
