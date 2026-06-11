# Context Pack - RAG 索引 Worker 自动执行与进度可观测

## 当前任务

完成 P3-2 的索引任务生产化切片：worker 自动执行、progress、heartbeat、bounded retry/requeue 和 task detail API。

## 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/memory/API_MEMORY.md`

## 关联文档

- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260606-rag-index-active-dedup.md`
- `docs/specs/SPEC-20260606-rag-index-timeout-recovery.md`
- `docs/specs/SPEC-20260606-rag-index-recovery-scheduler-lock.md`
- `docs/product/PRD-20260606-rag-index-worker-progress.md`
- `docs/requirements/REQ-20260606-rag-index-worker-progress.md`
- `docs/specs/SPEC-20260606-rag-index-worker-progress.md`
- `docs/plans/PLAN-20260606-rag-index-worker-progress.md`
- `docs/tasks/TASK-20260606-rag-index-worker-progress.md`

## 已选 Skills

- `feature-development-workflow`
- `brainstorming`
- `educational-rag-pipeline`
- `test-driven-development`
- `security-review`
- `unit-test-generator`
- `verification-before-completion`
- `systematic-debugging`
- `dispatching-parallel-agents`

## Subagent 计划

### 是否启用

是。

### 原因

任务涉及 RAG、数据库、后台 worker、权限、安全和测试；用户要求专家 subagent 并行。

### 并行级别

L1 并行分析 / 审查。实现阶段由 Main Codex 单线程执行。

### 专家报告

- `docs/subagents/runs/RUN-20260606-rag-index-worker-progress-rag-backend-expert.md`
- `docs/subagents/runs/RUN-20260606-rag-index-worker-progress-security-quality.md`
- `docs/subagents/runs/RUN-20260606-rag-index-worker-progress-integration-review.md`

## 允许修改的文件

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

## 禁止修改的文件

- `frontend/**`
- `docs/superpowers/**`
- `backend/pom.xml`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/RerankerService.java`
- `backend/src/main/java/com/learningos/rag/application/AdaptiveRagRouter.java`
- `backend/src/main/java/com/learningos/orchestrator/**`
- 与 P3-2 本切片无关的业务模块

## 测试命令

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

## 当前任务边界

- 只关闭 P3-2 的 worker/progress/heartbeat/retry/detail API 项。
- 不关闭 parser adapter/OCR、token chunk、embedding/VectorDB、hybrid retrieval/reranker 项。
- 不把整个 P3-2 或整个 backend TODO 标记完成。
