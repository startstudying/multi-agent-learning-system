# RAG 索引恢复调度与并发锁 Evidence

## 变更范围

- `IndexService.createPendingTask(...)` 增加文档行 `PESSIMISTIC_WRITE` 锁后再执行 active task 查询与创建。
- 新增 `IndexTaskRecoveryScheduler`，启动后和定时调用既有 `recoverTimedOutRunningTasks(...)`。
- 新增 `IndexRecoveryProperties`，提供恢复开关、启动恢复、超时阈值和调度间隔默认值。
- 新增服务层并发测试和 scheduler 调用测试。

## 测试命令与结果

### 1. 新增服务和调度测试

```powershell
cd backend
mvn "-Dtest=IndexServiceTest,IndexTaskRecoverySchedulerTest" test
```

首次结果：

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T12:09:32+08:00
```

覆盖：

- `KbDocumentRepository.findByIdAndDeletedAtIsNullForUpdate(...)` 标注 `@Lock(PESSIMISTIC_WRITE)`。
- 两个并发 reindex 入口同时调用 `createPendingTask(...)` 时返回同一个 task。
- 超时 `RUNNING` 恢复为 `FAILED` 的既有行为保持。
- scheduler 启动恢复、定时恢复、disabled 跳过、关闭启动恢复。

### 2. RAG 文档 API + 服务 + 调度聚焦测试

```powershell
cd backend
mvn "-Dtest=IndexServiceTest,DocumentControllerTest,IndexTaskRecoverySchedulerTest" test
```

结果：

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T12:10:31+08:00
```

覆盖：

- 文档上传创建 `PENDING` index task。
- reindex 复用 `PENDING/RUNNING` active task。
- `FAILED/SUCCEEDED` 后 reindex 新建 `PENDING` task。
- 超时恢复后 reindex 新建任务。
- 文档列表和公开 KB 读写权限不回退。

### 3. 用户指定最小命令

```powershell
cd backend
mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test
```

结果：

```text
BUILD FAILURE
OrchestratorWorkflowService.java:[500,16] OrchestratorWorkflowResponse constructor cannot be applied
required: ..., List<String>, String
found:    ..., List<String>
```

说明：

- 失败发生在 Maven `compile` 阶段，测试未开始执行。
- 编译错误位于 `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java` 与 `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowResponse.java` 的构造器参数不一致。
- 这两个文件不在本 Worker 的允许修改范围内；本切片未修改 orchestrator 代码。
- 在该外部变更出现前，包含 `DocumentControllerTest` 的 RAG 聚焦命令已通过。

集成修复后复测：

```powershell
cd backend
mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test
```

结果：

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T12:21:44+08:00
```

### 4. 集成聚焦验证

```powershell
cd backend
mvn "-Dtest=SchemaConvergenceMigrationTest,DocumentControllerTest,IndexServiceTest,IndexTaskRecoverySchedulerTest" test
```

结果：

```text
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T12:18:47+08:00
```

## 架构漂移检查

| Check | Status | Evidence |
|---|---|---|
| Backend layering | PASS | Scheduler 只调用 `IndexService`；业务规则仍在 Service。 |
| Frontend rules | PASS | 未修改前端。 |
| Agent / RAG rules | PASS | RAG 索引任务 active 去重和超时恢复更稳定。 |
| Security | PASS | 未新增依赖、密钥或公开 API。 |
| API / Database | PASS | API DTO 和数据库 schema 不变。 |

## 限制

- H2 测试能证明应用层使用 `for update` 和并发结果收敛；真实 MySQL 锁行为仍建议在 P3 MySQL smoke 中验证。
- 当前只标记超时任务为 `FAILED`，不自动重新入队。
