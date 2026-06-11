# Subagent 集成评审 - MySQL Migration Smoke

## 功能名称

P3-1 MySQL 迁移真实验证

## 主要需求

整合数据库、测试基础设施、安全质量三个专家结论，形成实施决策。

## 已启用专家

| 专家 | 分配任务 | 状态 |
|---|---|---|
| Database/Migration Expert | 迁移链覆盖范围与 V1-V15 风险分析 | 完成 |
| Backend Test Harness Expert | smoke runner、依赖、执行命令分析 | 完成 |
| Security & Quality Expert | 本地凭据、schema 清理、证据要求分析 | 完成 |

## 并行级别

- [x] L1 - 仅并行分析
- [ ] L2 - 并行设计
- [ ] L3 - worktree 并行实现

## 发现的冲突

| 冲突 | 来源 | 解决方案 |
|---|---|---|
| P3-1 原文写 V1-V5，但当前迁移链已到 V15 | TODO 原始 wording vs 专家分析 | 完成标准扩展为“原始 V1-V5 验收 + 当前 V1-V15 全链路 smoke”。 |
| 是否引入 Testcontainers | 可选测试方案 | 不引入；复用现有 Docker Compose、Flyway、MySQL connector。 |
| 本地默认 root 密码是否可接受 | 安全评审 | 仅作为 local smoke/dev，Evidence 中明确不适用于生产。 |

## 最终集成决策

1. 保持 smoke test 默认跳过。
2. 使用现有 `scripts/mysql-migration-smoke.ps1` 作为真实执行入口。
3. 修改 `MysqlMigrationSmokeTest`，移除 `.target("5")`，断言当前版本 `15`。
4. 保留 V1-V5 原有验证，并补充 V6-V15 关键对象验证。
5. 真实 MySQL smoke 不通过则不关闭 P3-1。

## PLAN 更新

已写入 `docs/plans/PLAN-20260606-mysql-migration-smoke.md`。

## TASK 更新

已写入 `docs/tasks/TASK-20260606-mysql-migration-smoke.md`。

## 执行模式

单 Codex 实现，原因是代码改动集中在同一测试类和脚本，适合单线程集成。
