# Subagent 代码评审 - MySQL Migration Smoke

## 功能名称

P3-1 MySQL 迁移真实验证

## 评审范围

- `backend/src/main/resources/db/migration/V1__rag_foundation.sql`
- `backend/src/main/java/com/learningos/rag/domain/KbQueryLog.java`
- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `scripts/mysql-migration-smoke.ps1`
- P3-1 workflow docs

## Findings

未发现阻塞问题。

## Verification Performed

| 命令 | 结果 |
|---|---|
| `cd backend; mvn "-Dtest=MysqlMigrationSmokeTest" test` | 通过，默认跳过 smoke。 |
| `cd backend; mvn "-Dtest=SchemaConvergenceMigrationTest" test` | 通过，13 tests。 |
| `cd backend; mvn -q -DskipTests test-compile` | 通过。 |
| scoped `rg` security/pattern scan | 未发现阻塞问题；脚本中的默认密码仅属于文档化 local smoke scope。 |

## Scope Checks

- V1 row-size fix 已对齐：`kb_query_log.kb_ids_json/question/sources_json` 为 `text`，entity 使用 `@Column(columnDefinition = "text")`。
- V1-V15 smoke harness 已对齐：latest version/count 为 `15`，Flyway 不再 target V5。
- PowerShell runner 已调用最新测试方法。
- schema assertions 覆盖 V1-V5 与 V6-V15 关键对象。
- 历史 V1 edit checksum 风险已在 Evidence / Acceptance 中记录。

## Residual Risks

- 当前目录不是 Git working tree，无法用 `git diff` 审查；本次基于直接文件检查。
- 普通 `mvn test` 仍故意跳过真实 MySQL smoke，发布前需显式运行 smoke 命令。
