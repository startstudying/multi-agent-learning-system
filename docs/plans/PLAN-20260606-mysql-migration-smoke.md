# PLAN - MySQL 迁移真实验证

## 1. 追踪

- PRD：`docs/product/PRD-20260606-mysql-migration-smoke.md`
- REQ：`docs/requirements/REQ-20260606-mysql-migration-smoke.md`
- SPEC：`docs/specs/SPEC-20260606-mysql-migration-smoke.md`
- Context Pack：`docs/context/CONTEXT-20260606-mysql-migration-smoke.md`

## 2. Skill Selection Report

### Task Type

数据库迁移验证、测试基础设施、后端工程质量闭环。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 项目要求所有功能/修复按 PRD/REQ/SPEC/PLAN/TASK/Context/Evidence 闭环执行。 |
| test-driven-development | smoke harness 行为变更需要先让旧 V5 target 暴露失败，再实现 V1-V15。 |
| verification-before-completion | 关闭 P3-1 前必须有新鲜命令证据。 |
| database-design | 验证 Flyway、MySQL 8 方言、表/列/索引/约束。 |
| test-generator | 补充可重复运行的 smoke 验证断言。 |
| security-review | 检查本地默认凭据、schema drop/create 风险、敏感信息边界。 |
| dependency-review | 明确不新增 Testcontainers 或其他依赖。 |
| architecture-drift-check | 确认不改变后端分层、API、Agent/RAG 规则。 |

### Missing Skills

无。

### GitHub Research Needed

No。现有 Flyway/MySQL/JUnit/Compose 路径足够，且不新增依赖。

### New Project-Specific Skill To Create

暂不创建。若后续把 MySQL smoke 纳入 CI，可提炼数据库迁移验证技能。

## 3. Subagent Decision

Use Subagents: Yes  
Reason: 用户明确要求专家 subagent 并行开发；该任务涉及数据库、测试基础设施、安全/质量。  
Parallelism Level: L1 并行分析 / 设计  
Selected Subagents:

| 专家 | 输出 |
|---|---|
| Database/Migration Expert | 建议 P3-1 不应关闭到 V1-V5，应扩展到当前 V1-V15。 |
| Backend Test Harness Expert | 确认可保留 gated JUnit + PowerShell runner，不引入 Testcontainers。 |
| Security & Quality Expert | 指出本地默认凭据可用于 smoke，schema guard 必须保留，证据必须证明真实 MySQL 执行。 |

Implementation Mode: Single Codex。原因是最终代码变更集中在同一 smoke test / runner 文件，避免并行写同一文件。

## 4. 实施阶段

| 阶段 | 说明 | 状态 |
|---|---|---|
| 1 | 补齐 PRD/REQ/SPEC/PLAN/TASK/Context 和 subagent 归档。 | 完成 |
| 2 | 修改 smoke 断言为当前 V1-V15，先在旧 `.target("5")` 下验证失败。 | 完成 |
| 3 | 修复真实 MySQL 暴露的 V1 `kb_query_log` 行大小溢出。 | 完成 |
| 4 | 移除 Flyway V5 target，更新 runner 方法名，补充 V6-V15 验证。 | 完成 |
| 5 | 运行默认跳过、文本测试、真实 MySQL smoke、全量后端测试。 | 完成 |
| 6 | 生成 Evidence/Acceptance/Retro，更新 TODO/Memory/Changelog。 | 完成 |

## 5. 文件变更清单

| 文件 | 操作 |
|---|---|
| `docs/product/PRD-20260606-mysql-migration-smoke.md` | 新增 |
| `docs/requirements/REQ-20260606-mysql-migration-smoke.md` | 新增 |
| `docs/specs/SPEC-20260606-mysql-migration-smoke.md` | 新增 |
| `docs/plans/PLAN-20260606-mysql-migration-smoke.md` | 新增/更新 |
| `docs/tasks/TASK-20260606-mysql-migration-smoke.md` | 新增/更新 |
| `docs/context/CONTEXT-20260606-mysql-migration-smoke.md` | 替换旧 V1-V5-only scope |
| `docs/subagents/runs/RUN-20260606-mysql-migration-smoke-*.md` | 新增 subagent 归档和集成报告 |
| `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java` | 修改 smoke 覆盖 V1-V15 |
| `scripts/mysql-migration-smoke.ps1` | 更新 isolated runner 调用方法名 |
| `backend/src/main/resources/db/migration/V1__rag_foundation.sql` | 修复真实 MySQL V1 `kb_query_log` 行大小溢出 |
| `backend/src/main/java/com/learningos/rag/domain/KbQueryLog.java` | 同步长文本列 schema 映射，避免 Hibernate validate 漂移 |
| `docs/harness/TEST_COMMANDS.md` | 增加 MySQL smoke 命令 |
| `docs/evidence/EVIDENCE-20260606-mysql-migration-smoke.md` | 新增 |
| `docs/acceptance/ACCEPT-20260606-mysql-migration-smoke.md` | 新增 |
| `docs/retrospectives/RETRO-20260606-mysql-migration-smoke.md` | 新增 |
| `docs/planning/backend-architecture-todolist.md` | 完成 P3-1 |
| `docs/changelog/CHANGELOG.md` | 更新 |
| `docs/memory/PROJECT_MEMORY.md` | 更新 |
| `docs/memory/BACKEND_MEMORY.md` | 更新 |
| `docs/memory/DATABASE_MEMORY.md` | 更新 |

## 6. 风险评估

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| Docker daemon 不可用 | 无法产生真实 MySQL evidence | 脚本可连接已有 MySQL；若仍不可用，不关闭 P3-1。 |
| 3306 端口冲突 | compose 启动失败 | 支持 `MYSQL_PORT` 或传入自定义 JDBC URL。 |
| smoke 误删非测试 schema | 数据风险 | schema 名称限制必须保留，默认包含 `migration_smoke`。 |
| V6-V15 check 过重 | smoke 变慢或脆弱 | 只验证关键对象，不做全 schema diff。 |
| 修改历史 V1 migration | 已部署环境 checksum 风险 | 本项目当前没有生产发布证据；空库 V1 本身无法执行，V16 不能修复首次迁移。Evidence 明确记录原因，后续已部署环境需单独 repair/变更策略。 |

## 7. 架构漂移检查

实施前检查：本切片不改 Controller/Service/Repository/Agent/RAG/API，不新增依赖；因真实 MySQL smoke 暴露 V1 DDL 在空库无法执行，允许最小修正 V1 schema 类型并同步 entity 映射。预期无分层/API 漂移。

实施后检查：在 Evidence / Acceptance 中记录。

## 8. 测试策略

| 命令 | 目的 |
|---|---|
| `cd backend && mvn "-Dtest=MysqlMigrationSmokeTest" test` | 验证默认跳过，不影响普通测试。 |
| `cd backend && mvn "-Dtest=SchemaConvergenceMigrationTest" test` | 验证迁移文本覆盖和 H2/MySQL 差异说明。 |
| `powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1` | 真实 MySQL 8 V1-V15 smoke。 |
| `cd backend && mvn test` | 后端全量回归。 |

## 8.1 验证结果

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=MysqlMigrationSmokeTest test` | 通过：1 skipped |
| `mvn --% -Dtest=SchemaConvergenceMigrationTest test` | 通过：13 tests, 0 failures |
| `powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?...'` | 通过：MySQL 8.0，V1-V15，当前版本 v15 |
| `mvn test` | 通过：217 tests, 0 failures, 0 errors, 1 skipped |

## 9. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-06 | 通过 |
