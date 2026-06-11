# CONTEXT - RAG Parser Adapter 最小生产切片

## 1. 相关记忆与文档

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260606-rag-chunk-production-metadata.md`
- `docs/subagents/runs/RUN-20260607-backend-plan-rag-production-architect.md`
- `docs/subagents/runs/RUN-20260607-backend-plan-rag-production-security.md`
- `docs/subagents/runs/RUN-20260607-backend-plan-rag-production-test-strategy.md`
- `docs/subagents/runs/RUN-20260607-backend-plan-rag-production-integration-review.md`

## 2. 选用技能

- `feature-development-workflow`
- `educational-rag-pipeline`
- `test-driven-development`
- `confidence-check`
- `verification-before-completion`
- `security-review`

## 3. 子代理计划

已完成并行分析与评审。后续代码实现由 Main Codex 单线程推进，避免多个执行者修改同一 `IndexService` / parser 文件。

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/parser/**`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexTaskWorkerSchedulerTest.java`
- `backend/src/test/java/com/learningos/rag/parser/**`
- `docs/product/PRD-20260607-rag-parser-adapter-minimal.md`
- `docs/requirements/REQ-20260607-rag-parser-adapter-minimal.md`
- `docs/specs/SPEC-20260607-rag-parser-adapter-minimal.md`
- `docs/plans/PLAN-20260607-rag-parser-adapter-minimal.md`
- `docs/tasks/TASK-20260607-rag-parser-adapter-minimal.md`
- `docs/context/CONTEXT-20260607-rag-parser-adapter-minimal.md`
- `docs/evidence/EVIDENCE-20260607-rag-parser-adapter-minimal.md`
- `docs/acceptance/ACCEPT-20260607-rag-parser-adapter-minimal.md`
- `docs/retrospectives/RETRO-20260607-rag-parser-adapter-minimal.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/rag-parser-boundary.md`

## 5. 不允许修改的文件

- `frontend/**`
- `docs/superpowers/**`
- 与 parser 边界无关的 Agent / Orchestrator / VectorDB / model 接入模块

## 6. 测试命令

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest test
mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test
mvn test
```

## 7. 边界

本切片只处理 RAG 文档解析边界抽取，不做真实 OCR、不加新依赖、不扩展 VectorDB / embedding / hybrid retrieval。
