# EVIDENCE-20260621 Memory lifecycle governance MVP

## 1. 验证范围

本证据覆盖 P2 MVP：

- V23 memory lifecycle migration 文本断言。
- `KbChatSession` / `KbChatMessage` lifecycle 字段与 repository 查询。
- `MemoryLifecycleService` 的低敏 QA summary、salience、decay、用户编辑、软删除、列表映射。
- `AiQaService` 成功回答后写入低敏 session memory。
- `MemoryContextService` 优先注入未删除、未过期 session memory，缺省时回退 `learning_event`。
- AI QA 相邻回归与 backend compile。

## 2. RED 证据

首次 P2 focused 运行：

```powershell
mvn --% -Dtest=MemoryLifecycleServiceTest,MemoryContextServiceTest,SchemaConvergenceMigrationTest test
```

结果：

- Build FAILURE。
- 失败原因符合预期：缺少 `KbChatMessageRepository`、`MemoryLifecycleService`、`KbChatMessage` lifecycle getter/setter、`MemoryContextService` 新 constructor dependency，以及 V23 migration 文件。

## 3. GREEN / Focused

命令：

```powershell
mvn --% -Dtest=MemoryLifecycleServiceTest,MemoryContextServiceTest,SchemaConvergenceMigrationTest test
```

结果：

- Build SUCCESS。
- Tests run: 26。
- Failures: 0。
- Errors: 0。
- Skipped: 0。

## 4. Adjacent

命令：

```powershell
mvn --% -Dtest=AiQaControllerTest,QaRuntimeTest test
```

结果：

- Build SUCCESS。
- Tests run: 7。
- Failures: 0。
- Errors: 0。
- Skipped: 0。

## 5. Compile

命令：

```powershell
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```

结果：

- Build SUCCESS。

## 6. Privacy grep

命令：

```powershell
rg -n "teacher_note|provider_key|api_key|sk-secret|why does SQL JOIN duplicate rows|JOIN duplicates usually come from" backend/src/main/java/com/learningos/aiqa backend/src/main/java/com/learningos/rag/domain backend/src/main/java/com/learningos/rag/repository backend/src/main/resources/db/migration/V23__memory_lifecycle_governance.sql
```

结果：

- 生产命中仅出现在敏感标记过滤/校验规则：
  - `MemoryContextService`
  - `MemoryLifecycleService`
  - `AnswerVerifier`
- 未发现测试 raw question / full answer 样例进入生产持久化实现。

## 7. 未运行项

- 未运行真实 MySQL 外部 smoke；`MysqlMigrationSmokeTest` 是 opt-in，需要显式 `-Dlearningos.mysql.smoke=true` 和可用 MySQL 8 环境。
- 本次已更新 smoke 的 latest version/count 与 V23 schema assertions，但没有声明真实 MySQL smoke 通过。
