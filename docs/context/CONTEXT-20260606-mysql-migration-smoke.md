# Context Pack - MySQL Migration Smoke

> 语言：正文使用中文；命令、路径、类名和配置 key 保留英文。

## 当前任务

完成 TODO P3-1 的真实 MySQL 8 迁移验证：在不影响普通 `mvn test` 的前提下，通过显式开启的 smoke test 从空库执行当前 Flyway V1-V15 全量迁移，并归档 H2 与 MySQL 方言差异、真实执行结果和验收结论。

## 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 关联文档

- PRD：`docs/product/PRD-20260606-mysql-migration-smoke.md`
- REQ：`docs/requirements/REQ-20260606-mysql-migration-smoke.md`
- SPEC：`docs/specs/SPEC-20260606-mysql-migration-smoke.md`
- PLAN：`docs/plans/PLAN-20260606-mysql-migration-smoke.md`
- TASK：`docs/tasks/TASK-20260606-mysql-migration-smoke.md`

## 已选 Skills

- `feature-development-workflow`
- `test-driven-development`
- `verification-before-completion`
- `database-design`
- `test-generator`
- `security-review`
- `dependency-review`
- `architecture-drift-check`

## Subagent 计划

### 是否启用 Subagent

是。

### 原因

用户明确要求专家 subagent 并行开发；本任务涉及数据库迁移、测试基础设施、安全/质量边界。

### 并行级别

L1 并行分析 / 设计。实现阶段由 Main Codex 单线程集成，避免多个执行者修改同一 smoke test 文件。

### 专家输出归档

- `docs/subagents/runs/RUN-20260606-mysql-migration-smoke-database-expert.md`
- `docs/subagents/runs/RUN-20260606-mysql-migration-smoke-test-harness-expert.md`
- `docs/subagents/runs/RUN-20260606-mysql-migration-smoke-security-quality.md`
- `docs/subagents/runs/RUN-20260606-mysql-migration-smoke-integration-review.md`
- `docs/subagents/runs/RUN-20260606-mysql-migration-smoke-code-review.md`

## 关联代码区域

- `backend/src/main/resources/db/migration/`
- `backend/src/test/java/com/learningos/migration/`
- `backend/docker-compose.yml`
- `scripts/mysql-migration-smoke.ps1`

## 允许修改的文件

- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`，仅当迁移文本覆盖必须同步
- `backend/src/main/resources/db/migration/V1__rag_foundation.sql`，仅限修复真实 MySQL 空库 V1 `kb_query_log` row size 失败
- `backend/src/main/java/com/learningos/rag/domain/KbQueryLog.java`，仅限同步长文本列映射
- `scripts/mysql-migration-smoke.ps1`
- `docs/product/PRD-20260606-mysql-migration-smoke.md`
- `docs/requirements/REQ-20260606-mysql-migration-smoke.md`
- `docs/specs/SPEC-20260606-mysql-migration-smoke.md`
- `docs/plans/PLAN-20260606-mysql-migration-smoke.md`
- `docs/tasks/TASK-20260606-mysql-migration-smoke.md`
- `docs/context/CONTEXT-20260606-mysql-migration-smoke.md`
- `docs/subagents/runs/RUN-20260606-mysql-migration-smoke-*.md`
- `docs/evidence/EVIDENCE-20260606-mysql-migration-smoke.md`
- `docs/acceptance/ACCEPT-20260606-mysql-migration-smoke.md`
- `docs/retrospectives/RETRO-20260606-mysql-migration-smoke.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`，仅限清理与 P3-1 冲突的陈旧 MySQL smoke open issue

## 禁止修改的文件

- `backend/pom.xml`
- `backend/src/main/java/**`，除 `KbQueryLog.java` 映射同步外
- `backend/src/main/resources/db/migration/**`，除 `V1__rag_foundation.sql` 行大小修复外
- `frontend/**`
- `docs/superpowers/**`
- 与 P3-1 无关的业务模块和文档

## 测试命令

```bash
cd backend && mvn "-Dtest=MysqlMigrationSmokeTest" test
cd backend && mvn "-Dtest=SchemaConvergenceMigrationTest" test
cd backend && mvn "-Dtest=MysqlMigrationSmokeTest" "-Dlearningos.mysql.smoke=true" "-Dlearningos.mysql.smoke.url=jdbc:mysql://127.0.0.1:3306/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" "-Dlearningos.mysql.smoke.username=root" "-Dlearningos.mysql.smoke.password=learning_os_root" test
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1
cd backend && mvn test
```

## 任务边界

- 只完成 P3-1 MySQL migration smoke。
- 当前完成标准覆盖原始 V1-V5 以及仓库当前 V1-V15。
- 真实 MySQL 首次运行已暴露 V1 `kb_query_log` row-size overflow；本切片允许最小 schema 类型修复，因为 V16 不能修复空库 V1 `CREATE TABLE` 失败。
- 普通测试不能因为本地没有 MySQL 或 Docker 而失败。
- 真实 MySQL smoke 如果无法执行，不得关闭 P3-1。
- 不接触 P3-2 RAG 生产索引、P3-3 真实模型接入、P3-4 完整 RBAC、P3-5 可观测性。

## 架构漂移检查

实施前：不修改生产 Controller/Service/Repository/API/Agent/RAG，不新增依赖，不修改生产迁移 SQL。预期无漂移。

实施后：在 Evidence 和 Acceptance 中记录结果。
