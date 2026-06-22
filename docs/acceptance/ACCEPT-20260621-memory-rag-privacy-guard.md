# ACCEPT-20260621 Memory/RAG Privacy Guard

## Verdict

PASS。

## Acceptance Matrix

| Criteria | Verdict | Evidence |
|---|---|---|
| 新写入 RAG query log 不包含 raw question | PASS | `RagQueryServiceTest.durableRagArtifactsDoNotStoreRawQuestionOrFullExcerpt` |
| 新写入 replay snapshot 不包含 raw question / full excerpt | PASS | `RagQueryServiceTest.durableRagArtifactsDoNotStoreRawQuestionOrFullExcerpt`、`noSourceReplaySnapshotDoesNotStoreRawQuestion` |
| 新写入 source citation 不包含 full excerpt | PASS | `RagQueryServiceTest.durableRagArtifactsDoNotStoreRawQuestionOrFullExcerpt` |
| `profileSnapshot` 使用 `profileRef + 必要低敏字段` | PASS | `LearningWorkflowControllerTest`、`ResourceGenerationControllerTest` |
| `teacher_note` 不进入 ResourceAgent AI context | PASS | `ResourceGenerationControllerTest.resourceGenerationSnapshotUsesPersistedLearnerProfile` 验证 `ModelRequest.context.profileSnapshot` |
| RAG replay 降级策略已记录 | PASS | `SPEC-20260621-memory-rag-privacy-guard.md`、本 ACCEPT、Evidence |

## Notes

- API DTO 和 DB schema 未改变；改变的是 durable field content policy。
- 历史数据清理、TTL、retention sweep 留给后续 memory lifecycle governance。
