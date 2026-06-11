# RUN-20260608 RAG Embedding / Vector Adapter Test Plan

## 1. 任务类型 / 技能选择

- 任务类型：RAG / retrieval 测试计划、TDD 回归设计。
- 相关技能：`rag-project-review`、`rag-hybrid-retrieval`、`rag-parser-boundary`、`test-generator`、`database-design`。
- GitHub research：不需要。当前任务是基于现有代码和测试制定计划，不涉及选型或引入依赖。
- 新技能：暂不需要；完成后可沉淀 `rag-embedding-vector-adapter`。

## 2. 关键现状依据

- `EmbeddingService` 只有 `currentModelVersion()`，返回 `noop-embedding-v1`。
- `IndexService` 已写 `embeddingModel` metadata，但未调用 embedding provider，也未写 VectorDB。
- `ChunkService` 当前为 allowed KB 内 `keyword + recency + RRF`，`vectorEnabled=false`、`vectorCandidateCount=0`。
- `RagQueryService` 已把 hybrid/reranker metadata 写入 `kb_query_log.sourcesJson`。
- 当前迁移到 V17。
- 现有测试模式适合继续用 fake service，不接外部服务。

## 3. RED 测试清单

### A. EmbeddingService 单元测试

建议新增 `EmbeddingServiceTest.java`：

1. `returnsDeterministicEmbeddingForSameTextAndModel`
2. `normalizesBlankOrNullTextToValidationError`
3. `returnsConfiguredModelVersionAndDimension`
4. `sanitizesProviderFailureWithoutRawSecretOrPrompt`
5. `doesNotCallExternalProviderWhenEmbeddingDisabled`

### B. Vector adapter 单元测试

建议新增 `VectorIndexAdapterTest.java`：

1. `noopAdapterReportsDisabledAndDoesNotPersistVectors`
2. `inMemoryAdapterUpsertsAndSearchesByCosineSimilarity`
3. `searchFiltersCandidatesByAllowedKnowledgeBaseIds`
4. `upsertFailureIsConvertedToSafeIndexingError`
5. `deleteByDocumentIdRemovesPreviousVectorsBeforeReindex`

### C. IndexService embedding/vector 集成测试

建议新增 `IndexServiceEmbeddingVectorTest.java`：

1. `processIndexTaskEmbedsEachPersistedChunkWhenEmbeddingEnabled`
2. `processIndexTaskWritesEmbeddingMetadataWithoutRawVector`
3. `processIndexTaskUpsertsVectorsAfterChunksAreSaved`
4. `processIndexTaskMarksFailedWithSafeCodeWhenEmbeddingProviderFails`
5. `processIndexTaskContinuesWithoutVectorUpsertWhenVectorAdapterDisabled`
6. `workerPathUsesSameEmbeddingAndVectorIndexingBehaviorAsManualPath`

### D. ChunkService vector branch 测试

建议新增 `ChunkServiceVectorRetrievalTest.java`：

1. `retrieveAllowedChunksIncludesVectorBranchWhenAdapterEnabled`
2. `retrieveAllowedChunksKeepsVectorDisabledMetadataWhenAdapterDisabled`
3. `retrieveAllowedChunksFusesKeywordRecencyAndVectorCandidatesWithRrf`
4. `retrieveAllowedChunksNeverReturnsForbiddenVectorCandidate`
5. `retrieveAllowedChunksFallsBackToKeywordRecencyWhenVectorSearchFails`

### E. RagQueryService 回归测试

修改 `RagQueryServiceTest.java`：

1. `queryPersistsVectorEnabledRetrievalMetadataWhenAdapterReturnsCandidates`
2. `queryFallsBackWhenVectorAdapterTimesOutAndStillPersistsCitations`
3. `queryReturnsNoSourceWhenOnlyForbiddenVectorCandidatesExist`
4. `queryWithRequestIdReplaysVectorMetadataSnapshotWithoutDuplicateVectorSearch`

### F. RrfRanker 回归测试

修改 `RrfRankerTest.java`：

1. `fusesKeywordRecencyAndVectorBranchesByRrfScore`
2. `ignoresNullBranchesAndDuplicateChunksWithinVectorBranch`
3. `usesStableTieBreakWhenScoresAndRanksMatch`

### G. RagProperties 配置测试

新增或扩展配置测试：

1. `defaultsEmbeddingAndVectorAdapterToDisabled`
2. `normalizesInvalidEmbeddingDimensionAndTimeoutDefaults`
3. `bindsVectorAdapterProviderCollectionAndTimeoutProperties`

## 4. 模拟策略

### Embedding provider fake

- deterministic：同 text 同 vector。
- 可控失败：抛出含 secret/raw prompt 的异常，验证只暴露安全码。
- 调用计数：验证 disabled/replay 不调用。

### Vector adapter fake

- `NoopVectorIndexAdapter`：默认 disabled、no-op、空搜索。
- `InMemoryVectorIndexAdapter`：用内存 Map 存 `chunkId/kbId/documentId/vector/metadata`，search 只在 allowed KB 内做 cosine/dot-product。

## 5. DB schema 测试建议

优先不改 MySQL schema；若新增 V18：

- `SchemaConvergenceMigrationTest` 新增 V18 断言。
- `MysqlMigrationSmokeTest` 更新 latest version/count 和 information_schema 检查。
- 实体映射测试保存并读取新增列或新表。

## 6. 验证命令

聚焦：

```powershell
cd backend
mvn --% -Dtest=EmbeddingServiceTest,VectorIndexAdapterTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest,RagPropertiesTest test
```

相邻：

```powershell
cd backend
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest,IndexTaskWorkerSchedulerTest,RagQueryServiceTest,RrfRankerTest test
```

RAG + Orchestrator：

```powershell
cd backend
mvn --% -Dtest=RagQueryServiceTest,DocumentControllerTest,OrchestratorWorkflowControllerTest,IndexServiceTest,IndexTaskWorkerSchedulerTest test
```

全量：

```powershell
cd backend
mvn test
```

如新增 migration：

```powershell
cd backend
mvn --% -Dtest=SchemaConvergenceMigrationTest test
```

## 7. Flaky / 性能风险

- Timeout 测试不要依赖真实 sleep；用 fake adapter 抛 timeout。
- vector/RRF 排序测试设置固定 `createdAt`。
- fake vector adapter 返回列表显式排序，不依赖 Map iteration。
- `@Primary` fake provider 每个测试前 reset。
- 禁止 metadata 存完整向量数组或完整 chunk 内容。
- 默认配置必须 disabled/noop，`mvn test` 不依赖网络、Docker、API key。
- vector 分支必须只使用 allowed KB，且 query service 仍先执行 permission。
