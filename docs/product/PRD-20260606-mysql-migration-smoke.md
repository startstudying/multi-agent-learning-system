# PRD - MySQL 迁移真实验证

## 1. 问题陈述

当前后端普通测试使用 H2 MySQL mode，并在 `application-test.yml` 中关闭 Flyway。该配置能支撑快速单元/集成测试，但不能证明 `db/migration` 中的 MySQL 专属语法可以在真实 MySQL 8 上从空库完整执行。

P3-1 原始 TODO 要求从空库执行 V1-V5 迁移；但当前仓库迁移链已经推进到 V15。若只验证 V1-V5，会继续留下 V6-V15 的真实 MySQL 方言、索引、约束和外键执行风险。因此本切片以“原始 V1-V5 验收 + 当前 V1-V15 全链路 smoke”为完成标准。

## 2. 目标用户

| 用户 | 需求 |
|---|---|
| 后端开发者 | 在本地用 Docker Compose 启动 MySQL 8，并一条命令验证 Flyway 迁移链。 |
| 集成/测试负责人 | 获得可归档的 MySQL 8 空库迁移证据，区分 H2 覆盖与真实 MySQL 覆盖。 |
| 运维/架构负责人 | 确认本地默认凭据仅用于 smoke/dev，不作为生产配置。 |

## 3. MVP 范围

### 纳入范围

- 使用现有 `backend/docker-compose.yml` 启动 MySQL 8。
- 保持 `MysqlMigrationSmokeTest` 默认跳过，只有显式 `learningos.mysql.smoke=true` 才运行。
- 从空 MySQL schema 执行当前全量 Flyway 迁移 V1-V15。
- 保留 V1-V5 原始关键对象验证。
- 增加 V6-V15 关键表、列、索引、约束与 helper routine 清理验证。
- 记录 H2 与 MySQL 方言差异。
- 生成 Evidence、Acceptance、Retrospective，并更新 TODO、Memory、Changelog。

### 排除范围

- 不引入 Testcontainers 或其他新依赖。
- 不修改业务 API、实体、生产迁移 SQL 或前端代码。
- 不把本地 Docker Compose 默认凭据提升为生产配置。
- 不建立持续集成中的强制 MySQL 服务，本轮只提供可手动运行的真实 smoke 路径。

## 4. 成功指标

| 指标 | 目标 |
|---|---|
| 普通测试影响 | 未设置 `learningos.mysql.smoke=true` 时 smoke 测试跳过，不要求本地 MySQL。 |
| 真实 MySQL smoke | `scripts/mysql-migration-smoke.ps1` 能启动/使用 MySQL 8 并执行 V1-V15。 |
| 迁移完整性 | `flyway_schema_history` 有 15 条成功版本记录，当前版本为 `15`。 |
| 方言差异说明 | 文档明确 H2 关闭 Flyway，真实 MySQL 才覆盖 `DELIMITER`、procedure、InnoDB、utf8mb4 等。 |

## 5. 依赖与约束

- 复用现有 `flyway-core`、`flyway-mysql`、`mysql-connector-j`。
- 复用现有 `backend/docker-compose.yml` 的 `mysql:8.0` 服务。
- 不新增依赖，因此不需要新增 dependency review。
- smoke 会 drop/create 名称包含 `migration_smoke` 的 schema，默认 schema 为 `learning_os_migration_smoke`。

## 6. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-06 | 通过 |
