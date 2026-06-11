# SPEC-20260608 P3-4-F Assessment Record List RBAC / Pagination

## 1. Scope

本切片新增 assessment answer / wrong-question 最小分页列表：

- `GET /api/assessment/answers`
- `GET /api/assessment/wrong-questions`

本切片不新增 schema，不改提交/评分链路，不改前端。

## 2. API Contract

### 2.1 `GET /api/assessment/answers`

Query:

```http
GET /api/assessment/answers?learnerId=alice&courseId=course_sql&page=0&size=20
```

成功响应：

```json
{
  "code": "OK",
  "data": {
    "items": [
      {
        "answerId": "ans_x",
        "learnerId": "alice",
        "questionId": "q_sql_join",
        "score": 0.85,
        "safetyStatus": "SAFE",
        "wrongQuestionId": "wq_x",
        "resourcePushStrategy": "PUSH_REMEDIAL_PRACTICE_AND_CODE_LAB",
        "traceId": "trc_x",
        "createdAt": "2026-06-08T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 2.2 `GET /api/assessment/wrong-questions`

成功响应：

```json
{
  "code": "OK",
  "data": {
    "items": [
      {
        "wrongQuestionId": "wq_x",
        "answerId": "ans_x",
        "learnerId": "alice",
        "questionId": "q_sql_join",
        "knowledgePointId": "kp_sql_join",
        "score": 0.85,
        "resourcePushStrategy": "PUSH_REMEDIAL_PRACTICE_AND_CODE_LAB",
        "traceId": "trc_x",
        "createdAt": "2026-06-08T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

## 3. Authorization Matrix

| Actor | No filter | `learnerId` filter | `courseId` filter | Foreign learner/course |
|---|---|---|---|---|
| student | Own records | Only own learnerId | Own records within enrolled course | `FORBIDDEN` |
| teacher / teacher_* | `VALIDATION_ERROR` because `courseId` required | Course enrolled learner only | Own-course active enrollment records | foreign course `FORBIDDEN`; foreign learner empty list |
| admin | Global records | Filtered records | Filtered records | missing course `NOT_FOUND` |

## 4. Course Scope Derivation

Answer list course filtering uses the existing question/knowledge convention:

```text
courseId
-> KnowledgePointRepository.findByCourseIdOrderByCreatedAtAsc(courseId)
-> questionIds = "q_" + knowledgePointId.removePrefix("kp_")
-> AnswerRecordRepository scoped query by learnerIds + questionIds
```

Wrong-question list course filtering uses:

```text
courseId
-> knowledgePointIds
-> WrongQuestionRepository scoped query by learnerIds + knowledgePointIds
```

如果 course 没有 knowledge points，返回空 page。

## 5. Implementation Design

新增 DTO：

- `AssessmentPageResponse<T>`
- `AssessmentRecordSummaryResponse`
- `WrongQuestionSummaryResponse`

修改：

- `AssessmentController`
  - 增加 `GET /answers`
  - 增加 `GET /wrong-questions`
- `AssessmentService`
  - 增加 `listAnswers(currentUserId, learnerId, courseId, page, size)`
  - 增加 `listWrongQuestions(currentUserId, learnerId, courseId, page, size)`
  - 增加分页参数校验和 scoped query 辅助
- repositories
  - `AnswerRecordRepository` 增加 learner/question scoped page 查询方法
  - `WrongQuestionRepository` 增加 learner/knowledge scoped page 查询方法

不修改：

- `backend/pom.xml`
- migration
- frontend
- provider/RAG/vector/parser

## 6. Error Semantics

- student 查询他人 learnerId：`FORBIDDEN`，无 `data`。
- teacher 不传 `courseId`：`VALIDATION_ERROR`。
- teacher foreign/missing course：`FORBIDDEN`，无 `data`。
- admin missing course：`NOT_FOUND`。
- 非法 page/size：`VALIDATION_ERROR`。
- teacher 查询自己课程下未 enrolled learner：返回空 page，不暴露 learner 存在性。

## 7. Response Redaction

列表响应使用 summary DTO，批量场景不返回：

- answer 原文
- `requestId`
- `requestHash`
- `responseJson`
- `payloadJson`
- `gradingResultId`
- `causeAnalysis`
- `replanRecordId`

## 8. Tests

Focused:

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest test
```

Adjacent:

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test
```

Full:

```powershell
cd backend
mvn test
```
