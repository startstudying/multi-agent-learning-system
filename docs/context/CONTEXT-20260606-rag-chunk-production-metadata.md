# CONTEXT - RAG Chunk 生产化元数据补齐

## 1. 相关记忆与文档

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260606-rag-index-worker-progress.md`
- `docs/subagents/runs/RUN-20260606-rag-chunk-production-metadata-*.md`

## 2. 选用技能

- `feature-development-workflow`
- `educational-rag-pipeline`
- `test-driven-development`
- `confidence-check`
- `verification-before-completion`

## 3. 子代理计划

已完成只读分析与集成评审，后续不再并行改同一组文件。

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/domain/KbDocChunk.java`
- `backend/src/main/java/com/learningos/rag/repository/KbDocChunkRepository.java`
- `backend/src/main/resources/db/migration/V17__rag_chunk_production_metadata.sql`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexTaskWorkerSchedulerTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- `docs/product/PRD-20260606-rag-chunk-production-metadata.md`
- `docs/requirements/REQ-20260606-rag-chunk-production-metadata.md`
- `docs/specs/SPEC-20260606-rag-chunk-production-metadata.md`
- `docs/plans/PLAN-20260606-rag-chunk-production-metadata.md`
- `docs/tasks/TASK-20260606-rag-chunk-production-metadata.md`
- `docs/context/CONTEXT-20260606-rag-chunk-production-metadata.md`
- `docs/evidence/EVIDENCE-20260606-rag-chunk-production-metadata.md`
- `docs/acceptance/ACCEPT-20260606-rag-chunk-production-metadata.md`
- `docs/retrospectives/RETRO-20260606-rag-chunk-production-metadata.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. 不允许修改的文件

- `frontend/**`
- `docs/superpowers/**`
- `backend/pom.xml`
- 与 RAG chunk 生产元数据无关的业务模块

## 6. 测试命令

```powershell
cd backend
mvn --% -Dtest=IndexServiceTest test
mvn --% -Dtest=IndexTaskWorkerSchedulerTest test
mvn --% -Dtest=SchemaConvergenceMigrationTest test
mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test
mvn test
```

## 7. 边界

本切片只处理 chunk token 切分、overlap、稳定 hash 与 heading hierarchy 的生产化元数据，不扩展索引解析器、VectorDB 或公开 RAG API。
