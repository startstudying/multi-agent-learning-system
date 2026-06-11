# SPEC - MySQL 迁移真实验证

## 1. 概述

本规格定义 P3-1 的真实 MySQL 8 迁移验证方式：通过显式开启的 JUnit smoke test 和 PowerShell runner，从空 schema 执行当前 `classpath:db/migration` 下的 Flyway V1-V15 全链路迁移，并验证 MySQL 方言相关对象。

## 2. 追踪

- PRD：`docs/product/PRD-20260606-mysql-migration-smoke.md`
- REQ：`docs/requirements/REQ-20260606-mysql-migration-smoke.md`
- TODO：`docs/planning/backend-architecture-todolist.md` P3-1

## 3. 设计约束

- `MysqlMigrationSmokeTest` 使用 `@EnabledIfSystemProperty(named = "learningos.mysql.smoke", matches = "true")`。
- 不使用 Testcontainers，不修改 `pom.xml`。
- 真实 MySQL 启动通过 `backend/docker-compose.yml` 的 `mysql:8.0`。
- 默认 schema 为 `learning_os_migration_smoke`，清理逻辑只允许安全名称。

## 4. 迁移执行规则

```text
读取 smoke 配置
-> 校验 schema 名称
-> DROP/CREATE 空 schema
-> Flyway.locations("classpath:db/migration")
-> migrate() 当前全部 migration
-> 查询 flyway_schema_history 和 information_schema
-> 默认清理 schema
```

不再使用 `.target("5")`；当前完成标准为版本 `15`。

## 4.1 真实 MySQL 失败根因与修复约束

首次真实 MySQL 8.0.46 smoke 在 V1 创建 `kb_query_log` 时暴露 `ERROR 1118 Row size too large`。根因是 `kb_ids_json varchar(4000)`、`question varchar(4000)`、`sources_json varchar(8000)` 在 `utf8mb4` 下会按最大字节计入 InnoDB 行大小，超过 65,535 字节。

修复约束：

- 因失败发生在空库 V1 `CREATE TABLE` 阶段，新增 V16 不能修复空库首次迁移。
- 允许最小修改历史 V1，将非索引的大文本/JSON payload 列改为 `text`。
- 同步 JPA entity columnDefinition，避免 Hibernate validate 对生产 schema 类型产生漂移。
- 不修改业务行为、不新增依赖。

## 5. MySQL schema 验证点

### V1-V5 原始 P3-1 验证

- `kb_knowledge_base`、`app_user`、`answer_record`、`resource_generation_task` 存在。
- `kb_query_log.kb_ids_json/question/sources_json` 为 `text`，避免 MySQL utf8mb4 行大小溢出。
- `resource_generation_task.request_id` 与 `uk_rgt_learner_request` 存在。
- `answer_record.request_id/request_hash/response_json` 与 `uk_answer_learner_request` 存在。
- `agent_task.idx_agent_task_owner` 存在。
- `answer_record` 使用 InnoDB、`utf8mb4`，`response_json` 为 `text`。
- helper routines `add_column_if_missing`、`add_index_if_missing` 已清理。
- `flyway_schema_history` 中 V1-V5 均成功。

### V6-V15 当前迁移链验证

- V6：`resource_review.reason/citation_check/safety_check/revision_suggestion`。
- V7：`kb_query_log.request_id/request_hash/response_json` 与 `uk_kb_query_user_request`。
- V8：`kb_document.request_id/request_hash/response_json` 与 `uk_kb_document_user_request`。
- V9：`learning_path_node.recommendation_reason/estimated_duration_minutes/resource_type/assessment_binding_relation`。
- V10：`learning_path.profile_snapshot` 与 `resource_generation_task.profile_snapshot`。
- V11：`resource_generation_task.retry_count/next_retry_at/last_error/recoverable`。
- V12：`model_call_log.prompt_code/prompt_version/temperature/structured_output_schema`。
- V13：`evaluation_set`、`evaluation_sample` 表和关键索引。
- V14：`evaluation_run`、`evaluation_run_metric` 表、外键/唯一索引/check constraints。
- V15：`agent_tool_call.trace_id/input_summary/output_summary/retention_class` 与 trace/status 索引。

## 6. H2 与 MySQL 差异

- `application-test.yml` 使用 H2 `MODE=MySQL`，但 `spring.flyway.enabled=false`。
- H2 普通测试不执行 Flyway SQL，因此不能覆盖 `DELIMITER`、`CREATE PROCEDURE`、`PREPARE`、`DATABASE()`、InnoDB table option、`utf8mb4_0900_ai_ci` 和 MySQL check/foreign key 行为。
- `SchemaConvergenceMigrationTest` 只做迁移文本覆盖；真实可执行性由本 smoke 负责。

## 7. 错误处理

- 连接、迁移、断言失败时抛出带本地运行提示的 `AssertionError`。
- schema 清理失败不能覆盖原始迁移失败。
- 脚本重试耗尽后返回失败。

## 8. 安全说明

- Docker Compose 和脚本中的默认密码是本地 smoke/dev 配置。
- smoke 使用 root 是为了 drop/create 测试 schema；默认 schema 名称 guard 降低误删风险。
- 不允许把这些默认值作为生产部署凭据。

## 9. 测试策略

```bash
cd backend && mvn "-Dtest=MysqlMigrationSmokeTest" test
cd backend && mvn "-Dtest=SchemaConvergenceMigrationTest" test
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1
cd backend && mvn test
```

## 10. 验收清单

- [x] 普通 smoke 测试默认跳过。
- [x] 文本迁移覆盖测试通过。
- [x] 真实 MySQL 8 smoke V1-V15 通过。
- [x] 全量后端测试通过或说明限制。
- [x] Evidence / Acceptance 已归档。
