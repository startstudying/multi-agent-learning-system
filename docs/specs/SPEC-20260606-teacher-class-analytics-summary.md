# 教师端班级学习分析规格

## API

### `GET /api/analytics/classes/{courseId}/summary`

请求头：

```http
X-User-Id: teacher
```

响应：

```json
{
  "code": "OK",
  "data": {
    "courseId": "course_backend",
    "teacherId": "teacher",
    "learnerCount": 2,
    "weakKnowledgePoints": [
      {
        "knowledgePointId": "kp_sql_join",
        "title": "SQL JOIN",
        "averageMastery": 0.5,
        "wrongQuestionCount": 2,
        "affectedLearnerCount": 2,
        "topCause": "Missed join cardinality."
      }
    ],
    "wrongCauseDistribution": [
      {
        "knowledgePointId": "kp_sql_join",
        "causeAnalysis": "Missed join cardinality.",
        "count": 2
      }
    ],
    "resourceCompletion": {
      "totalTasks": 3,
      "doneTasks": 1,
      "waitingReviewTasks": 1,
      "failedTasks": 1,
      "averageProgressPercent": 60.0,
      "completionRate": 0.3333333333333333
    },
    "pendingReviews": [
      {
        "reviewId": "review_1",
        "resourceId": "res_1",
        "generationTaskId": "task_1",
        "status": "PENDING_CRITIC",
        "reviewerType": "CRITIC_AGENT",
        "resourceTitle": "SQL JOIN remediation",
        "resourceType": "EXERCISE"
      }
    ]
  }
}
```

## 聚合规则

- `learnerCount`：`LearningPath.goalId == courseId` 的不同 `learnerId` 数。
- `weakKnowledgePoints`：
  - 只统计课程知识点内的 path node。
  - `averageMastery < 0.6` 或存在非 `DONE` 节点时进入候选。
  - `affectedLearnerCount` 为该知识点涉及的不同学习者数。
  - `wrongQuestionCount` 为该知识点相关错题数。
  - `topCause` 为该知识点出现次数最多的 `causeAnalysis`。
- `wrongCauseDistribution`：按 `knowledgePointId + causeAnalysis` 分组计数。
- `resourceCompletion`：
  - 统计 `goalId == courseId` 且 learner 属于课程学习者集合的 `ResourceGenerationTask`。
  - `doneTasks` 统计 `status == DONE`。
  - `waitingReviewTasks` 统计 `reviewStatus == PENDING_CRITIC` 或 `status == WAITING_REVIEW`。
  - `failedTasks` 统计 `status == FAILED`。
  - `completionRate = doneTasks / totalTasks`，无任务时为 `0.0`。
- `pendingReviews`：
  - 只返回课程相关任务的 `PENDING_CRITIC` 和 `REVISION_REQUESTED` review。
  - 关联 `LearningResource` 后补充 `resourceTitle` 和 `resourceType`。
  - 不返回 `markdownContent`。

## 权限策略

- `admin` 通过。
- `currentUserId == Course.teacherId` 通过。
- 其他用户返回 `FORBIDDEN`。
- 课程不存在返回 `NOT_FOUND`。

## 架构漂移检查

- Controller 只做 HTTP 路由和当前用户传递。
- Service 负责课程存在性、权限和聚合逻辑。
- 不新增数据库结构。
- 不新增依赖。
- 不调用 LLM。
