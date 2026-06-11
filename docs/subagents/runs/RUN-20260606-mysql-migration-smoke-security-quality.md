# Subagent 运行报告 - MySQL Migration Smoke Security & Quality

## 功能名称

P3-1 MySQL 迁移真实验证

## 主要需求

审查本地 MySQL smoke 的安全边界、配置风险和验收证据要求。

## 任务复杂度

| 影响模块 | 数量 | Agent/RAG | 安全 |
|---|---|---|---|
| Docker Compose / Test / Config | 3 | 否 | 是 |

## 已选 Skills

- security-review
- dependency-review
- architecture-drift-check

## 专家输出

- `backend/docker-compose.yml` 使用 `mysql:8.0` 和可预测本地默认密码；只适合作为 local smoke/dev。
- `scripts/mysql-migration-smoke.ps1` 默认连接 `127.0.0.1`、`root`、`learning_os_root`，必须在文档中标记为本地 smoke。
- smoke test 会 drop/create schema；当前 schema 名称必须包含 `migration_smoke` 的 guard 应保留。
- H2 无法覆盖 `DELIMITER`、procedure、dynamic DDL、InnoDB、utf8mb4 和 MySQL check/foreign key 行为。
- 真实证据必须包括 MySQL 8 版本、Flyway 成功版本、关键对象存在、helper routines 已清理、普通测试不受影响。

## 开放风险

| 风险 | 结论 |
|---|---|
| 本地默认凭据 | 可接受于 local smoke，不可复用于生产。 |
| root drop/create schema | 依赖 schema guard 和默认 smoke schema；不得使用非 smoke schema 关闭 P3-1。 |
| V6-V15 未覆盖 | 如果只跑 V1-V5，当前仓库仍有迁移风险。 |

## 最终建议

Evidence 必须证明默认命令使用 `learning_os_migration_smoke`，并真实执行当前迁移链。
