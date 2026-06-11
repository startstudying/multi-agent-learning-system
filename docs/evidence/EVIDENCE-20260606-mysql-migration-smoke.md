# Evidence - MySQL 迁移真实验证

## 1. 范围

本证据归档 TODO P3-1：使用真实 MySQL 8 从空 schema 执行当前 Flyway V1-V15，并验证 H2 测试与 MySQL 方言差异。

## 2. 关键变更证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/resources/db/migration/V1__rag_foundation.sql` | `kb_query_log.kb_ids_json/question/sources_json` 从大 `varchar` 修正为 `text`，解决 MySQL 8 `ERROR 1118 Row size too large`。 |
| `backend/src/main/java/com/learningos/rag/domain/KbQueryLog.java` | 对应字段和 `responseJson` 使用 `@Column(columnDefinition = "text")`，避免 Hibernate schema drift。 |
| `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java` | 移除 `.target("5")`，断言当前版本 `15`、成功迁移数 `15`、V1-V15 关键对象、MySQL 8、InnoDB、utf8mb4、helper routines 清理。 |
| `scripts/mysql-migration-smoke.ps1` | isolated runner 调用最新测试方法。 |
| `SchemaConvergenceMigrationTest` | 增加 `kb_query_log` 大文本列为 `text` 的文本级回归断言。 |

## 3. TDD / Debug 证据

### RED-01：真实 MySQL 首次暴露 V1 行大小失败

命令：

```powershell
$env:JAVA_TOOL_OPTIONS='-Dlearningos.mysql.smoke=true -Dlearningos.mysql.smoke.url=jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
mvn --% -Dtest=MysqlMigrationSmokeTest test
```

结果：失败。MySQL 8.0.46 在 V1 创建 `kb_query_log` 时返回：

```text
ERROR 1118 Row size too large
Location: db/migration/V1__rag_foundation.sql
Line: 88
```

根因：`kb_query_log` 多个大 `varchar` 字段在 `utf8mb4` 下超过 InnoDB 行大小限制。修复方式已记录在 SPEC。

### RED-02：修复 V1 后旧 V5 target 暴露覆盖不足

命令同上。

结果：失败。Flyway 成功执行 V1-V5，但测试期望当前 V15：

```text
Successfully applied 5 migrations ... now at version v5
expected: 15 but was: 5
```

结论：旧 `.target("5")` 不能关闭当前仓库的 P3-1。

## 4. 正向验证命令

### 4.0 收口新鲜验证

2026-06-06 22:17-22:22 在文档更新后重新执行以下验证：

- `mvn --% -Dtest=MysqlMigrationSmokeTest test`：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 1`
- `mvn --% -Dtest=SchemaConvergenceMigrationTest test`：`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`
- `mvn test`：`Tests run: 217, Failures: 0, Errors: 0, Skipped: 1`
- `scripts/mysql-migration-smoke.ps1` with `MYSQL_PORT=3307`：`Successfully applied 15 migrations ... now at version v15`，`MySQL migration smoke test passed.`

### 4.1 默认 smoke 跳过

命令：

```powershell
cd backend
mvn --% -Dtest=MysqlMigrationSmokeTest test
```

结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

### 4.2 迁移文本覆盖

命令：

```powershell
cd backend
mvn --% -Dtest=SchemaConvergenceMigrationTest test
```

结果：

```text
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 4.3 真实 MySQL 8 smoke

环境说明：本机已有 `MySQL84` 占用 3306，因此本次通过 `MYSQL_PORT=3307` 启动 `backend/docker-compose.yml` 的 `mysql:8.0` 服务。

命令：

```powershell
$env:MYSQL_PORT='3307'
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
```

结果：

```text
Starting MySQL service from backend/docker-compose.yml
Database: jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke... (MySQL 8.0)
Successfully validated 15 migrations
Current version of schema `learning_os_migration_smoke`: << Empty Schema >>
Migrating schema ... to version "1 - rag foundation"
...
Migrating schema ... to version "15 - agent tool call trace governance"
Successfully applied 15 migrations to schema `learning_os_migration_smoke`, now at version v15
MySQL migration smoke test passed.
```

说明：Flyway 输出了若干 idempotent DDL 警告，例如 `Table ... already exists` 和 `PROCEDURE ... does not exist`，来源于迁移脚本中的 `create table if not exists` / `drop procedure if exists` 模式；最终迁移成功且 information_schema 断言全部通过。

### 4.4 后端全量回归

命令：

```powershell
cd backend
Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
mvn test
```

结果：

```text
Tests run: 217, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. H2 / MySQL 方言差异

- `application-test.yml` 使用 H2 `MODE=MySQL`，但 `spring.flyway.enabled=false`。
- H2 普通测试不执行 Flyway SQL，无法覆盖 `DELIMITER`、stored procedure、dynamic DDL、`DATABASE()`、InnoDB、`utf8mb4_0900_ai_ci`、MySQL check constraint 和外键行为。
- `SchemaConvergenceMigrationTest` 是文本级防回归；真实可执行性由 `MysqlMigrationSmokeTest` + MySQL 8 smoke 覆盖。

## 6. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 未修改 Controller/Service/Repository 业务流程。 |
| Frontend rules | PASS | 未修改前端。 |
| Agent / RAG rules | PASS | 未改变 Agent/RAG 调用链；仅同步 `KbQueryLog` schema 映射。 |
| Security | PASS | 未新增依赖；本地默认 DB 凭据仅用于 smoke/dev；schema guard 保留。 |
| API / Database | PASS | SPEC 已记录 V1 行大小修复与 V1-V15 smoke 验收。 |

## 6.1 Subagent Review

| Review | Result |
|---|---|
| `docs/subagents/runs/RUN-20260606-mysql-migration-smoke-code-review.md` | 0 findings；确认 V1 row-size fix、V1-V15 smoke harness、runner 方法、schema assertions 和 checksum 风险记录均对齐。 |
| Verifier subagent `019e9d4c-7927-78f0-8b2d-e0dca018235d` | PASS for P3-1 only；唯一 low finding 是 `docs/memory/AGENT_RAG_MEMORY.md` 的陈旧 MySQL smoke open issue，已清理为仅保留 RAG query concurrency stress。 |

## 7. 残余风险

- 历史 V1 被最小修正。若存在已经应用旧 V1 的外部环境，需要单独制定 Flyway checksum repair 或环境迁移策略；本仓库当前目标是空库真实迁移可执行。
- Docker Compose 默认密码和 root smoke 用户只适用于本地 smoke/dev，不可作为生产部署凭据。
- 真实 MySQL smoke 仍是显式运行项，普通 `mvn test` 不会自动启动 MySQL。
