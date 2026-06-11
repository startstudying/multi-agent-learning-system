# RAG 质量评估 Worker Run

## Worker Role

并行后端 worker，负责 TODO P2-2「RAG 质量评估」最小可验收切片。

## Scope

- 允许修改 `rag` 包和本任务指定文档。
- 不修改 `evaluation` 包。
- 不修改共享 memory、changelog、backend TODO。
- 不新增依赖。

## Analysis

现有 `RagEvaluationService` 已能按 `expectedSourceIds`、`actualCitations`、`topK` 计算基础 `recallAtK`、`citationAccuracy` 和 `groundedness`。缺口是：

- 没有课程 benchmark 元数据和样本级结果。
- 没有 `noSourceRefusalRate`。
- 没有可归档 report。
- Controller 响应未覆盖新增字段。

## Design Decision

本轮采用兼容扩展：

- `RagEvaluationRequest` 保留旧字段，新增 `benchmark` 嵌套 record。
- `RagEvaluationResult` 保留旧字段，新增 `noSourceRefusalRate`、`benchmarkSummary`、`sampleResults`、`report`。
- `RagEvaluationService` 在存在 benchmark samples 时按样本聚合；否则继续旧逻辑。
- 复用 `SourceCitation.documentId` 作为 expected chunk/source id 对齐字段，避免改造 citation DTO 或数据库。

## Risks

- 本轮没有直接读取 `evaluation_set/evaluation_sample`，仅让 API 输入结构兼容其 RAG 样本字段；后续可由 evaluation run 或脚本负责装配请求。
- 无来源拒答目前用确定性文本/`NO_SOURCE` 标记识别，不做模型裁判。
