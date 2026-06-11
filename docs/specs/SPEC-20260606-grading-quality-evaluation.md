# 自动批改质量评估 SPEC

## API

### POST `/api/assessment/grading-evaluations`

请求示例：

```json
{
  "courseId": "course_sql",
  "samples": [
    {
      "sampleId": "sample_1",
      "questionType": "SHORT_ANSWER",
      "knowledgePointId": "kp_sql_join",
      "rubricVersion": "rubric-v1",
      "humanScore": 0.9,
      "systemScore": 0.8,
      "humanGrade": "A",
      "systemGrade": "B",
      "humanWrongCause": "TRANSFER_WEAKNESS",
      "systemWrongCause": "TRANSFER_WEAKNESS"
    }
  ]
}
```

旧版兼容请求：

```json
{
  "courseId": "course_sql",
  "humanScores": [1.0, 0.7],
  "aiScores": [0.96, 0.72],
  "agreementThreshold": 0.05
}
```

响应示例：

```json
{
  "code": "OK",
  "data": {
    "meanAbsoluteError": 0.08,
    "agreementRate": 0.66,
    "gradeAgreementRate": 0.66,
    "wrongCauseAgreementRate": 1.0,
    "sampleCount": 3,
    "groupedAnalysis": {
      "questionType": [
        {
          "groupKey": "SHORT_ANSWER",
          "sampleCount": 2,
          "meanAbsoluteError": 0.1,
          "gradeAgreementRate": 0.5,
          "wrongCauseAgreementRate": 1.0
        }
      ],
      "knowledgePointId": [],
      "rubricVersion": []
    }
  },
  "message": ""
}
```

## 指标口径

### `meanAbsoluteError`

公式：

```text
sum(abs(humanScore - systemScore)) / sampleCount
```

分母为有效评分样本数。

### `gradeAgreementRate`

新版 `samples` 模式：

```text
matched grade pair count / comparable grade pair count
```

旧版分数数组模式：

```text
abs(humanScore - systemScore) <= agreementThreshold 的样本数 / sampleCount
```

### `agreementRate`

保留字段，值等于 `gradeAgreementRate`。

### `wrongCauseAgreementRate`

```text
matched wrong-cause pair count / comparable wrong-cause pair count
```

当没有可比较错因样本时返回 `0.0`。

## 分组规则

`groupedAnalysis` 使用固定 key：

- `questionType`
- `knowledgePointId`
- `rubricVersion`

每个维度按样本字段分组。空白或缺失字段归入 `UNKNOWN`。分组顺序按首次出现顺序返回。

## 服务边界

- `AssessmentController` 只负责 HTTP request 转交和响应 envelope。
- `GradingEvaluationService` 负责校验、兼容旧请求、总体指标和分组计算。
- 本切片不触碰 `AssessmentService` 答题提交流程。
- 2026-06-08 P3-4-G 后，HTTP 入口必须提供 `courseId`，并在计算指标前执行 course scope 授权与 sample `knowledgePointId` 归属校验；纯指标计算构造器/测试仍保留算法兼容。
- 本切片原始指标设计不读写数据库；P3-4-G 的 HTTP 授权路径会读取 Course / KnowledgePoint 以完成权限和一致性校验，但不写入数据库。

## 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Controller 只委托 Service | PASS | endpoint 调用 `GradingEvaluationService` |
| Service 包含业务逻辑 | PASS | 评估指标在 service 内计算 |
| 无新增依赖 | PASS | 使用 JDK 和现有测试栈 |
| 无 DB schema 改动 | PASS | 离线请求计算；P3-4-G 只新增授权读取，不新增 migration |
| 不调用 LLM / Agent / RAG | PASS | 不涉及模型调用和 trace |

## 2026-06-08 P3-4-G 权限注记

本 SPEC 的指标口径仍有效，但 HTTP 合同已由 `docs/specs/SPEC-20260608-grading-evaluation-course-scope.md` 收口：

- `POST /api/assessment/grading-evaluations` 请求必须包含 `courseId`。
- student 请求优先返回 `FORBIDDEN`。
- teacher 只能评估 own course；teacher missing/foreign course 均返回安全 `FORBIDDEN`。
- admin 可评估任意 existing course；admin missing course 返回 `NOT_FOUND`。
- 非空 `samples[].knowledgePointId` 必须属于请求 `courseId`。
