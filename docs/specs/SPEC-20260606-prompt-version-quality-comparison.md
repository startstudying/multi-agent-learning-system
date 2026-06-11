# Prompt Version 质量对比 SPEC

## 数据模型

### `evaluation_run`

| 字段 | 说明 |
|---|---|
| `id` | run id，前缀 `evr_` |
| `evaluation_set_id` | 关联 `evaluation_set.id` |
| `set_type` | `RAG_QUESTION` / `GRADING_SAMPLE` / `RESOURCE_GENERATION_SAMPLE` |
| `prompt_code` | prompt code |
| `prompt_version` | prompt version |
| `model_name` | 可选模型名 |
| `run_status` | `SUCCEEDED` / `FAILED` |
| `sample_count` | 本次 run 覆盖的样本数 |
| `created_by` | 创建者 |
| `trace_id` | 可选 trace |
| `error_message` | 安全错误摘要 |
| `metadata_json` | 白名单元数据 |
| `started_at` | 开始时间 |
| `finished_at` | 完成时间 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

### `evaluation_run_metric`

| 字段 | 说明 |
|---|---|
| `id` | metric id，前缀 `evrm_` |
| `run_id` | 关联 `evaluation_run.id` |
| `metric_name` | 指标名 |
| `metric_value` | 指标值 |
| `metric_unit` | 可选单位，默认 `score` |
| `sample_count` | 该指标覆盖样本数，必须大于 0 |
| `metric_json` | 白名单指标摘要 |
| `created_at` | 创建时间 |

## API

### POST `/api/evaluation-runs`

记录一次评测结果。

请求：

```json
{
  "evaluationSetId": "evs_xxx",
  "promptCode": "rag-answer",
  "promptVersion": "agent-rag-v2",
  "model": "mock-model",
  "status": "SUCCEEDED",
  "sampleCount": 10,
  "traceId": "trace_xxx",
  "metrics": [
    {
      "metricName": "groundedness",
      "metricValue": 0.8,
      "sampleCount": 10
    }
  ]
}
```

响应返回 run 和 metrics，不返回样本输入、raw prompt 或 raw output。

### GET `/api/evaluation-runs/comparison`

查询参数：

- `evaluationSetId`
- `promptCode`
- `promptVersions`：逗号分隔，例如 `agent-rag-v1,agent-rag-v2`
- `baselinePromptVersion`：可选，默认取请求列表中第一个有 `SUCCEEDED` run 的版本

响应核心字段：

```json
{
  "evaluationSetId": "evs_xxx",
  "promptCode": "rag-answer",
  "baselinePromptVersion": "agent-rag-v1",
  "rows": [
    {
      "promptVersion": "agent-rag-v1",
      "runCount": 1,
      "sampleCount": 10,
      "metrics": {
        "groundedness": {
          "average": 0.55,
          "sampleCount": 10,
          "runCount": 1
        }
      },
      "deltas": {
        "groundedness": 0.0
      }
    }
  ],
  "winnerByMetric": {
    "groundedness": "agent-rag-v2"
  }
}
```

## 聚合规则

- comparison 只读取 `SUCCEEDED` run。
- 同一 `promptVersion + metricName` 下存在多次 run 时，平均值按 `evaluation_run_metric.sample_count` 加权。
- 加权公式：`sum(metricValue * metricSampleCount) / sum(metricSampleCount)`。
- `meanAbsoluteError` 是越低越好，其余白名单指标默认越高越好。

## 错误规则

- evaluation set 不存在：`NOT_FOUND`
- 权限不足：`FORBIDDEN`
- 少于两个 prompt version 有 `SUCCEEDED` run：`VALIDATION_ERROR`
- 指标名不在白名单：`VALIDATION_ERROR`
- 指标值为空、NaN 或无穷：`VALIDATION_ERROR`
- 指标样本数小于等于 0：`VALIDATION_ERROR`
