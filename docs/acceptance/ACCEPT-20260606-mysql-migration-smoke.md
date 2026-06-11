# Acceptance - MySQL 迁移真实验证

## 1. 验收结论

P3-1 MySQL Migration 真实验证已通过。

本次验收证明：

- `backend/docker-compose.yml` 的 MySQL 8 服务可用于本地真实 smoke。
- 从空 schema 执行当前 Flyway V1-V15 成功，当前版本为 `15`。
- 原始 P3-1 V1-V5 验收点仍被覆盖。
- V6-V15 当前迁移链关键对象也被真实 MySQL 断言覆盖。
- H2 与 MySQL 方言差异已文档化。
- 普通后端测试不依赖 MySQL，smoke 未显式开启时跳过。

## 2. 验收清单

| 验收项 | 状态 | 证据 |
|---|---|---|
| 使用 `backend/docker-compose.yml` 启动 MySQL 8 | 通过 | `scripts/mysql-migration-smoke.ps1` 输出 `Starting MySQL service from backend/docker-compose.yml`，数据库日志显示 MySQL 8.0。 |
| 从空库执行 V1-V5 | 通过 | smoke 输出 V1-V5 均执行，测试断言 `flyway_schema_history` V1-V5 成功数为 5。 |
| 从空库执行当前 V1-V15 | 通过 | smoke 输出 `Successfully applied 15 migrations ... now at version v15`。 |
| 修复 V1 MySQL row-size 问题 | 通过 | V1 smoke 成功；`kb_query_log.kb_ids_json/question/sources_json/response_json` 断言为 `text`。 |
| 检查 H2 / MySQL 方言差异 | 通过 | SPEC、Context、Evidence 均记录 H2 Flyway disabled 与 MySQL-only SQL 风险。 |
| 普通测试不受 MySQL 影响 | 通过 | `MysqlMigrationSmokeTest` 默认 `Tests run: 1, Skipped: 1`。 |
| 后端回归 | 通过 | `mvn test`：217 tests, 0 failures, 0 errors, 1 skipped。 |
| 收口审查 | 通过 | Verifier subagent 判定 PASS for P3-1 only；陈旧 `AGENT_RAG_MEMORY.md` MySQL smoke open issue 已清理，剩余 P3-2/P3-5 未误标完成。 |

## 3. 测试结果

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=MysqlMigrationSmokeTest test` | 通过：1 skipped |
| `mvn --% -Dtest=SchemaConvergenceMigrationTest test` | 通过：13 tests |
| `powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?...'` | 通过：MySQL 8 V1-V15 |
| `mvn test` | 通过：217 tests, 0 failures, 0 errors, 1 skipped |

## 4. 非目标与后续

- 不新增 Testcontainers 或 CI 强制 MySQL job。
- 不处理 P3-2 RAG 生产索引、P3-3 真实模型接入、P3-4 完整 RBAC、P3-5 可观测性。
- 若未来存在已部署且已应用旧 V1 checksum 的环境，需要单独制定 Flyway repair/变更策略。

## 5. 审批状态

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-06 | 通过 |
