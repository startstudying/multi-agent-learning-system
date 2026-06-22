# EVIDENCE-20260621 Memory/RAG Privacy Guard

## 1. RED Evidence

| Command | Result | Evidence |
|---|---|---|
| `mvn --% -Dtest=RagQueryServiceTest#durableRagArtifactsDoNotStoreRawQuestionOrFullExcerpt,noSourceReplaySnapshotDoesNotStoreRawQuestion test` | FAIL | 新断言发现 `kb_query_log.question` 仍是 raw question：`Please explain my private exam anxiety and teacher note details.` |
| `mvn --% -Dtest=LearningWorkflowControllerTest#extractsProfileDraftAndGeneratesTraceableLearningPath,ResourceGenerationControllerTest#resourceGenerationSnapshotUsesPersistedLearnerProfile test` | FAIL | 学习路径和资源生成 `profileSnapshot` 仍包含 `learnerId` / `teacher_note`，缺少 `profileRef`。 |

## 2. Implementation Evidence

- 新增 `MemoryPrivacyPolicy`，提供 question hash/length、citation excerpt hash/length、profileRef 和 replay redacted answer 策略。
- `RagQueryService`：
  - `kb_query_log.question` 写入 `questionHash/questionLength`。
  - `kb_query_log.responseJson` 写入可反序列化但降级的 `RagQueryResponse`。
  - `source_citation.excerpt` 写入 citation reference/hash/length。
- `LearningWorkflowService`：
  - `profileSnapshot` 写入 `profileRef`，不再写 raw `learnerId` / `teacher_note`。
  - snapshot sources 过滤 `TEACHER_NOTE`。
- `ResourceGenerationService`：
  - `profileSnapshot` 写入 `profileRef`，不再写 raw `learnerId` / `teacher_note`。
  - `ResourceAgent` 模型上下文复用同一低敏快照。

## 3. GREEN Evidence

| Command | Result |
|---|---|
| `mvn --% -Dtest=RagQueryServiceTest#durableRagArtifactsDoNotStoreRawQuestionOrFullExcerpt,noSourceReplaySnapshotDoesNotStoreRawQuestion test` | PASS, 1 run, 0 failures |
| `mvn --% -Dtest=LearningWorkflowControllerTest#extractsProfileDraftAndGeneratesTraceableLearningPath,ResourceGenerationControllerTest#resourceGenerationSnapshotUsesPersistedLearnerProfile test` | PASS, 2 run, 0 failures |
| `mvn --% -Dtest=RagQueryServiceTest test` | PASS, 22 run, 0 failures, 0 errors |
| `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test` | PASS, 54 run, 0 failures, 0 errors |
| `mvn --% -Dtest=OrchestratorWorkflowControllerTest test` | PASS, 33 run, 0 failures, 0 errors |
| `mvn --% -Dtest=RagQueryServiceTest#durableRagArtifactsDoNotStoreRawQuestionOrFullExcerpt test` | PASS, 1 run, 0 failures after null-safe policy fallback |

## 4. Residual Risk

- 未运行完整 `mvn test`；本切片采用 focused + adjacent backend verification。
- 历史已写入数据库中的 raw question、full excerpt、旧 profileSnapshot 不在本切片清理范围。
- requestId replay 命中时返回降级 answer/source excerpt；调用方若需要完整回答，应使用新 requestId 重新执行查询。
