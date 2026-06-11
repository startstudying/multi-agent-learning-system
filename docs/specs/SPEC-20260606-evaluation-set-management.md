# Evaluation Set 管理 SPEC

## 领域模型

### EvaluationSet

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `varchar(80)` | `evs_` 前缀 |
| `code` | `varchar(120)` | 评估集业务编码 |
| `version` | `varchar(80)` | 评估集版本 |
| `name` | `varchar(255)` | 展示名称 |
| `description` | `text` | 描述 |
| `set_type` | `varchar(40)` | `RAG_QUESTION` / `GRADING_SAMPLE` / `RESOURCE_GENERATION_SAMPLE` |
| `status` | `varchar(40)` | `DRAFT` / `ACTIVE` / `ARCHIVED` |
| `course_id` | `varchar(80)` | 可选课程边界 |
| `kb_id` | `varchar(80)` | 可选知识库边界 |
| `prompt_code` | `varchar(120)` | 后续质量对比预留 |
| `prompt_version` | `varchar(120)` | 后续质量对比预留 |
| `created_by` | `varchar(120)` | 创建者 |
| `created_at` | `datetime(6)` | 创建时间 |
| `updated_at` | `datetime(6)` | 更新时间 |
| `deleted_at` | `datetime(6)` | 软删除预留 |

唯一约束：`(created_by, code, version)`。

### EvaluationSample

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `varchar(80)` | `evsm_` 前缀 |
| `set_id` | `varchar(80)` | 所属评估集 |
| `sample_type` | `varchar(40)` | 与 set type 一致 |
| `sample_key` | `varchar(120)` | 可选样本编码 |
| `input_json` | `text` | 类型白名单后的输入快照 |
| `expected_json` | `text` | 类型白名单后的期望输出 |
| `metadata_json` | `text` | 标签、topK、质量标准等低敏元数据 |
| `created_at` | `datetime(6)` | 创建时间 |
| `updated_at` | `datetime(6)` | 更新时间 |

## API 合同

### POST `/api/evaluation-sets`

创建或更新评估集。

```json
{
  "code": "rag-sql-benchmark",
  "version": "v1",
  "name": "SQL RAG benchmark",
  "description": "Course RAG benchmark",
  "type": "RAG_QUESTION",
  "status": "ACTIVE",
  "courseId": "course_sql",
  "kbId": "kb_sql",
  "promptCode": "rag-answer",
  "promptVersion": "agent-rag-v1",
  "samples": [
    {
      "sampleKey": "rag-001",
      "question": "How do JOIN duplicates happen?",
      "expectedSourceIds": ["doc_sql_join"],
      "topK": 3
    }
  ]
}
```

### GET `/api/evaluation-sets?type=RAG_QUESTION`

返回当前用户可见的评估集列表。列表响应不返回样本明细。

### GET `/api/evaluation-sets/{setId}`

返回评估集详情和样本明细。

## 权限规则

- `admin` 可管理全部评估集。
- `teacher` 可创建和读取自己创建的评估集。
- 如果绑定课程，课程 `teacherId` 等于当前用户时可管理。
- 普通学生访问管理 API 返回 `FORBIDDEN`。
- Service 层必须执行最终权限判断。

## 安全边界

- 不保存 raw prompt。
- 不保存 raw model output。
- 外部请求不允许写入 `createdBy`、`createdAt`、`updatedAt`、`deletedAt`。
- 样本 JSON 由 Service 根据白名单字段生成，不接受任意 JSON 原样入库。

## 测试策略

- Service 测试覆盖三类样本创建、upsert、类型校验和学生 403。
- Controller 测试覆盖创建、查询、列表和 403。
- Migration 文本测试覆盖表、列、索引和唯一约束。
