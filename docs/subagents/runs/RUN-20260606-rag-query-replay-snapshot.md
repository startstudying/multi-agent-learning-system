# Subagent Run: RAG 查询重放与响应快照

## Subagent Decision

Use Subagents: Yes。

Reason：用户要求多 subagent 并行开发，且本切片涉及 RAG、Orchestrator、数据库、权限、隐私和测试。

Parallelism Level：L1。

Implementation Mode：Single Codex implementation with parallel analysis。

## 已收敛结论

### 架构审查

结论：

- replay 根应放在 `RagQueryService`，以 `userId + requestId` 查快照。
- `kb_query_log` 需要 `requestId/requestHash/responseJson` 和唯一索引。
- Orchestrator 只负责传入 requestId，并在命中旧 trace 时复用旧 workflow。
- replay 返回首次 traceId，不复制 citation。

本轮处理：

- 新增 `queryWithTraceIdAndRequestId(...)`、`queryWithRequestId(...)`、`replayQueryIfPresent(...)`。
- 新增 V7 迁移和实体字段。
- Orchestrator `RAG_QA` 创建前 replay 首次 workflow。

### 测试审查

结论：

- 必须断言表计数不变，不能只看 answer 相同。
- 覆盖服务层 snapshot/replay/conflict/no-source。
- 覆盖 Orchestrator replay/conflict/exact envelope。
- 覆盖 V7 migration 文本。

本轮处理：

- 新增 4 个 `RagQueryServiceTest` 行为测试。
- 新增 4 个 `OrchestratorWorkflowControllerTest` 行为测试，并加固现有 RAG_QA envelope 脱敏断言。
- 新增 `SchemaConvergenceMigrationTest.v7MigrationAddsRagQueryReplaySnapshotColumnsAndConstraint`。

### 安全 / 隐私审查

结论：

- replay 必须限定当前用户，跨用户 requestId 不互通。
- replay 前应重新验证 KB 权限。
- payload hash 不应包含 traceId/workflowId 等运行时数据。
- workflow envelope 不应保存 RAG 问题原文。

本轮处理：

- `RagQueryService.replayQueryIfPresent(...)` 在 replay 前重新执行输入安全检查和 KB 权限过滤。
- `RAG_QA` workflow payload 改为 hash/长度/计数。
- `requestId` 对 `RAG_QA` 变为必填，缺失时在创建 task 前返回 400。

## 集成决策

- 本轮完成 P0-3 RAG query replay / response snapshot。
- 不把文档上传幂等、索引 DB 锁、后台恢复任务并入本切片。
- 不新增外部依赖。

## 验证结果

```text
mvn "-Dtest=SchemaConvergenceMigrationTest,RagQueryServiceTest,OrchestratorWorkflowControllerTest" test
Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
```

```text
mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest,AssessmentControllerTest,AssessmentServiceTest" test
Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
```

```text
mvn test
Tests run: 131, Failures: 0, Errors: 0, Skipped: 0
```
