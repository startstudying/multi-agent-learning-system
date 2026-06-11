# RAG 质量评估需求

## 功能需求

1. `RagEvaluationRequest` 必须继续兼容旧字段：
   - `expectedSourceIds`
   - `actualCitations`
   - `topK`
2. 请求可以新增 `benchmark`，包含课程 benchmark 元数据和样本列表。
3. 每个 benchmark sample 至少支持：
   - `sampleKey`
   - `question`
   - `expectedChunkIds`
   - `expectedAnswer`
   - `forbiddenAnswerScope`
   - `expectedNoSource`
   - `actualAnswer`
   - `actualCitations`
   - `topK`
4. `Recall@K` 按每个样本 topK 内命中的期望 chunk 比例计算，再对样本取平均。
5. `Citation Accuracy` 按实际引用中命中期望 chunk 的比例计算，再对样本取平均。
6. `Groundedness` 由样本的 `Recall@K` 和 `Citation Accuracy` 调和平均得到，再对样本取平均。
7. `No-source Refusal Rate` 只统计 `expectedNoSource=true` 的样本：如果实际答案是拒答且没有引用，则记为命中。
8. 响应必须包含：
   - 顶层四项指标
   - `benchmarkSummary`
   - `sampleResults`
   - `report`
9. `report` 必须是可归档的文本摘要，包含 benchmark 名称、版本、样本数和核心指标。

## 兼容需求

1. 旧三字段请求仍然返回原有指标字段和计数字段。
2. benchmark 为空时按旧字段计算。
3. 引用命中应优先使用 `SourceCitation.documentId` 与期望 chunk/source id 对齐，避免改造现有 citation DTO。

## 安全与治理需求

1. 不保存 raw prompt、API key 或模型 provider 响应。
2. Controller 只处理 HTTP 请求/响应，指标逻辑必须在 Service。
3. 不新增依赖。
4. 不改 `evaluation` 包和 V14 migration。
