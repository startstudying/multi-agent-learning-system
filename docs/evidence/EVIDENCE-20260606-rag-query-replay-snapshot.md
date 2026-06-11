# RAG 查询重放与响应快照证据

## 范围

本轮完成 P0-3 `RAG_QA query replay / response snapshot` 最小切片：

- `kb_query_log` 增加 `request_id`、`request_hash`、`response_json` 和 `(user_id, request_id)` 唯一索引。
- `RagQueryService` 支持带 `requestId` 的查询重放、payload 冲突 409、无来源响应快照。
- Orchestrator `RAG_QA` 在创建 workflow task 前重放首次 workflow，同 payload 不新增 task、trace、query log、citation。
- Orchestrator `RAG_QA` workflow envelope 对问题和 KB 列表脱敏，只保存 hash、长度、计数和 topK。

## 代码证据

- `backend/src/main/resources/db/migration/V7__rag_query_replay_snapshot.sql`
  - 增加 RAG query replay 字段和唯一索引。
- `backend/src/main/java/com/learningos/rag/domain/KbQueryLog.java`
  - 增加 `requestId`、`requestHash`、`responseJson`。
- `backend/src/main/java/com/learningos/rag/repository/KbQueryLogRepository.java`
  - 增加 `findByUserIdAndRequestId(...)`。
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
  - 增加 `queryWithTraceIdAndRequestId(...)`、`queryWithRequestId(...)`、`replayQueryIfPresent(...)`。
  - replay 前重新执行输入安全检查和 KB 权限过滤。
  - 相同 payload 返回首次 `RagQueryResponse` 快照；不同 payload 返回 `CONFLICT`。
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
  - `RAG_QA` 强制 `requestId`。
  - 创建 workflow task 前检查 RAG replay。
  - 精确匹配 workflow envelope，避免只按 `requestId` 字符串误命中。
  - RAG payload 脱敏为 `questionHash/questionLength/kbIdsHash/kbCount/topK`。
- `backend/src/main/java/com/learningos/rag/api/dto/RagQueryDtos.java`
  - `/api/rag/query` 请求体新增可选 `requestId`。

## TDD 过程

### RED

先写测试后运行：

```powershell
cd backend
mvn "-Dtest=SchemaConvergenceMigrationTest,RagQueryServiceTest,OrchestratorWorkflowControllerTest" test
```

失败结果：

```text
Compilation failure:
cannot find symbol
method queryWithTraceIdAndRequestId(...)
BUILD FAILURE
```

说明当前 RAG query replay 服务入口不存在，测试准确暴露缺口。

### GREEN：聚焦测试

```powershell
cd backend
mvn "-Dtest=SchemaConvergenceMigrationTest,RagQueryServiceTest,OrchestratorWorkflowControllerTest" test
```

结果：

```text
Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T11:39:09+08:00
```

### GREEN：交叉回归

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest,AssessmentControllerTest,AssessmentServiceTest" test
```

结果：

```text
Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T11:40:24+08:00
```

### GREEN：全量后端测试

```powershell
cd backend
mvn test
```

结果：

```text
Tests run: 131, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 01:04 min
Finished at: 2026-06-06T11:41:52+08:00
```

## 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | Controller 只转发请求；幂等、权限、快照逻辑在 Service 层。 |
| RAG permission | PASS | replay 前重新执行 KB 权限过滤；无权限仍返回 403。 |
| Agent trace | PASS | Orchestrator replay 不创建第二个 task/trace；首次成功 trace 保持不变。 |
| API / DB | PASS | SPEC 覆盖新增字段和 V7 迁移；请求体新增可选 `requestId` 向后兼容。 |
| Security | PASS | `RAG_QA` workflow envelope 不保存问题原文；冲突错误不回显旧 payload。 |

## 剩余风险

- V7 迁移目前有文本测试和 H2 JPA create-drop 覆盖，仍未做真实 MySQL 8 Flyway smoke。
- 并发同 `requestId` 的唯一键兜底有代码路径，但本轮没有新增并发压力测试。
- 文档上传幂等、索引 DB 级锁、后台恢复任务仍未完成。
