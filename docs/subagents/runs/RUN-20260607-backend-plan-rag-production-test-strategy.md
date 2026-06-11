# RUN-20260607 Backend Plan RAG Production Test Strategy

## 结论摘要

当前 RAG chunk 生产化已完成的是 P3-2 中的“chunk token-ish 切分、40-token overlap、稳定 chunk hash、Markdown heading hierarchy、worker/manual 路径一致性、V17 schema 覆盖”。从测试证据看，这一子切片的单测和 schema smoke 基本充分，能够防住 chunk 规则回退、重复索引 hash 漂移、同正文不同标题被误去重、worker 产物 metadata 不一致等核心回归。

仍未完成且风险最高的是后续生产化检索链路：parser adapter/OCR 仍停留在轻量解析雏形，Embedding/VectorDB 当前是 `noop-embedding-v1` 占位，hybrid/RRF/reranker 当前仍是按 chunk 创建时间取候选并原样返回。下一步测试策略必须优先用 RED 测试锁定“真实解析质量、向量写入一致性、权限过滤不被检索策略绕过、reranker 超时降级仍保留引用和 query log”的行为边界。

## 分析范围与证据

本报告为只读分析，仅允许创建/覆盖本文件。未修改实现代码，未修改 `docs/superpowers/**`。

直接证据：

| 文件 | 证据摘要 |
|---|---|
| `docs/planning/backend-architecture-todolist.md` | P3-2 剩余项为 parser adapter/OCR、Embedding/VectorDB、hybrid retrieval/RRF/reranker timeout fallback；chunk token/overlap/hash/heading 已完成。 |
| `docs/specs/SPEC-20260606-rag-chunk-production-metadata.md` | 定义 V17 `chunk_hash`、`TOKEN_WINDOW_V1`、220 token-ish window、40 overlap、heading metadata、稳定 SHA-256 hash 和验收测试要求。 |
| `docs/evidence/EVIDENCE-20260606-rag-chunk-production-metadata.md` | 记录 `IndexServiceTest`、`IndexTaskWorkerSchedulerTest`、`SchemaConvergenceMigrationTest`、`MysqlMigrationSmokeTest` 的覆盖点和通过结果。 |
| `docs/acceptance/ACCEPT-20260606-rag-chunk-production-metadata.md` | 明确当前只验收 chunk 生产化元数据子项，parser/OCR、Embedding/VectorDB、hybrid/RRF/reranker 仍非目标。 |
| `backend/src/main/java/com/learningos/rag/application/IndexService.java` | 已实现内置 Markdown/TXT/PDF/DOCX 轻量解析、chunk split、metadata、hash；PDF/DOCX 解析仍是简化抽取，不是生产级 adapter/OCR。 |
| `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java` | 当前只返回 `noop-embedding-v1`，未体现真实 embedding 调用、批量写入、重试、维度校验或向量库状态。 |
| `backend/src/main/java/com/learningos/rag/application/ChunkService.java` | 当前候选检索为 `findTop20ByKbIdInOrderByCreatedAtDesc` 后按 `topK` 截断，未体现 keyword/vector/hybrid score。 |
| `backend/src/main/java/com/learningos/rag/application/RerankerService.java` | 当前 `rerankOrFallback` 原样返回候选，未体现 timeout fallback、score 合并或重排失败处理。 |
| `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java` | 已覆盖 active task 锁、timeout recovery、Markdown chunk 清洗、token overlap、stable hash、同文不同 heading、不同行文档基础 chunk、失败重试。 |
| `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java` | 已覆盖 allowed chunk、traceId、requestId replay、NO_SOURCE、profile/history router、权限拒绝、安全输入；尚未覆盖 hybrid/RRF/vector/reranker 行为。 |
| `docs/harness/TEST_COMMANDS.md` | 当前推荐命令包括 `cd backend && mvn test`、单测类、compile、MySQL migration smoke。 |

说明：`git status --short` 在当前工作目录返回 “not a git repository”，因此无法基于 Git 判断未提交改动；本报告按文件系统只读证据形成。

## 当前 Chunk 生产化测试覆盖评价

| 覆盖对象 | 现有测试 | 评价 | 风险 |
|---|---|---|---|
| token-ish window 与 40 overlap | `IndexServiceTest.processMarkdownIndexTaskUsesTokenWindowOverlapStableHashAndHeadingHierarchy` | 强。直接断言单 chunk token 数、相邻 chunk 首尾 40 token 重叠、metadata。 | 低 |
| 稳定 `chunkHash` | `reindexKeepsStableChunkHashesForSameDocumentVersion`、V17 schema 测试 | 强。覆盖重复索引同版本 hash 稳定和 64 hex 格式。 | 低 |
| heading hierarchy | `processMarkdownIndexTaskUsesTokenWindowOverlapStableHashAndHeadingHierarchy`、`workerProducesProductionChunkMetadata` | 中强。Markdown heading path 已覆盖；PDF/DOCX 章节层级仍未覆盖。 | 中 |
| 同正文不同 heading 不误去重 | `sameTextUnderDifferentHeadingsIsNotDeduplicatedAway` | 强。直接覆盖当前 hash 输入包含 headingPath 的关键行为。 | 低 |
| TXT/PDF/DOCX 基础解析兼容 | `processTxtPdfAndDocxIndexTasksProduceSearchableChunks` | 中。覆盖轻量样例，但不是复杂 PDF/DOCX、页码、OCR、表格、乱码、章节层级。 | 高 |
| worker 与手动路径一致 | `IndexTaskWorkerSchedulerTest.workerProducesProductionChunkMetadata` | 中强。覆盖 worker 产物 metadata，但未覆盖 worker 中 parser/OCR 或 embedding/vector 失败路径。 | 中 |
| schema 收敛与 MySQL migration | `SchemaConvergenceMigrationTest`、`MysqlMigrationSmokeTest` | 强。V17 文本和真实 MySQL smoke 已覆盖。 | 低 |
| metadata 安全边界 | evidence/acceptance 记录，测试断言 metadata 包含关键字段 | 中。正向字段有覆盖；未见对“不包含原文片段、storage key、provider error”的专门负向断言。 | 中 |

总体判断：chunk 规则层面的回归覆盖健康；生产级解析与检索质量层面的覆盖尚未开始。

## 推荐测试矩阵

| 切片 | 单测 | 集成测 | MySQL smoke | 端到端测 |
|---|---|---|---|---|
| parser adapter/OCR | parser adapter contract：按 content type/扩展名选择 adapter；PDF 多页页码；DOCX heading；空文档失败；OCR fallback 被触发且 error sanitized；metadata 不含原文/密钥。 | `IndexService` + fake storage + fake parser/OCR：解析失败重试、页码/section 写入 chunk、worker 路径一致。 | 若新增 parser/OCR 结果字段、page/section 字段、任务状态字段，必须覆盖 migration。 | 上传复杂 PDF/DOCX/扫描件样例，等待 index task 完成，查询文档详情和 RAG sources 中 page/section。 |
| Embedding/VectorDB | `EmbeddingService` contract：批量维度校验、空文本跳过、provider error 分类、model version 写入、重试/超时不泄露原始错误；VectorDB adapter upsert/delete/query contract。 | `IndexService` + fake embedding + fake vector store：chunk 保存和 vector upsert 一致；重索引先删除旧向量或按 version 隔离；embedding 失败任务状态可恢复。 | 若新增 `embedding_status`、`vector_id`、`embedding_model`、`vector_index_task` 等表/列，跑 Flyway smoke 并断言索引/唯一键。 | 上传文档后执行 RAG query，断言命中来自向量候选且 citations 与 chunk/document 对齐；禁用 VectorDB 时降级 NO_SOURCE 或 keyword fallback 可解释。 |
| hybrid retrieval/RRF | RRF score 合并纯函数：keyword/vector 排名融合、重复 chunk 合并、权限过滤前置或后置一致、topK 稳定排序、score cutoff。 | `RagQueryService` + seeded chunks/vector hits：keyword-only、vector-only、hybrid 命中，query log 记录 candidate/final/source 数和 strategy。 | 若新增 query log score/routing/vector columns，覆盖 migration。 | 教师上传课程材料，学生提问同义问题，断言 sources 来自授权课程且 NO_SOURCE 不编造引用。 |
| reranker timeout fallback | `RerankerService`：成功重排、超时 fallback、异常 fallback、空候选、score 保留、耗时记录。 | `RagQueryService` 注入 fake slow reranker：超时后返回 fallback sources，query log 标记 downgraded/rerankerStatus，不丢 traceId。 | 若新增 reranker latency/status 字段，覆盖 migration。 | Reranker 服务不可用或超时场景下，RAG API 仍返回可解释 fallback 或 NO_SOURCE，并保留 trace/citation。 |
| 权限与引用回归 | 权限 filter 单测：mixed allowed/forbidden、public/private、teacher/student/admin。 | 检索链路集成测：vector hit 中含 forbidden chunk 时必须剔除，source citation 不写 forbidden 文档。 | 无 schema 变更时不需要。 | 跨用户/跨课程文档检索渗透：请求混合 `kbIds`、只返回允许 sources。 |
| 质量评估回归 | RAG evaluation metric 单测保持 Recall@K、Citation Accuracy、Groundedness、No-source Refusal Rate。 | 用 hybrid/vector fake runner 固化小 benchmark，比较不同策略输出指标。 | 无 schema 变更时不需要。 | 周期性 archive runner 生成报告，作为检索质量回归基线。 |

## 测试分层建议

单测应覆盖：

- 纯算法和策略：chunk split、heading path、hash、RRF、score cutoff、reranker fallback、query routing、metadata sanitizer。
- adapter contract：parser、embedding provider、VectorDB adapter、reranker client 的输入输出、异常分类、超时。
- 不触 DB 的安全断言：metadata 不包含 raw text、storage key、provider error、API key。

集成测应覆盖：

- `IndexService` 从 storage read 到 parse、chunk、embedding、vector upsert、task status 的事务行为。
- `IndexTaskWorkerScheduler` 的 worker/manual 路径一致性、失败重试、lease/heartbeat、单任务失败不阻断批次。
- `RagQueryService` 的 permission filter、hybrid 候选、reranker fallback、query log、source citation 持久化。
- Orchestrator `RAG_QA` 与 RAG query/citation/traceId 对齐。

MySQL smoke 应覆盖：

- 所有新增 migration 从空库执行成功。
- 新增列、唯一键、查询索引真实存在。
- MySQL 方言特有字段长度、JSON/text、向量元数据字段不会触发 row-size 或索引长度问题。
- 若引入 VectorDB 不落 MySQL，也至少 smoke MySQL 侧任务表、vector external id、status、error 字段。

端到端测应覆盖：

- 上传文档 -> index worker 完成 -> RAG query -> 返回 answer/sources/retrieval metadata -> query log/source citation/trace 可查。
- 复杂 PDF/DOCX/扫描件样例的 page/section citation 可见。
- 无来源时返回 `NO_SOURCE_REFUSAL`，不写 citation。
- Reranker/VectorDB 不可用时系统可解释降级。
- 跨用户/跨课程检索不泄露 forbidden sources。

## Context Pack 建议写入的测试命令

基础命令：

```powershell
cd backend
mvn test
```

当前 chunk/worker/schema 回归聚焦命令：

```powershell
cd backend
mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test
```

RAG query、权限、评估回归命令：

```powershell
cd backend
mvn --% -Dtest=RagQueryServiceTest,PermissionServiceTest,RagEvaluationServiceTest,RagEvaluationArchiveReportTest,RagEvaluationControllerTest test
```

Orchestrator RAG trace/citation 对齐回归命令：

```powershell
cd backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest test
```

真实 MySQL migration smoke：

```powershell
$env:MYSQL_PORT='3307'
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
```

后续新增 Embedding/VectorDB/hybrid/reranker 后，Context Pack 还应加入对应聚焦类名，例如：

```powershell
cd backend
mvn --% -Dtest=EmbeddingServiceTest,VectorStoreAdapterTest,HybridRetrievalServiceTest,RrfRankerTest,RerankerServiceTest test
```

如果引入外部 provider 或容器化 VectorDB，应把 provider 调用默认 mock 化；真实 provider/VectorDB smoke 使用显式 profile 或脚本，不应进入默认 `mvn test`。

## 风险最高的缺口

1. 复杂 PDF/DOCX/OCR 解析质量缺口：当前 `IndexService` 的 PDF/DOCX 解析是轻量字符串/regex 提取，无法证明真实页码、章节层级、表格、扫描件 OCR fallback 正确。风险是 citations 指向错误页码或 chunk 为空但任务误判成功。
2. Embedding/VectorDB 一致性缺口：当前 `EmbeddingService` 仅返回 `noop-embedding-v1`，没有向量维度、批量 upsert、删除旧向量、重索引 version 隔离、provider 超时失败的测试。风险是 DB chunk 已成功但向量索引缺失，查询质量静默下降。
3. 检索策略缺口：当前 `ChunkService` 按创建时间取 chunk，`RerankerService` 原样返回，无法防住 hybrid/RRF 实现后权限过滤、score 合并、topK 稳定性和 fallback 的回归。风险是检索看似返回 sources，但不是语义相关 sources。
4. 权限与 VectorDB 交叉缺口：已有 strict `kbIds` 测试，但未来 vector hit 可能从外部索引返回 forbidden chunk。必须测试 forbidden vector hits 被 service 层剔除且不写 source citation。
5. metadata 负向安全缺口：当前覆盖重点是包含必要字段，仍需要明确断言 metadata、query log、provider error 不保存 raw overlap、storage key、OCR 原始错误或密钥。
6. 质量回归缺口：已有 RAG evaluation 指标服务，但尚未绑定真实/模拟 hybrid retrieval pipeline 做固定 benchmark。风险是上线 hybrid 后 Recall@K 或 Citation Accuracy 下降但单测仍通过。

## 下一步切片的最小 RED/GREEN 路径

### 切片 A：parser adapter/OCR

RED：

- 新增 `DocumentParserAdapterTest`：`selectsPdfAdapterAndReturnsPageAwareSections`，断言 PDF adapter 返回 `pageNum`、`sectionTitle`、`headingPath`。
- 新增 `IndexServiceTest`：`usesOcrFallbackWhenPdfTextExtractionIsEmpty`，断言扫描 PDF 空文本时调用 OCR fallback，chunk metadata 标记 `parser=PDF`、`ocrFallback=true`，任务成功。
- 新增负向测试：`failsIndexTaskWithSanitizedErrorWhenParserFails`，断言 error message 为稳定 code，不包含 provider 原始错误。

GREEN：

- 抽出 parser adapter 接口，先用 fake/test adapter 满足 contract。
- 保留当前内置解析作为 default adapter，OCR 先以可注入 fake fallback 通过测试。
- 不在此切片引入真实 OCR 依赖；如必须引入，先完成 dependency review。

### 切片 B：Embedding/VectorDB

RED：

- `EmbeddingServiceTest.embedsChunksInBatchesAndRejectsDimensionMismatch`。
- `VectorStoreAdapterTest.upsertsChunkVectorsWithDocumentVersionAndDeletesPreviousVersionVectors`。
- `IndexServiceTest.marksTaskRecoverableWhenEmbeddingProviderFailsWithoutSavingPartialVectorState`。

GREEN：

- 定义 `EmbeddingProvider` 与 `VectorStoreAdapter` contract，默认 fake/noop adapter 用于测试。
- `IndexService` 在 chunk 保存后执行 embedding/vector upsert，并在失败时写入可恢复状态。
- 新增 MySQL 字段时同步 migration 和 smoke 断言。

### 切片 C：hybrid/RRF/reranker

RED：

- `RrfRankerTest.mergesKeywordAndVectorRankingsWithStableScores`。
- `HybridRetrievalServiceTest.filtersForbiddenVectorHitsBeforeRrfMerge`。
- `RerankerServiceTest.returnsFallbackOrderWhenRerankerTimesOut`。
- `RagQueryServiceTest.persistsDowngradedRetrievalMetadataWhenRerankerFallsBack`。

GREEN：

- 先实现纯 Java RRF ranker 和 fake retrievers。
- 将 permission filter 固定在 service 层，确保 keyword/vector 两路结果都经过 allowed chunk 校验。
- reranker timeout 使用配置项 `rag.rerankerTimeoutMs`，失败时返回 fallback candidates 并写 query log 降级状态。

## 明显遗漏的回归场景

| 场景 | 推荐层级 | 说明 |
|---|---|---|
| Markdown 空 heading、跳级 heading、重复 heading path | 单测 | 防止 heading hierarchy 规则在复杂文档下漂移。 |
| PDF 多页同名章节 citation pageNum | 集成测/端到端 | 当前 PDF pageNum 只简化为 1，后续 adapter 必须防错页引用。 |
| DOCX 表格、列表、标题样式 | adapter 单测/集成测 | 真实课程资料常见，不能只测 `word/document.xml` 简单文本。 |
| OCR fallback 超时、低置信度、空结果 | 单测/集成测 | 应区分 `FAILED`、可重试、NO_SOURCE，而不是成功写空 chunk。 |
| 重索引删除旧 chunks 后 vector 旧数据残留 | 集成测 | 防止 query 命中过期文档版本。 |
| Embedding provider 部分批次失败 | 集成测 | 防止半成功状态被标为 `INDEXED`。 |
| VectorDB 返回 forbidden chunk id | 集成测/端到端 | 最高优先级安全回归。 |
| keyword 与 vector 命中同一 chunk | 单测 | RRF 合并应去重并保留最高/融合分数。 |
| reranker 返回不存在或重复 chunk id | 单测 | service 应忽略非法 id，保持 fallback 完整性。 |
| reranker 慢响应超过 timeout | 单测/集成测 | 不应阻塞 RAG_QA 工作流或丢 trace。 |
| topK 为 0、负数、超大数 | 单测/集成测 | 当前已有默认限制思路，hybrid 后需要统一上限。 |
| query log/sourcesJson 截断后仍可解析关键 metadata | 集成测 | 防止长 sources 破坏审计字段。 |
| RAG evaluation benchmark 对 hybrid 策略的 Recall@K 下降 | 集成测/离线评估 | 需要固定小样本回归基线。 |

## 测试健康判断

**Coverage**：chunk 子切片约 80% 行为覆盖；完整 RAG 生产化约 35%-45% 行为覆盖。

**Test Health**：NEEDS ATTENTION。

原因：当前 chunk 生产化子项健康，但剩余 P3-2 生产链路尚处于占位或轻量实现，测试矩阵必须随下一步切片先行补齐。最小可接受标准是每个新切片先提交 RED 测试，默认 `mvn test` 不依赖外部 provider，真实 MySQL/VectorDB/provider smoke 显式 opt-in。
