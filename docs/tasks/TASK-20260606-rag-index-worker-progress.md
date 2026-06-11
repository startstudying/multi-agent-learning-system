# TASK - RAG 索引 Worker 自动执行与进度可观测

## 1. 追踪

- PRD：`docs/product/PRD-20260606-rag-index-worker-progress.md`
- REQ：`docs/requirements/REQ-20260606-rag-index-worker-progress.md`
- SPEC：`docs/specs/SPEC-20260606-rag-index-worker-progress.md`
- PLAN：`docs/plans/PLAN-20260606-rag-index-worker-progress.md`
- 任务编号：TASK-20260606-rag-index-worker-progress

## 2. 目标

关闭 `docs/planning/backend-architecture-todolist.md` P3-2 中的索引任务 worker/progress/heartbeat/retry/detail API 开放项。

## 3. 纳入范围

- 新增 V16 migration 和 schema 文本/真实 MySQL smoke 覆盖。
- `kb_index_task` 补齐 progress、phase、heartbeat、lease、next retry、recoverable。
- 新增 bounded index worker。
- worker 原子 claim due `PENDING` task。
- 处理阶段更新 progress 和 heartbeat。
- 失败后 bounded retry/requeue，超过上限终态失败。
- task detail API 和权限检查。
- 错误信息脱敏。
- Evidence / Acceptance / Retro / Memory / Changelog / TODO 更新。

## 4. 排除范围

- 不做 VectorDB、真实 embedding provider、hybrid retrieval、RRF、reranker fallback。
- 不升级 parser/OCR/复杂 PDF/DOCX。
- 不做 token chunk、overlap、stable chunk hash、heading hierarchy。
- 不改前端。
- 不改 Agent/Orchestrator/RAG query/citation 链路。

## 5. 允许修改的文件

- `backend/src/main/resources/db/migration/V16__rag_index_task_worker_progress.sql`
- `backend/src/main/java/com/learningos/LearningOsApplication.java`
- `backend/src/main/java/com/learningos/config/IndexWorkerProperties.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-test.yml`
- `backend/src/main/java/com/learningos/rag/domain/KbIndexTask.java`
- `backend/src/main/java/com/learningos/rag/repository/KbIndexTaskRepository.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/IndexTaskRecoveryScheduler.java`
- `backend/src/main/java/com/learningos/rag/application/IndexTaskWorkerScheduler.java`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/main/java/com/learningos/rag/api/DocumentController.java`
- `backend/src/main/java/com/learningos/rag/api/dto/DocumentDtos.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexTaskRecoverySchedulerTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexTaskWorkerSchedulerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- `docs/product/PRD-20260606-rag-index-worker-progress.md`
- `docs/requirements/REQ-20260606-rag-index-worker-progress.md`
- `docs/specs/SPEC-20260606-rag-index-worker-progress.md`
- `docs/plans/PLAN-20260606-rag-index-worker-progress.md`
- `docs/tasks/TASK-20260606-rag-index-worker-progress.md`
- `docs/context/CONTEXT-20260606-rag-index-worker-progress.md`
- `docs/subagents/runs/RUN-20260606-rag-index-worker-progress-*.md`
- `docs/evidence/EVIDENCE-20260606-rag-index-worker-progress.md`
- `docs/acceptance/ACCEPT-20260606-rag-index-worker-progress.md`
- `docs/retrospectives/RETRO-20260606-rag-index-worker-progress.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/harness/TEST_COMMANDS.md`

## 6. 禁止修改的文件

- `frontend/**`
- `docs/superpowers/**`
- `backend/pom.xml`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/RerankerService.java`
- `backend/src/main/java/com/learningos/rag/application/AdaptiveRagRouter.java`
- `backend/src/main/java/com/learningos/orchestrator/**`
- 与本切片无关的业务模块

## 7. TDD 测试命令

```powershell
cd backend
mvn --% -Dtest=SchemaConvergenceMigrationTest test
mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,IndexTaskRecoverySchedulerTest,DocumentControllerTest test
mvn test
```

真实 MySQL smoke：

```powershell
$env:MYSQL_PORT='3307'
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
```

## 8. 完成标准

- [x] V16 migration 文本测试通过。
- [x] worker 自动消费 due `PENDING` task。
- [x] worker claim 并发安全。
- [x] progress/heartbeat/lease 在处理阶段更新。
- [x] progress/heartbeat/lease 阶段更新独立提交，长耗时处理中 task detail 可见。
- [x] bounded retry/requeue 和 retry exhaustion 测试通过。
- [x] worker batch 单任务异常不阻断后续任务。
- [x] lease recovery 使用 `leaseUntil < now`，不叠加 `runningTimeout`。
- [x] task detail API 权限和脱敏测试通过。
- [x] 后端全量测试通过。
- [x] 真实 MySQL V1-V16 smoke 通过。
- [x] Evidence / Acceptance / Retro 完成。
- [x] TODO / Memory / Changelog 更新。

## 9. 状态

| 字段 | 值 |
|---|---|
| 状态 | 已完成 |
| 负责人 | Main Codex |
| 开始日期 | 2026-06-06 |
| 完成日期 | 2026-06-06 |
| 证据 | `docs/evidence/EVIDENCE-20260606-rag-index-worker-progress.md` |
| 验收 | `docs/acceptance/ACCEPT-20260606-rag-index-worker-progress.md` |
