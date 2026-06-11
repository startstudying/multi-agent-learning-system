# TASK - MySQL 迁移真实验证

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260606-mysql-migration-smoke.md`
- SPEC：`docs/specs/SPEC-20260606-mysql-migration-smoke.md`
- 任务编号：TASK-20260606-mysql-migration-smoke

## 2. 目标

关闭 `backend-architecture-todolist.md` 的 P3-1：用真实 MySQL 8 从空库执行当前 Flyway V1-V15 迁移，并归档证据；同时保留普通测试不依赖 MySQL 的行为。

## 3. 范围

### 纳入范围

- 补齐 workflow 文档和 subagent 归档。
- 扩展 `MysqlMigrationSmokeTest` 从 V1-V5 到 V1-V15。
- 修复真实 MySQL 暴露的 V1 `kb_query_log` 行大小溢出。
- 更新 `scripts/mysql-migration-smoke.ps1` runner 调用。
- 运行并归档默认跳过、文本迁移、真实 MySQL smoke、后端全量测试。
- 更新 TODO、Memory、Changelog、Evidence、Acceptance、Retro。

### 排除范围

- 不新增依赖。
- 不修改生产业务代码。
- 不修改生产迁移 SQL。
- 不修改前端。
- 不处理 P3-2/P3-3/P3-4/P3-5 剩余项。

## 4. 允许修改的文件

- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`，仅当文本覆盖必须同步
- `backend/src/main/resources/db/migration/V1__rag_foundation.sql`，仅限修复空库迁移失败的 `kb_query_log` 长文本列类型
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

## 5. 禁止修改的文件

- `backend/pom.xml`
- `backend/src/main/java/**`，除 `KbQueryLog.java` 映射同步外
- `backend/src/main/resources/db/migration/**`，除 `V1__rag_foundation.sql` 行大小修复外
- `frontend/**`
- `docs/superpowers/**`
- 与 P3-1 无关的业务模块和文档

## 6. 实施步骤

1. 补齐 workflow 文档、Context Pack 和 subagent 集成报告。
2. 先把 smoke 断言更新为 V1-V15，并运行真实 smoke 暴露旧迁移链问题。
3. 修复真实 MySQL 暴露的 V1 `kb_query_log` 行大小溢出。
4. 移除 `.target("5")`，补齐 V6-V15 关键对象断言，更新 runner 方法名。
5. 运行默认跳过、文本覆盖、真实 MySQL smoke、全量后端测试。
6. 生成 evidence / acceptance / retrospective。
7. 更新 TODO、memory、changelog。
8. 邀请 code review subagent 做最终审查。

## 7. 测试命令

```bash
cd backend && mvn "-Dtest=MysqlMigrationSmokeTest" test
cd backend && mvn "-Dtest=SchemaConvergenceMigrationTest" test
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1
cd backend && mvn test
```

## 8. 完成标准

- [x] `MysqlMigrationSmokeTest` 默认跳过。
- [x] `SchemaConvergenceMigrationTest` 通过。
- [x] V1 `kb_query_log` 可在 MySQL 8 上创建，不再触发 row-size overflow。
- [x] `scripts/mysql-migration-smoke.ps1` 在 MySQL 8 上执行 V1-V15 成功。
- [x] `flyway_schema_history` 当前版本为 `15`，成功版本数为 15。
- [x] V1-V5 与 V6-V15 关键对象断言通过。
- [x] 后端全量测试通过或限制已解释。
- [x] Evidence / Acceptance / Retro 完成。
- [x] TODO / Memory / Changelog 更新。

## 9. 状态

| 字段 | 值 |
|---|---|
| 状态 | 完成 |
| 负责人 | Main Codex |
| 开始日期 | 2026-06-06 |
| 完成日期 | 2026-06-06 |

## 10. 证据

- Evidence：`docs/evidence/EVIDENCE-20260606-mysql-migration-smoke.md`
- Acceptance：`docs/acceptance/ACCEPT-20260606-mysql-migration-smoke.md`
- Retrospective：`docs/retrospectives/RETRO-20260606-mysql-migration-smoke.md`
