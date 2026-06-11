# TASK - RAG Chunk 生产化元数据补齐

## 1. 追踪

- PRD: `docs/product/PRD-20260606-rag-chunk-production-metadata.md`
- REQ: `docs/requirements/REQ-20260606-rag-chunk-production-metadata.md`
- SPEC: `docs/specs/SPEC-20260606-rag-chunk-production-metadata.md`
- PLAN: `docs/plans/PLAN-20260606-rag-chunk-production-metadata.md`
- 任务编号: `TASK-20260606-rag-chunk-production-metadata`

## 2. 目标

补齐 P3-2 中的生产级 chunk token 切分、overlap、稳定 chunk hash 和 heading hierarchy。

## 3. 范围内工作

- 新增 V17 migration。
- 扩展 `KbDocChunk` 模型。
- 重构 `IndexService` 的 chunk 生产逻辑。
- 让 worker 路径复用同一逻辑。
- 更新 schema 和 MySQL smoke 测试。
- 更新相关 memory、changelog、evidence、acceptance、retro。

## 4. 允许修改的文件

- `backend/src/main/resources/db/migration/V17__rag_chunk_production_metadata.sql`
- `backend/src/main/java/com/learningos/rag/domain/KbDocChunk.java`
- `backend/src/main/java/com/learningos/rag/repository/KbDocChunkRepository.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
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

## 5. 禁止修改的文件

- `frontend/**`
- `docs/superpowers/**`
- 与本切片无关的 Agent / Orchestrator / RAG query / citation 模块
- 新依赖相关构建文件，除非后续单独批准

## 6. 验证命令

```powershell
cd backend
mvn --% -Dtest=IndexServiceTest test
mvn --% -Dtest=IndexTaskWorkerSchedulerTest test
mvn --% -Dtest=SchemaConvergenceMigrationTest test
mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test
mvn test
```

## 7. 完成标准

- 测试先 RED 再 GREEN。
- `chunkHash`、overlap、heading hierarchy、worker 一致性、V17 migration 和 MySQL smoke 全部有证据。
- Evidence / Acceptance / Retro / Changelog / Memory / TODO 更新完成。

## 8. 完成记录

- 状态：已完成。
- Evidence：`docs/evidence/EVIDENCE-20260606-rag-chunk-production-metadata.md`
- Acceptance：`docs/acceptance/ACCEPT-20260606-rag-chunk-production-metadata.md`
- Retrospective：`docs/retrospectives/RETRO-20260606-rag-chunk-production-metadata.md`
- 测试：`mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test` 通过；`mvn test` 通过；真实 MySQL smoke 通过 V1-V17。
