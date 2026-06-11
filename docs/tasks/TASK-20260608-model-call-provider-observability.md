# TASK-20260608 模型调用 provider 持久化观测

## Task：P3-3-B `model_call_log.provider` schema / provider observability

Status：Completed。

## Parent Docs

- `docs/product/PRD-20260608-model-call-provider-observability.md`
- `docs/requirements/REQ-20260608-model-call-provider-observability.md`
- `docs/specs/SPEC-20260608-model-call-provider-observability.md`
- `docs/plans/PLAN-20260608-model-call-provider-observability.md`

## Done Criteria

- [x] V18 migration 为 `model_call_log` 增加 `provider varchar(80) not null default 'none'`。
- [x] `ModelCallLog` entity 增加 `provider` 字段，默认值和 `@PrePersist` 兜底为 `none`。
- [x] `AgentRunRecorder` 成功路径从 `ModelResponse.provider()` 写 provider。
- [x] `AgentRunRecorder` 失败路径支持 provider overload；旧方法保持兼容并默认 `none`。
- [x] `AiModelGateway` provider 归一化为低基数安全值，未知/敏感配置归一化为 `other`。
- [x] `model_call_log.errorMessage` 仍只保存安全错误码，不保存 raw provider error。
- [x] Migration / recorder / gateway / resource generation 测试覆盖 provider。
- [x] 不新增依赖、不修改前端、不接真实 provider、不新增 API。
- [x] Evidence / Acceptance / Memory / Changelog / Retrospective / Skill 更新。

## Execution Evidence

| Item | Result |
|---|---|
| Focused + adjacent tests | `mvn --% -Dtest=SchemaConvergenceMigrationTest,AgentRunRecorderTest,AiModelGatewayTest,ResourceGenerationControllerTest test` -> BUILD SUCCESS；51 tests, 0 failures |
| Full backend tests | `mvn test` -> BUILD SUCCESS；306 tests, 0 failures, 1 skipped |
| MySQL smoke | 已执行但受环境限制失败：本机 3306 MySQL `root` 凭据不可用，`Access denied`；待环境恢复后补验 |
| Evidence | `docs/evidence/EVIDENCE-20260608-model-call-provider-observability.md` |
| Acceptance | `docs/acceptance/ACCEPT-20260608-model-call-provider-observability.md` |

## Allowed Files

- `backend/src/main/resources/db/migration/V18__model_call_provider_observability.sql`
- `backend/src/main/java/com/learningos/agent/domain/ModelCallLog.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- `backend/src/test/java/com/learningos/agent/application/AgentRunRecorderTest.java`
- `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `docs/subagents/runs/RUN-20260608-model-call-provider-observability-integration.md`
- 本任务 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / Evidence / Acceptance / Memory / Changelog / Retro / Skill 文件。

## Files Not Allowed

- `backend/pom.xml`
- `frontend/**`
- `docs/superpowers/**`
- RAG parser / index / retrieval implementation files
- Auth / RBAC implementation files
- 任何 provider SDK 或 secret 配置文件

## Test Commands

```powershell
cd backend
mvn --% -Dtest=SchemaConvergenceMigrationTest,AgentRunRecorderTest,AiModelGatewayTest test
mvn --% -Dtest=SchemaConvergenceMigrationTest,AgentRunRecorderTest,AiModelGatewayTest,ResourceGenerationControllerTest test
mvn --% -Dtest=MysqlMigrationSmokeTest -Dlearningos.mysql.smoke=true test
mvn test
```

如果本机 3306 被占用，使用：

```powershell
$env:MYSQL_PORT='3307'
mvn --% -Dtest=MysqlMigrationSmokeTest -Dlearningos.mysql.smoke=true -Dlearningos.mysql.smoke.url=jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC test
```
