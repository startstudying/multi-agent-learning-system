# Analytics 学习分析扩展规格

## API

### `GET /api/analytics/overview`

保留当前响应 envelope：

```json
{
  "code": "OK",
  "data": {
    "agentTaskCount": 0,
    "modelCallCount": 0,
    "tokenUsage": {},
    "answerRecordCount": 0,
    "wrongQuestionCount": 0,
    "learningEventCount": 0,
    "resourceReviewStatusCounts": {},
    "agentSummary": {}
  }
}
```

新增字段不移除旧字段。

### `GET /api/analytics/students/{learnerId}/summary`

响应：

```json
{
  "code": "OK",
  "data": {
    "learnerId": "alice",
    "progress": {
      "totalNodes": 3,
      "doneNodes": 1,
      "activeNodes": 1,
      "lockedNodes": 1,
      "completionRate": 0.3333333333
    },
    "currentMastery": [
      {
        "knowledgePointId": "kp_sql_join",
        "mastery": 0.58,
        "sourceType": "ASSESSMENT",
        "sourceId": "ans_1",
        "reasonSummary": "..."
      }
    ],
    "masteryTrend": [
      {
        "knowledgePointId": "kp_sql_join",
        "mastery": 0.42,
        "sourceType": "ASSESSMENT"
      }
    ],
    "recentWrongCauses": [
      {
        "knowledgePointId": "kp_sql_join",
        "questionId": "q_sql_join",
        "score": 0.4,
        "causeAnalysis": "...",
        "resourcePushStrategy": "..."
      }
    ],
    "recommendedNextSteps": [
      {
        "type": "PATH_NODE",
        "knowledgePointId": "kp_tx",
        "title": "Transactional Boundaries",
        "reason": "..."
      }
    ]
  }
}
```

## 聚合规则

- `DONE` path node 计入完成；`ACTIVE`、`READY` 计入 active；`LOCKED` 计入 locked；其他状态只计入 total。
- `currentMastery` 按 `knowledgePointId` 取最新 mastery record；没有 mastery record 时使用 path node mastery。
- `masteryTrend` 按现有记录返回，按可用顺序稳定输出。
- `recentWrongCauses` 使用最近可读错题记录；当前实体缺少 getter 的创建时间排序能力时，使用 repository 返回顺序的反序作为“最近”近似。
- `recommendedNextSteps` 最多返回 3 条，优先未完成 path node，再补最近错因的资源推送策略。
- `ragHitRate = retrievalCount > 0 的日志数 / 全部 kb_query_log 日志数`。

## 架构漂移检查

- Controller 只处理 HTTP 与当前用户校验。
- Service 只读 repository 并做确定性聚合。
- 不新增 DB schema。
- 不新增依赖。
- 不让前端或 analytics 直接调用 LLM。
