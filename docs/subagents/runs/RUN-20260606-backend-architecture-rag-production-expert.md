# RUN-20260606 后端架构 RAG 生产化 Agent/RAG Expert 分析报告

## 分析范围

本报告只做代码库探索和实现现状分析，不修改业务代码。聚焦 `docs/planning/backend-architecture-todolist.md` 中：

- P2-2：定期 RAG 评估脚本 / 可归档报告。
- P3-2：RAG 索引生产化，包括解析、chunk 清洗、embedding adapter、hybrid retrieval/RRF/reranker fallback、索引任务进度/恢复。

已读取指定上下文：

- `AGENTS.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/specs/SPEC-20260606-rag-quality-evaluation.md`
- `docs/planning/backend-architecture-todolist.md`

检索时已按要求排除 `frontend/node_modules` 和 `backend/target`。

## 结论摘要

1. P2-2 的 TODO 状态存在文档滞后：`backend-architecture-todolist.md` 中“增加定期评估脚本或测试，输出可归档报告”仍未勾选，但仓库已有 `scripts/run-rag-evaluation-archive.ps1`、`RagEvaluationArchiveReportCli` 和 `RagEvaluationArchiveReportTest`，且 acceptance/evidence 文档也记录了可归档报告能力。
2. P3-2 的 TODO 状态也存在部分滞后：`IndexService` 已经具备最小 parser/chunker/index processor，覆盖 Markdown/TXT/PDF/DOCX 的轻量解析、清洗、去重、切块、metadata 写入，并有测试。但这仍不是生产级解析管线，尤其 PDF/DOCX 解析能力过轻，缺少可替换 parser adapter、OCR、页码/章节精确识别和 worker 进度模型。
3. P3-2 的核心生产化缺口仍然明确：`EmbeddingService` 是 `noop-embedding-v1`，没有 embedding 向量生成或 VectorDB adapter；`ChunkService` 只是按 `createdAt` 倒序取 chunk；`RerankerService` 原样返回，没有 hybrid retrieval、RRF、reranker timeout fallback 的真实实现。
4. 索引任务恢复已有基础：active task 去重、文档行 `PESSIMISTIC_WRITE` 锁、超时 `RUNNING` 恢复为 `FAILED`、启动/定时恢复 scheduler 已实现。但缺少进度百分比、阶段级进度事件、worker heartbeat、自动重试/重新入队策略，以及面向 API 的 index task detail/progress 查询。

## 1. 当前 RAG 模块和 evaluation 模块现状图谱

### 1.1 RAG 文档与索引链路

证据：

- `backend/src/main/java/com/learningos/rag/api/DocumentController.java:29` 提供 `POST /api/knowledge-bases/{kbId}/documents` 上传入口；`DocumentController.java:62` 提供 `POST /api/documents/{documentId}/reindex`。
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:55` 上传时校验 KB、写权限、可选 `requestId`，然后调用 `createDocumentUpload(...)`。
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:103` 创建 `KbDocument`，`DocumentService.java:119` 调用 `IndexService.createPendingTask(saved)` 生成索引任务。
- `backend/src/main/java/com/learningos/rag/application/IndexService.java:71` 的 `createPendingTask(...)` 先锁定文档，再复用最新 `PENDING/RUNNING` active task；`IndexService.java:90` 的 `processIndexTask(...)` 执行实际解析、切块和状态更新。
- `backend/src/main/java/com/learningos/rag/domain/KbIndexTask.java:23` 到 `KbIndexTask.java:47` 已有 `documentId`、`kbId`、`status`、`retryCount`、`errorMessage`、`startedAt`、`finishedAt`、`createdAt`、`updatedAt`。
- `backend/src/main/java/com/learningos/rag/domain/KbDocument.java:64` 到 `KbDocument.java:73` 已有 `parseStatus`、`indexStatus` 和 `errorMessage`。
- `backend/src/main/java/com/learningos/rag/domain/KbDocChunk.java:22` 到 `KbDocChunk.java:46` 已有 `kbId`、`documentId`、`documentVersion`、`chunkIndex`、`content`、`pageNum`、`sectionTitle`、`metadataJson`。
- `backend/src/main/resources/db/migration/V1__rag_foundation.sql:25` 到 `V1__rag_foundation.sql:76` 创建了 `kb_document`、`kb_doc_chunk`、`kb_index_task`；`V1__rag_foundation.sql:102` 到 `V1__rag_foundation.sql:106` 增加了 RAG 基础索引。

推断：

- 当前索引链路已经从“只创建 pending task”演进到“可同步处理 task 并产出 chunks”的最小实现。
- 该处理入口目前是服务方法 `IndexService.processIndexTask(String)`，代码库未发现生产 worker、队列消费者或外部调度入口调用它；因此生产环境仍缺异步索引执行器。

### 1.2 Parser / chunker / embedding 现状

证据：

- `backend/src/main/java/com/learningos/rag/application/IndexService.java:38` 定义 `MAX_CHUNK_LENGTH = 1000`；`IndexService.java:39` 到 `IndexService.java:41` 定义 Markdown heading、DOCX text、PDF text object 正则。
- `backend/src/main/java/com/learningos/rag/application/IndexService.java:149` 到 `IndexService.java:156` 根据文档类型选择 Markdown/TXT/PDF/DOCX 解析。
- `backend/src/main/java/com/learningos/rag/application/IndexService.java:159` 到 `IndexService.java:174` Markdown 按 heading 建 section。
- `backend/src/main/java/com/learningos/rag/application/IndexService.java:192` 到 `IndexService.java:216` 进行固定长度切块、空白清洗和完全相同内容去重。
- `backend/src/main/java/com/learningos/rag/application/IndexService.java:218` 到 `IndexService.java:233` 将 chunk 写入 `KbDocChunk`，保留 `pageNum`、`sectionTitle`、`documentVersion`、`metadataJson`。
- `backend/src/main/java/com/learningos/rag/application/IndexService.java:236` 到 `IndexService.java:245` metadata 中写入 `parser`、`embeddingModel`、`contentLength`。
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java:5` 到 `EmbeddingService.java:10` 只返回固定字符串 `noop-embedding-v1`。
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java:165` 到 `IndexServiceTest.java:203` 覆盖 Markdown 清洗、切块、section title、metadata。
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java:205` 到 `IndexServiceTest.java:224` 覆盖 TXT/PDF/DOCX 可产出 searchable chunks。

推断：

- TODO 中“增加 PDF、DOCX、Markdown、TXT 解析器”和“chunk 清洗、去重、切分策略、页码和章节识别”已有最小可执行雏形。
- 但当前 PDF 解析只是读取 text object 或原始 bytes，DOCX 只取 `word/document.xml` 的 `<w:t>`，不能覆盖复杂 PDF、扫描 PDF、表格、脚注、标题层级、分页、图片 OCR 或文档预览校验，因此不应视为生产级完成。

### 1.3 RAG 查询、引用和权限链路

证据：

- `backend/src/main/java/com/learningos/rag/api/ChatController.java:36` 提供 `POST /api/rag/query`，支持 optional `requestId`；`ChatController.java:51` 提供 SSE stream。
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:147` 到 `RagQueryService.java:157` 在查询前执行 content safety 和 `permissionService.filterAllowedKbIds(...)`。
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:182` 调用 `ChunkService.retrieveAllowedChunks(...)`，`RagQueryService.java:183` 调用 `RerankerService.rerankOrFallback(...)`。
- `backend/src/main/java/com/learningos/rag/application/ChunkService.java:18` 到 `ChunkService.java:25` 只从 `KbDocChunkRepository.findTop20ByKbIdInOrderByCreatedAtDesc(...)` 取允许 KB 的最新 chunks。
- `backend/src/main/java/com/learningos/rag/application/RerankerService.java:8` 到 `RerankerService.java:13` 原样返回输入 chunks。
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:191` 到 `RagQueryService.java:195` 返回 `answer/sources/traceId/retrieval`，写 `kb_query_log`，有 sources 时写 `source_citation`。
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:228` 到 `RagQueryService.java:244` 将 citation 按 `traceId` 持久化。
- `backend/src/main/java/com/learningos/rag/domain/SourceCitationRecord.java:19` 到 `SourceCitationRecord.java:35` 持久化 `traceId`、`documentId`、`documentName`、`pageNum`、`sectionTitle`、`excerpt`、`score`。
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java:50` 到 `PermissionService.java:63` 对 requested `kbIds` 做服务层 permission filter。
- `docs/architecture/rag-architecture.md:50` 到 `docs/architecture/rag-architecture.md:64` 将 hybrid retrieval、RRF、reranker timeout fallback 标为 online query pipeline 的 optional/待硬化能力。

推断：

- Permission 和 citation 的基础链路已具备，且 retrieval 在读取 chunk 前使用 allowed KB ids。
- 当前 retrieval 不是语义检索，也不是 lexical scoring，而是“允许 KB 下最新 chunk 优先”的确定性占位实现；因此 P3-2 的 hybrid retrieval/RRF/reranker fallback 实质仍未完成。

### 1.4 RAG evaluation 现状

证据：

- `backend/src/main/java/com/learningos/rag/api/RagEvaluationController.java:12` 到 `RagEvaluationController.java:25` 提供 `POST /api/rag/evaluations`。
- `backend/src/main/java/com/learningos/rag/application/RagEvaluationRequest.java:7` 到 `RagEvaluationRequest.java:55` 支持 legacy input 和 `benchmark`，benchmark sample 包含 `sampleKey`、`question`、`expectedChunkIds`、`expectedAnswer`、`forbiddenAnswerScope`、`expectedNoSource`、`actualAnswer`、`actualCitations`、`topK`。
- `backend/src/main/java/com/learningos/rag/application/RagEvaluationService.java:56` 到 `RagEvaluationService.java:148` 计算 benchmark 级 `recallAtK`、`citationAccuracy`、`groundedness`、`noSourceRefusalRate`、`benchmarkSummary`、`sampleResults` 和 `report`。
- `backend/src/main/java/com/learningos/rag/application/RagEvaluationService.java:151` 到 `RagEvaluationService.java:160` 判断 no-source refusal。
- `backend/src/test/java/com/learningos/rag/application/RagEvaluationServiceTest.java:57` 到 `RagEvaluationServiceTest.java:153` 覆盖课程 benchmark、no-source refusal 和 report。
- `backend/src/test/java/com/learningos/rag/api/RagEvaluationControllerTest.java:78` 到 `RagEvaluationControllerTest.java:167` 覆盖 API 返回 benchmark summary、sample results 和 archivable report。
- `scripts/run-rag-evaluation-archive.ps1:18` 到 `scripts/run-rag-evaluation-archive.ps1:64` 构建 classpath、编译相关源码并运行 `RagEvaluationArchiveReportCli`。
- `backend/src/test/java/com/learningos/rag/application/RagEvaluationArchiveReportCli.java:27` 到 `RagEvaluationArchiveReportCli.java:69` 写出 `rag-quality-evaluation-report.md`，内容包含 benchmark metadata、metrics、samples 和 service report。
- `backend/src/test/java/com/learningos/rag/application/RagEvaluationArchiveReportTest.java:20` 到 `RagEvaluationArchiveReportTest.java:42` 验证可重复生成归档报告；`RagEvaluationArchiveReportTest.java:44` 到 `RagEvaluationArchiveReportTest.java:57` 验证脚本契约。
- `docs/acceptance/ACCEPT-20260606-rag-quality-evaluation.md:23` 到 `ACCEPT-20260606-rag-quality-evaluation.md:26` 明确记录脚本、输出路径和合约测试均已完成。

推断：

- P2-2 最后一项“脚本或测试 + 可归档报告”已有直接实现证据，TODO 未勾选应视作文档状态滞后。
- 该脚本是手动/本地可执行 fixture archive，不是从 `evaluation_set` 拉取 active benchmark、不调用真实 RAG query、不记录到 `evaluation_run` 的周期性评估执行器。

### 1.5 evaluation 模块现状

证据：

- `backend/src/main/java/com/learningos/evaluation/api/EvaluationSetController.java:19` 到 `EvaluationSetController.java:44` 提供 `/api/evaluation-sets` 管理 API。
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationSetService.java:55` 到 `EvaluationSetService.java:87` 支持 upsert evaluation set 和 samples。
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationSetService.java:126` 到 `EvaluationSetService.java:139` 校验 `RAG_QUESTION`、`GRADING_SAMPLE`、`RESOURCE_GENERATION_SAMPLE` 三类样本。
- `backend/src/main/java/com/learningos/evaluation/api/EvaluationRunController.java:19` 到 `EvaluationRunController.java:55` 提供 `/api/evaluation-runs` 记录与 comparison API。
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java:70` 到 `EvaluationRunService.java:106` 记录 run 和 metrics；`EvaluationRunService.java:118` 到 `EvaluationRunService.java:185` 比较 prompt versions。
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java:39` 到 `EvaluationRunService.java:50` 已允许 RAG 指标 `recallAtK`、`citationAccuracy`、`groundedness`、`noSourceRefusalRate`。
- `backend/src/main/resources/db/migration/V13__evaluation_set_management.sql:1` 到 `V13__evaluation_set_management.sql:42` 创建 `evaluation_set` 和 `evaluation_sample`。
- `backend/src/main/resources/db/migration/V14__evaluation_run_quality_metrics.sql:1` 到 `V14__evaluation_run_quality_metrics.sql:43` 创建 `evaluation_run` 和 `evaluation_run_metric`。

推断：

- evaluation 模块已经具备持久化实验集和质量 run 的基础，但 `RagEvaluationService` 当前不直接读取 `evaluation_set`，归档脚本也不把结果写入 `evaluation_run`。

## 2. 已经存在但 TODO 未勾选的证据

### 2.1 P2-2：定期 RAG 评估脚本 / 归档报告

TODO 原文：

- `docs/planning/backend-architecture-todolist.md` P2-2 中“增加定期评估脚本或测试，输出可归档报告”仍为未勾选。

已有证据：

- 脚本存在：`scripts/run-rag-evaluation-archive.ps1`。
- CLI 存在：`backend/src/test/java/com/learningos/rag/application/RagEvaluationArchiveReportCli.java`。
- 合约测试存在：`backend/src/test/java/com/learningos/rag/application/RagEvaluationArchiveReportTest.java`。
- evidence 记录执行脚本输出：`docs/evidence/EVIDENCE-20260606-rag-quality-evaluation.md` 记录输出 `backend/target/rag-evaluation-archive/latest/rag-quality-evaluation-report.md`。
- acceptance 已勾选脚本与报告：`docs/acceptance/ACCEPT-20260606-rag-quality-evaluation.md:23` 到 `ACCEPT-20260606-rag-quality-evaluation.md:26`。

判断：

- “脚本或测试 + 可归档报告”可认为已有实现，建议将 P2-2 最后一项更新为已完成。
- 如果主任务把“定期”解释为 CI/scheduler 自动周期执行，则仍有缺口：目前脚本没有 GitHub Actions/CI schedule、后端 scheduler，也没有从 `evaluation_set` 自动执行真实 RAG query。

### 2.2 P3-2：PDF/DOCX/Markdown/TXT 解析器

TODO 原文：

- P3-2 “增加 PDF、DOCX、Markdown、TXT 解析器”仍为未勾选。

已有证据：

- `IndexService.java:149` 到 `IndexService.java:156` 已按文档类型分派 Markdown/TXT/PDF/DOCX。
- `IndexService.java:248` 到 `IndexService.java:260` 根据 content type / 文件名识别 parser。
- `IndexService.java:263` 到 `IndexService.java:280` 提供 DOCX text 提取。
- `IndexService.java:282` 到 `IndexService.java:294` 提供 PDF text 提取。
- `IndexServiceTest.java:205` 到 `IndexServiceTest.java:224` 覆盖 TXT/PDF/DOCX 产出 chunks。

判断：

- 最小解析器雏形存在，但“生产级 parser”未完成。建议 TODO 不直接全勾选，而是拆为“最小解析器已完成”和“生产级 parser adapter/OCR/复杂文档支持待完成”。

### 2.3 P3-2：chunk 清洗、去重、切分、页码和章节识别

已有证据：

- `IndexService.java:311` 到 `IndexService.java:320` 做空白和控制字符清洗。
- `IndexService.java:192` 到 `IndexService.java:216` 固定长度切块并对相同 chunk 内容去重。
- `IndexService.java:159` 到 `IndexService.java:174` Markdown heading 识别章节。
- `IndexService.java:177` 到 `IndexService.java:188` PDF section 的 `pageNum` 简化为 `1`。
- `IndexServiceTest.java:165` 到 `IndexServiceTest.java:203` 验证 chunk 长度、空白清洗、metadata、section title。

判断：

- 最小 chunk 清洗/去重/切分已存在。
- 页码识别仍很弱，只有 PDF 简化页码，不支持真实分页；章节识别主要限 Markdown heading，不支持 DOCX heading style 或 PDF outline。

### 2.4 P3-2：索引任务幂等、重试、进度、失败恢复

已有证据：

- active 去重：`IndexService.java:71` 到 `IndexService.java:88`。
- 文档行锁：`KbDocumentRepository.java:17` 到 `KbDocumentRepository.java:19` 使用 `PESSIMISTIC_WRITE`。
- timeout recovery：`IndexService.java:136` 到 `IndexService.java:147` 将超时 `RUNNING` 标记 `FAILED`、递增 `retryCount`、写 `errorMessage/finishedAt`。
- scheduler：`IndexTaskRecoveryScheduler.java:38` 到 `IndexTaskRecoveryScheduler.java:56` 在应用 ready 和 fixed delay 触发 recovery。
- 配置：`application.yml:44` 到 `application.yml:48` 提供 `RAG_INDEX_RECOVERY_*`。
- 测试：`IndexServiceTest.java:94` 到 `IndexServiceTest.java:127` 覆盖并发 active 去重；`IndexServiceTest.java:129` 到 `IndexServiceTest.java:163` 覆盖超时恢复；`IndexTaskRecoverySchedulerTest.java:23` 到 `IndexTaskRecoverySchedulerTest.java:87` 覆盖 scheduler 行为。

仍未完成：

- `KbIndexTask` 没有 `progressPercent`、`currentStage`、`processedChunkCount`、`totalChunkCount`、`heartbeatAt`、`nextRetryAt`、`recoverable` 字段。
- `DocumentUploadResponse` 只返回 `documentId/indexTaskId/status`，`DocumentStatusResponse` 只返回文档状态，不返回 task 进度。
- 未发现生产 worker 自动处理 `PENDING` 任务并调用 `processIndexTask(...)`。
- scheduler 只失败化超时任务，不做重新入队。

## 3. 缺口和推荐最小实现顺序

### 3.1 缺口清单

P2-2 缺口：

- 脚本存在，但 fixture 固定，不读取 `evaluation_set`。
- 脚本不触发真实 `RagQueryService`，因此不能评估当前 retrieval 链路的真实召回。
- 脚本不写 `evaluation_run/evaluation_run_metric`，不能进入 prompt-version comparison。
- 没有 CI schedule 或后端 scheduler 的“定期”语义。

P3-2 缺口：

- Parser 生产化不足：PDF/DOCX 解析过轻，没有 adapter 接口、OCR fallback、复杂布局处理、真实页码和章节识别。
- Chunker 生产化不足：固定 1000 字符切块，没有 token budget、overlap、语义边界、标题层级、表格/代码块策略、稳定 chunk hash。
- Embedding 缺失：`EmbeddingService` 只返回 model version，没有 `embed(text)`、批量 embedding、缓存、失败重试或 provider adapter。
- VectorDB adapter 缺失：没有 VectorStore 接口、索引 upsert/delete、相似度搜索、metadata filter。
- Retrieval 缺失：`ChunkService` 只取最新 chunk，没有 keyword scoring、vector scoring、hybrid candidate fusion、RRF。
- Reranker 缺失：`RerankerService` 原样返回，没有 timeout、fallback、状态标记或降级原因。
- 索引 worker 缺失：没有独立 worker/scheduler 处理 `PENDING` task，`processIndexTask(...)` 目前只在测试中直接调用。
- Progress 缺失：没有 task-level 进度字段、阶段更新、heartbeat、对外查询接口。
- MySQL smoke 缺失：当前测试主要 H2，`PESSIMISTIC_WRITE`、MySQL stored procedure migration、TEXT/JSON 长度差异仍需 MySQL 8 smoke。

### 3.2 推荐最小实现顺序

1. 先补“真实索引执行器 + 进度 API”最小切片。
   - 原因：现在已有 `processIndexTask(...)`，但没有生产 worker 触发；先让 pending task 能被消费、能查询进度，才能让后续 parser/embedding/retrieval 进入可观测闭环。
   - 最小范围：新增 bounded scheduler/worker，处理少量 `PENDING` tasks；扩展 `KbIndexTask` 进度字段；新增 task detail DTO/API。

2. 再抽 parser/chunker adapter。
   - 原因：当前解析逻辑都在 `IndexService`，后续加入 PDF/DOCX 库或 OCR 会让 service 过大。
   - 最小范围：引入内部接口如 `DocumentParserAdapter`、`ChunkingStrategy`，先迁移现有轻量逻辑，不新增外部依赖；后续再做 dependency review。

3. 再做 embedding adapter 和 chunk hash。
   - 原因：embedding 前需要稳定 chunk identity，否则 reindex 会导致向量索引难以删除/更新。
   - 最小范围：`EmbeddingService` 增加 `embedBatch(...)` 接口和 noop/test adapter；`KbDocChunk.metadataJson` 或新字段保存 `chunkHash/embeddingModel/embeddingStatus`。

4. 再做 VectorDB adapter behind interface。
   - 原因：架构文档要求 MySQL 仍是主库，VectorDB 是可选 adapter。
   - 最小范围：接口 + noop/in-memory test adapter；生产 VectorDB 实现需 dependency review。

5. 再做 hybrid retrieval + RRF。
   - 原因：需要同时具备 lexical candidates 和 vector candidates 才有 fusion 意义。
   - 最小范围：`RetrievalCandidate` DTO、keyword retriever、vector retriever、`RrfFusionService`。

6. 最后做 reranker timeout fallback。
   - 原因：reranker 应接在 fused candidates 后；fallback 应返回 fused TopN，并在 `retrieval` metadata / `kb_query_log.rerankerStatus` 中记录降级。

7. P2-2 自动评估应接在 retrieval 完成后。
   - 原因：当前评估脚本只能验证指标计算；生产 retrieval 没做好前，自动 benchmark 对真实质量的信号弱。
   - 最小范围：基于 `evaluation_set` 的 RAG evaluation runner，调用 `RagQueryService`，写 `evaluation_run_metric`，同时输出 Markdown archive。

## 4. 文件边界建议

### 4.1 可修改文件边界建议

RAG 索引执行器 / 进度：

- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/IndexTaskRecoveryScheduler.java`
- `backend/src/main/java/com/learningos/rag/repository/KbIndexTaskRepository.java`
- `backend/src/main/java/com/learningos/rag/domain/KbIndexTask.java`
- `backend/src/main/java/com/learningos/rag/api/DocumentController.java`
- `backend/src/main/java/com/learningos/rag/api/dto/DocumentDtos.java`
- `backend/src/main/resources/db/migration/V16__rag_index_task_progress.sql` 或后续实际版本
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexTaskRecoverySchedulerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`

Parser/chunker adapter：

- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- 新增 `backend/src/main/java/com/learningos/rag/application/indexing/*`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- 新增 `backend/src/test/java/com/learningos/rag/application/indexing/*`

Embedding / VectorDB adapter：

- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- 新增 `backend/src/main/java/com/learningos/rag/application/retrieval/*`
- `backend/src/main/resources/application.yml`
- 如新增外部依赖，必须新增 `docs/security/DEP-*.md`，并更新 PLAN/TASK。

Hybrid retrieval / RRF / reranker fallback：

- `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/RerankerService.java`
- 新增 `backend/src/main/java/com/learningos/rag/application/retrieval/*`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`

P2-2 自动评估 runner：

- `backend/src/main/java/com/learningos/rag/application/RagEvaluationService.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java`
- 新增 `backend/src/main/java/com/learningos/evaluation/application/RagEvaluationRunService.java` 或 `backend/src/main/java/com/learningos/rag/application/RagEvaluationRunner.java`
- `scripts/run-rag-evaluation-archive.ps1`
- `backend/src/test/java/com/learningos/rag/application/RagEvaluationArchiveReportTest.java`
- `backend/src/test/java/com/learningos/evaluation/application/EvaluationRunServiceTest.java`

### 4.2 不建议修改的边界

- 不建议让 frontend 直接参与 RAG 评估执行或模型调用。
- 不建议让 Agent tool 直接访问 Repository；若 Course RAG Agent 触发 retrieval，应通过 `RagQueryService` 或新建 service adapter。
- 不建议在没有 dependency review 的情况下直接引入 PDFBox、Apache POI、Tika、Milvus、Chroma、OpenSearch、reranker SDK。
- 不建议为了 VectorDB 把 MySQL 主存储换成 PostgreSQL；`docs/architecture/rag-architecture.md:19` 明确 MySQL 是主数据库，VectorDB 应是 adapter。

## 5. 测试建议，含 TDD 首批红灯测试

### 5.1 P2-2 自动评估首批红灯测试

1. `RagEvaluationArchiveReportTest.archiveScriptCanLoadActiveEvaluationSetAndWriteRunMetrics`
   - 期望：给定 ACTIVE `evaluation_set` + RAG samples，runner 生成 Markdown 并写入 `evaluation_run`、`evaluation_run_metric`。
   - 当前会红：现有脚本只使用固定 fixture，不读取 `evaluation_set`。

2. `RagEvaluationRunServiceTest.recordsRecallCitationGroundednessAndNoSourceMetrics`
   - 期望：RAG benchmark run 的四个指标进入 `evaluation_run_metric`，metric names 使用 `recallAtK/citationAccuracy/groundedness/noSourceRefusalRate`。
   - 当前会红：没有 dedicated RAG evaluation runner。

3. `RagEvaluationArchiveReportTest.reportIncludesEvaluationRunIdAndTraceId`
   - 期望：归档报告包含 `evaluationRunId`、`traceId`、benchmark code/version。
   - 当前会红：现有 CLI 报告只有 fixture summary，不绑定 run。

4. `RagEvaluationRunServiceTest.deniesStudentRunningCourseBenchmark`
   - 期望：学生不能运行/读取不属于自己的 course benchmark。
   - 当前会红或无法编写：runner API/权限边界尚未定义。

### 5.2 P3-2 索引执行器和进度首批红灯测试

1. `IndexTaskWorkerTest.processesPendingTaskAndUpdatesProgressStages`
   - 期望：`PENDING -> RUNNING -> SUCCEEDED`，`progressPercent` 从 parse/chunk/embed/index 阶段递增。
   - 当前会红：`KbIndexTask` 没有 `progressPercent/currentStage`，也没有 worker。

2. `DocumentControllerTest.exposesIndexTaskProgressByTaskId`
   - 期望：`GET /api/index-tasks/{taskId}` 返回 task status、progress、stage、retryCount、errorMessage。
   - 当前会红：没有 index task detail API。

3. `IndexServiceTest.failedParserMarksDocumentAndTaskFailedWithSafeError`
   - 期望：解析失败同时收敛 `kb_index_task.status = FAILED`、`kb_document.parseStatus/indexStatus = FAILED`，错误脱敏且长度受限。
   - 当前部分可能已通过：`markIndexFailed(...)` 已做失败收敛，但建议补安全错误用例。

4. `IndexServiceTest.reindexDeletesOldVectorsWhenChunkHashChanges`
   - 期望：reindex 后旧 vector ids 被删除或标记 inactive。
   - 当前会红：没有 vector adapter 和 chunk hash。

### 5.3 Parser/chunker 红灯测试

1. `DocumentParserAdapterTest.markdownPreservesHeadingHierarchyAndChunkOverlap`
   - 期望：chunk metadata 包含 heading path、token length、overlap 信息。
   - 当前会红：metadata 只有 parser/model/contentLength。

2. `DocumentParserAdapterTest.docxHeadingStylesBecomeSectionTitles`
   - 期望：DOCX heading style 识别成 section title。
   - 当前会红：只提取 `<w:t>`，不识别 style。

3. `DocumentParserAdapterTest.pdfPageBoundariesBecomePageNums`
   - 期望：多页 PDF chunk 的 `pageNum` 正确。
   - 当前会红：当前 PDF 简化 `pageNum = 1`。

4. `ChunkingStrategyTest.deduplicatesByNormalizedHashNotOnlyExactText`
   - 期望：大小写、空白差异、重复页眉页脚被归一化去重。
   - 当前会红：只基于 cleaned content 完全一致去重。

### 5.4 Embedding / VectorDB / retrieval 红灯测试

1. `EmbeddingServiceTest.callsConfiguredEmbeddingAdapterAndReturnsVectorDimension`
   - 当前会红：`EmbeddingService` 没有 embedding API。

2. `VectorStoreAdapterTest.upsertsAndDeletesChunksByDocumentVersion`
   - 当前会红：没有 VectorDB adapter。

3. `HybridRetrievalServiceTest.fusesKeywordAndVectorCandidatesWithRrf`
   - 当前会红：没有 lexical/vector candidates 和 RRF。

4. `RerankerServiceTest.fallsBackToFusedCandidatesWhenRerankerTimesOut`
   - 当前会红：`RerankerService` 原样返回，没有 timeout 状态。

5. `RagQueryServiceTest.queryLogRecordsRrfAndRerankerFallbackMetadata`
   - 当前会红：`kb_query_log.rerankerStatus` 只写 routing strategy，未区分 RRF/reranker fallback。

### 5.5 权限和 citation 回归测试

1. `RagQueryServiceTest.vectorRetrievalFiltersForbiddenKbBeforeSearch`
   - 期望：即使请求 forged `kbIds`，vector search filter 只包含 allowed KB。
   - 当前暂无 vector path，但应作为 adapter 前置红灯测试。

2. `RagQueryServiceTest.noSourceResponseDoesNotPersistFabricatedCitation`
   - 现有链路应保持，通过该测试防止 reranker/vector fallback 引入伪引用。

3. `RagEvaluationRunServiceTest.evaluationCannotUseForeignCourseKb`
   - 期望：benchmark runner 不能用不属于当前 teacher/admin scope 的 KB。

4. `SourceCitationRepository` 关联完整性测试。
   - 期望：RAG query/resource generation 的 citations 带 `traceId`、`documentId`、`pageNum/sectionTitle/excerpt/score`，且 no-source 时不写伪记录。

## 6. 与 Agent Trace / Citation / Permission 的风险

### 6.1 Agent Trace 风险

证据：

- `RagQueryService` 写 `kb_query_log.traceId` 和 `source_citation.traceId`，但普通 RAG query 并不创建 `agent_task/agent_trace`；Orchestrator `RAG_QA` 已在上层统一 trace。
- `RagEvaluationArchiveReportCli` 是 test-scope CLI，不创建 `agent_task`、`agent_trace`、`model_call_log` 或 `token_usage_log`。

风险：

- 如果 P2-2 自动评估未来触发真实 RAG query 或模型生成，却不创建 evaluation run trace，将无法解释每次 benchmark 的输入、retrieval、降级、失败原因。
- 如果索引 worker 失败只写 `kb_index_task.errorMessage`，不写 trace step，运维侧只能看到 task 失败，无法解释 parser/chunker/embedding/vector upsert 哪一步失败。

建议：

- RAG evaluation runner 至少写 `evaluation_run.traceId`，并在 report 中包含 trace id。
- 索引 worker 可先不接 Agent Trace，但应记录 stage-level safe error；若作为 Agent/Orchestrator 子流程执行，则必须通过 `AgentRunRecorder` 写 trace。

### 6.2 Citation 风险

证据：

- `RagQueryService.java:184` 到 `RagQueryService.java:195` 从 chunks 生成 `sources`，有 sources 时持久化 `source_citation`。
- `SourceCitationRecord` 存 `documentId/documentName/pageNum/sectionTitle/excerpt/score`，但没有 `chunkId` 字段；当前 DTO 的 `documentId` 在 evaluation 中被用作 expected chunk/source id。
- `RagEvaluationService` 用 `SourceCitation.documentId` 对齐 `expectedChunkIds`。

风险：

- 字段语义存在混用：`SourceCitation.documentId` 实际承载 document id，但 evaluation benchmark 期望的是 chunk id/source id。当前可用但会影响准确评估 citation 命中。
- 引入 hybrid/vector retrieval 后，如果 candidate 不带稳定 `chunkId`、`documentVersion`、score source，citation accuracy 将难以解释。
- Reranker fallback 若未保留原始 retrieval score 和 fused rank，报告只能看到最终 score，不能解释降级来源。

建议：

- 后续应新增或扩展 citation DTO/schema：`chunkId`、`documentId`、`documentVersion`、`retrievalScore`、`rerankScore`、`rankSource`。
- RAG evaluation 的 expected ids 应明确是 `expectedChunkIds`，不要再隐式复用 `documentId`。

### 6.3 Permission 风险

证据：

- `PermissionService.filterAllowedKbIds(...)` 在 `RagQueryService` 读取 chunks 前执行。
- `ChunkService.retrieveAllowedChunks(...)` 查询 `kb_id in allowedKbIds`。
- `EvaluationSetService` 和 `EvaluationRunService` 只有 teacher/admin/course teacher 的管理访问控制。

风险：

- 引入 VectorDB adapter 后，如果 vector search 只按 frontend requested `kbIds` 查询，而不是使用 `allowedKbIds` metadata filter，会造成 forged `kbIds` 越权检索。
- `evaluation_set.kb_id` 与 `course_id` 的权限关系尚不强；自动 runner 如果按 set 中 `kbId` 直接查询，可能绕开 `PermissionService`。
- SSE stream 当前文档说明 local dev 可 fallback default user；生产 auth 若未收敛，可能扩大 RAG stream 权限风险。

建议：

- VectorDB adapter 的 search API 必须接收 `allowedKbIds`，不能接收 raw requested `kbIds`。
- 自动评估 runner 必须通过 `PermissionService` 或 course teacher/admin scope 校验 KB。
- 权限泄漏测试应覆盖 forged `kbIds`、foreign `evaluationSetId`、foreign `kbId`、foreign `courseId`。

## 7. 建议更新 TODO 状态

建议对 `docs/planning/backend-architecture-todolist.md` 做如下状态拆分，但本报告不修改该文件：

- P2-2 “增加定期评估脚本或测试，输出可归档报告”：若按“脚本或测试 + 可归档报告”验收，建议勾选；若按“周期自动执行 + evaluation_run 持久化”验收，建议拆成两个子项，其中脚本/报告已完成，周期 runner 未完成。
- P3-2 “增加 PDF、DOCX、Markdown、TXT 解析器”：建议拆为“最小轻量 parser 已完成”和“生产级 parser adapter/OCR/复杂文档支持未完成”。
- P3-2 “chunk 清洗、去重、切分策略、页码和章节识别”：建议拆为“最小清洗/去重/切分/Markdown 章节已完成”和“真实页码/heading hierarchy/token overlap/chunk hash 未完成”。
- P3-2 “embedding service 和可选 VectorDB adapter”：仍未完成。
- P3-2 “hybrid retrieval、RRF、reranker timeout fallback”：仍未完成。
- P3-2 “索引任务支持幂等、重试、进度、失败恢复”：幂等和失败恢复已完成；进度、heartbeat、worker retry/requeue 仍未完成。

## 8. 未运行测试说明

本次作为子代理只做代码库探索和报告，未运行 Maven 测试或 `scripts/run-rag-evaluation-archive.ps1`。原因是这些命令会写入 `backend/target` 或生成归档文件，而本任务明确要求只创建一个报告文件。

