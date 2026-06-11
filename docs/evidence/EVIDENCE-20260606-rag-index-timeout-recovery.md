# RAG 索引任务超时恢复证据

## 范围

本轮完成 P0-3 的最小长任务恢复切片：服务层可以扫描超时 `RUNNING` 索引任务，将其标记为 `FAILED`，记录恢复证据，并让后续同一文档 `reindex` 新建 `PENDING` 任务。

本轮不实现后台定时调度、文档上传 `requestId`、RAG query 重放、数据库级并发去重或真实索引 worker。

## 代码证据

- `backend/src/main/java/com/learningos/rag/domain/KbIndexTask.java`
  - 补齐 `retryCount`、`errorMessage`、`startedAt`、`finishedAt`、`createdAt`、`updatedAt` getter/setter。
- `backend/src/main/java/com/learningos/rag/repository/KbIndexTaskRepository.java`
  - 新增 `findByStatusAndUpdatedAtBefore(...)`。
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
  - 新增 `recoverTimedOutRunningTasks(Instant timeoutCutoff)`。
  - 超时 `RUNNING` 任务恢复为 `FAILED`，递增 `retryCount`，写入 `errorMessage`，设置 `finishedAt`。
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
  - 覆盖超时恢复、非超时保护、非 RUNNING 状态保护。
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
  - 覆盖恢复后 `reindex` 创建新 `PENDING` 任务。

## TDD 过程

### RED

先补测试后运行聚焦测试：

```text
cd backend
mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test
```

结果：失败，符合预期。

关键失败：

```text
Compilation failure
cannot find symbol: method recoverTimedOutRunningTasks(java.time.Instant)
cannot find symbol: method setRetryCount(int)
cannot find symbol: method getRetryCount()
cannot find symbol: method getErrorMessage()
cannot find symbol: method getFinishedAt()
cannot find symbol: method setUpdatedAt(java.time.Instant)
```

说明生产代码缺少本切片要求的恢复方法和索引任务恢复字段访问器。

## 调试记录

实现后第一次聚焦测试时，`DocumentControllerTest` 已通过，但 `IndexServiceTest` 使用 `@DataJpaTest` 触发 Flyway 将 MySQL 风格 `V1__rag_foundation.sql` 跑到 H2，导致启动失败。

根因：新测试未按项目现有 `DataJpaTest` 模式配置 `@ActiveProfiles("test")` 和 `spring.jpa.hibernate.ddl-auto=create-drop`。

处理：参考 `AgentRunRecorderTest`、`PromptVersionServiceTest` 等同类测试，为 `IndexServiceTest` 增加 test profile 和 ddl-auto 配置。

## GREEN：聚焦测试

命令：

```text
cd backend
mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test
```

结果：

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T02:29:11+08:00
```

## GREEN：全量后端测试

命令：

```text
cd backend
mvn test
```

结果：

```text
Tests run: 97, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T02:31:02+08:00
```

## 覆盖范围

- `RUNNING` 且 `updatedAt` 早于阈值的任务恢复为 `FAILED`。
- 恢复时 `retryCount` 增加。
- 恢复时 `errorMessage` 非空且包含超时语义。
- 恢复时 `finishedAt` 非空。
- `RUNNING` 但未超时的任务不变。
- `PENDING/SUCCEEDED/FAILED` 任务不被误修改。
- 超时恢复后，同一文档 `reindex` 创建新的 `PENDING` 任务。
- 既有文档上传、列表、权限、active 去重测试继续通过。

## 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | 恢复逻辑在 `IndexService`，Controller 未新增业务逻辑。 |
| Frontend rules | PASS | 未修改前端。 |
| Agent / RAG rules | PASS | 改动限定 RAG 索引任务状态恢复，不影响引用生成规则。 |
| Security | PASS | 未新增依赖或密钥；reindex 权限仍由 `DocumentService.ensureCanWrite(...)` 控制。 |
| API / Database | PASS | 未修改公开 API；沿用现有 `kb_index_task` 字段，无新增迁移。 |

## 后续限制

- 尚未接入启动扫描或定时恢复调度。
- 真实索引 worker 还没有 heartbeat/progress 更新，后续需定义超时阈值来源。
- 文档上传 `requestId` 和 RAG query 重放仍未完成。
- active 去重仍不是数据库级并发约束。
