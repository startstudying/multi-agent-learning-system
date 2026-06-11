# REQ - MySQL 迁移真实验证

## 1. 追踪

- PRD：`docs/product/PRD-20260606-mysql-migration-smoke.md`
- TODO：`docs/planning/backend-architecture-todolist.md` P3-1
- 需求编号：REQ-20260606-mysql-migration-smoke

## 2. 功能需求

| 编号 | 需求描述 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | smoke 测试必须默认跳过，只在 `learningos.mysql.smoke=true` 时运行。 | 必须 | 普通 `mvn "-Dtest=MysqlMigrationSmokeTest" test` 不连接 MySQL。 |
| FR-02 | smoke runner 必须可通过 `scripts/mysql-migration-smoke.ps1` 启动现有 Compose MySQL。 | 必须 | 脚本使用 `backend/docker-compose.yml` 的 `mysql:8.0` 服务。 |
| FR-03 | smoke 必须从空 schema 执行当前迁移链 V1-V15。 | 必须 | `flyway_schema_history` 成功版本数为 15，当前版本为 `15`。 |
| FR-04 | smoke 必须覆盖原始 P3-1 V1-V5 关键对象。 | 必须 | V1-V5 表、列、索引、InnoDB、utf8mb4、helper routine 清理均通过断言。 |
| FR-05 | smoke 必须增加 V6-V15 关键对象断言。 | 必须 | V6-V15 新增列、表、索引、约束可在真实 MySQL 查询到。 |
| FR-06 | 文档必须解释 H2 与 MySQL 方言差异。 | 必须 | Evidence / Acceptance / Context Pack 记录 H2 Flyway disabled 与 MySQL-only SQL。 |

## 3. 非功能需求

| 编号 | 需求描述 | 优先级 |
|---|---|---|
| NFR-01 | 不新增依赖。 | 必须 |
| NFR-02 | 不修改生产业务代码和生产迁移 SQL。 | 必须 |
| NFR-03 | smoke schema 名称必须有安全限制，避免误删非测试库。 | 必须 |
| NFR-04 | Docker/本地默认密码只能作为 local smoke/dev 示例，不写入生产指南。 | 必须 |

## 4. 输入 / 输出

### 输入

| 输入 | 默认值 | 说明 |
|---|---|---|
| `learningos.mysql.smoke.url` | `jdbc:mysql://127.0.0.1:3306/learning_os_migration_smoke?...` | smoke schema JDBC URL |
| `learningos.mysql.smoke.username` | `root` | 本地 smoke 用户 |
| `learningos.mysql.smoke.password` | `learning_os_root` | 本地 compose root 密码 |
| `learningos.mysql.smoke.keepSchema` | `false` | 是否保留 smoke schema |

### 输出

| 输出 | 说明 |
|---|---|
| Maven/JUnit 结果 | smoke 是否跳过或通过 |
| 脚本控制台输出 | MySQL 启动、重试、smoke 结果 |
| Evidence 文档 | 归档真实命令、结果、风险 |

## 5. 边界情况

| 场景 | 预期行为 |
|---|---|
| Docker 不可用但 MySQL 已存在 | 脚本跳过 compose 启动，尝试连接已有 MySQL。 |
| MySQL 未就绪 | 脚本按重试次数等待后重跑。 |
| schema 名称不含 `migration_smoke` | 默认失败，除非显式设置 allow flag。 |
| 未显式开启 smoke | 测试跳过，不影响普通后端测试。 |

## 6. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-06 | 通过 |
