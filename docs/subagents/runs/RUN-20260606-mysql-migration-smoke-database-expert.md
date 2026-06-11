# Subagent 运行报告 - MySQL Migration Smoke Database Expert

## 功能名称

P3-1 MySQL 迁移真实验证

## 主要需求

判断现有 MySQL migration smoke 是否足以关闭 P3-1，并给出 V1-V5 与 V1-V15 范围建议。

## 任务复杂度

| 影响模块 | 数量 | Agent/RAG | 安全 |
|---|---|---|---|
| Database / Backend Test | 2 | 否 | 是，本地 drop/create schema 风险 |

## 已选 Skills

- database-design
- test-generator
- architecture-drift-check

## 专家输出

- 当前迁移文件已经从 V1 推进到 `V15__agent_tool_call_trace_governance.sql`。
- `MysqlMigrationSmokeTest` 是 opt-in 测试，但当前硬编码 `.target("5")`，只断言 5 条 migration。
- 普通测试使用 H2 MySQL mode 且 Flyway disabled，不能证明 MySQL-only migration SQL 可执行。
- `SchemaConvergenceMigrationTest` 提供 V1-V15 文本级覆盖，但不能替代真实 MySQL smoke。
- P3-1 不应在仅 V1-V5 的状态下关闭；建议扩展到当前 V1-V15，并保留原始 V1-V5 验收点。

## 关键风险

| 风险 | 说明 | 建议 |
|---|---|---|
| V6-V15 未真实执行 | 迁移链已增长，旧 smoke 只到 V5。 | 移除 `.target("5")`，断言当前版本 `15`。 |
| H2 覆盖误判 | H2 测试不运行 Flyway。 | Evidence 明确区分文本覆盖与真实 MySQL 覆盖。 |

## 最终建议

关闭 P3-1 前必须运行并归档 MySQL 8 空库 Flyway V1-V15 成功证据。
