# RAG 质量评估 SPEC

## API

### POST `/api/rag/evaluations`

请求继续支持旧格式：

```json
{
  "expectedSourceIds": ["chunk_sql_join"],
  "actualCitations": [],
  "topK": 3
}
```

新增 benchmark 格式：

```json
{
  "benchmark": {
    "benchmarkId": "rag-sql-course",
    "courseId": "course_sql",
    "name": "SQL 课程 RAG Benchmark",
    "version": "v1",
    "topK": 3,
    "samples": [
      {
        "sampleKey": "rag-001",
        "question": "JOIN 为什么会产生重复行？",
        "expectedChunkIds": ["chunk_join"],
        "expectedAnswer": "JOIN 会按照匹配行组合结果。",
        "forbiddenAnswerScope": "不得引用课程资料之外的执行计划细节。",
        "expectedNoSource": false,
        "actualAnswer": "JOIN 会按照匹配行组合结果。",
        "actualCitations": [
          {
            "documentId": "chunk_join",
            "documentName": "sql.md",
            "pageNum": 1,
            "sectionTitle": "JOIN",
            "excerpt": "JOIN 会组合匹配行。",
            "score": 0.95
          }
        ],
        "topK": 3
      }
    ]
  }
}
```

响应核心字段：

```json
{
  "recallAtK": 1.0,
  "citationAccuracy": 1.0,
  "groundedness": 1.0,
  "noSourceRefusalRate": 0.0,
  "expectedSourceCount": 1,
  "evaluatedCitationCount": 1,
  "relevantCitationCount": 1,
  "benchmarkSummary": {
    "benchmarkId": "rag-sql-course",
    "courseId": "course_sql",
    "name": "SQL 课程 RAG Benchmark",
    "version": "v1",
    "sampleCount": 1,
    "noSourceSampleCount": 0,
    "sourceRequiredSampleCount": 1,
    "topK": 3
  },
  "sampleResults": [],
  "report": "..."
}
```

## 指标规则

### Recall@K

- 对每个 source-required sample，取实际引用前 `topK` 条。
- 命中数按去重后的 `documentId` 与 `expectedChunkIds` 交集计算。
- 分母为去重后的 `expectedChunkIds` 数量。
- 无期望 chunk 且无引用时记为 `1.0`；无期望 chunk 但有引用时记为 `0.0`。

### Citation Accuracy

- 对每个 source-required sample，统计全部实际引用中命中 `expectedChunkIds` 的比例。
- 没有实际引用但有期望 chunk 时记为 `0.0`。
- 没有期望 chunk 且没有实际引用时记为 `1.0`。

### Groundedness

- 对每个 source-required sample，使用该样本的 `Recall@K` 和 `Citation Accuracy` 调和平均。
- 任一指标为 `0.0` 时该样本 `groundedness=0.0`。

### No-source Refusal Rate

- 只统计 `expectedNoSource=true` 的样本。
- 命中条件：
  - 实际引用为空；
  - 实际答案包含拒答语义，例如 `NO_SOURCE`、`No cited course material`、`没有找到课程资料`、`无法基于课程资料回答`。
- 如果没有 no-source 样本，返回 `0.0`。

## Benchmark Summary

`benchmarkSummary` 包含：

- `benchmarkId`
- `courseId`
- `name`
- `version`
- `sampleCount`
- `noSourceSampleCount`
- `sourceRequiredSampleCount`
- `topK`

## Sample Result

每个样本输出：

- `sampleKey`
- `question`
- `expectedNoSource`
- `recallAtK`
- `citationAccuracy`
- `groundedness`
- `noSourceRefusal`
- `expectedChunkCount`
- `evaluatedCitationCount`
- `relevantCitationCount`

## 架构约束

- 不新增数据库迁移。
- 不改 `evaluation` 包。
- 不改 RAG query 执行链路。
- 不新增依赖。
- Service 负责全部指标计算，Controller 保持转发。
