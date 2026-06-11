# RAG 查询重放与响应快照验收

## 验收结论

通过。

本轮完成 `RAG_QA` 查询重放与响应快照：同一用户使用同一 `requestId` 和同一 RAG payload 重复提交时，返回首次 workflow 和首次 RAG response，不新增 `agent_task`、`agent_trace`、`kb_query_log`、`source_citation`；同一 `requestId` 被不同 payload 复用时返回 `409 CONFLICT`。

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| 首次 RAG 查询保存 `requestId/requestHash/responseJson` | PASS | `RagQueryServiceTest.queryWithRequestIdPersistsRequestHashAndResponseSnapshot` |
| 同 payload RAG 查询 replay 且不重复写 query/citation | PASS | `RagQueryServiceTest.replaysExistingResponseWithSameRequestIdWithoutDuplicatingQueryArtifacts` |
| 不同 payload 复用 `requestId` 返回 409 | PASS | `RagQueryServiceTest.rejectsSameRequestIdWhenPayloadHashDiffers` |
| 无来源 RAG 响应可 replay 且不写 citation | PASS | `RagQueryServiceTest.replaysNoSourceResponseWithoutPersistingCitations` |
| Orchestrator `RAG_QA` replay 返回首次 workflow | PASS | `OrchestratorWorkflowControllerTest.replaysRagQaWorkflowWithSameRequestIdWithoutDuplicatingRows` |
| Orchestrator `RAG_QA` 冲突不创建第二个 workflow | PASS | `OrchestratorWorkflowControllerTest.rejectsRagQaWorkflowWhenSameRequestIdPayloadDiffersBeforeCreatingWorkflowTask` |
| Orchestrator replay 精确匹配 envelope | PASS | `OrchestratorWorkflowControllerTest.replaysRagQaWorkflowByExactEnvelopeInsteadOfFirstRequestIdMarker` |
| RAG workflow envelope 不保存问题原文 | PASS | `createsRagQaWorkflowAndReusesWorkflowTraceContext` |
| 缺失 RAG_QA `requestId` 在创建 task 前失败 | PASS | `rejectsMissingRagQaRequestIdBeforeCreatingWorkflowTask` |
| V7 迁移字段和唯一索引存在 | PASS | `SchemaConvergenceMigrationTest.v7MigrationAddsRagQueryReplaySnapshotColumnsAndConstraint` |

## 测试结果

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

## 未纳入本轮的后续项

- 文档上传 `requestId` 或业务唯一键。
- 文档索引 DB 级并发去重约束或锁。
- 后台恢复任务。
- 真实 MySQL 8 迁移 smoke。
