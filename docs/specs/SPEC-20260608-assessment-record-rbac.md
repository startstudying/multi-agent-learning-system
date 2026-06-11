# SPEC-20260608 P3-4-E Assessment Record RBAC Matrix

## 1. Scope

本切片只新增 assessment record 最小详情读取能力：

- `GET /api/assessment/answers/{answerId}`
- `GET /api/assessment/wrong-questions/{wrongQuestionId}`

不实现 list，不新增 schema，不改提交/评分链路。

## 2. API Contract

### 2.1 `GET /api/assessment/answers/{answerId}`

成功响应：

```json
{
  "code": "OK",
  "data": {
    "answerId": "ans_x",
    "learnerId": "alice",
    "questionId": "q_sql_join",
    "answer": "learner answer text",
    "score": 0.85,
    "safetyStatus": "SAFE",
    "gradingResultId": "grd_x",
    "wrongQuestionId": "wq_x",
    "wrongCauseAnalysis": "{...}",
    "masteryUpdates": "[...]",
    "resourcePushStrategy": "PUSH_REMEDIAL_PRACTICE_AND_CODE_LAB",
    "replanRecordId": "trc_x",
    "traceId": "trc_x"
  }
}
```

说明：

- `answer` 仅在通过授权后返回。
- `masteryUpdates` 和 `wrongCauseAnalysis` 暂以已清洗 JSON 字符串返回，避免本切片扩大解析模型。
- 不返回 `requestId` / `requestHash` / `responseJson`。

### 2.2 `GET /api/assessment/wrong-questions/{wrongQuestionId}`

成功响应：

```json
{
  "code": "OK",
  "data": {
    "wrongQuestionId": "wq_x",
    "answerId": "ans_x",
    "gradingResultId": "grd_x",
    "learnerId": "alice",
    "questionId": "q_sql_join",
    "knowledgePointId": "kp_sql_join",
    "score": 0.85,
    "wrongCauseAnalysis": "{...}",
    "resourcePushStrategy": "PUSH_REMEDIAL_PRACTICE_AND_CODE_LAB",
    "replanRecordId": "trc_x",
    "traceId": "trc_x"
  }
}
```

## 3. Authorization Matrix

| Actor | Own record | Course-authorized learner record | Foreign record | Missing record |
|---|---:|---:|---:|---:|
| student | 200 | N/A | 403 | 403 |
| teacher / teacher_* | 200 only if course-authorized | 200 | 403 | 403 |
| admin | 200 | 200 | 200 | 404 |

Teacher course authorization requires:

1. `questionId` resolves to a `knowledgePointId` through `AssessmentFeedbackService.resolveKnowledgePointId(...)`.
2. `KnowledgePoint` exists and has non-blank `courseId`.
3. `CourseAccessService.requireCourseRead(currentUserId, courseId)` passes for the teacher-owned course.
4. `CourseAccessService.listActiveLearnerIds(courseId)` contains the record learner.

If any step fails for non-admin/non-owner, return `FORBIDDEN`.

## 4. Implementation Design

新增 DTO：

- `AssessmentRecordDetailResponse`
- `WrongQuestionDetailResponse`

修改：

- `AssessmentController`
  - 增加两个 `@GetMapping`，只传 current user 和 path variable。
- `AssessmentService`
  - 增加 `answerDetail(currentUserId, answerId)`。
  - 增加 `wrongQuestionDetail(currentUserId, wrongQuestionId)`。
  - 增加私有授权辅助：`requireAnswerReadable(...)`、`requireWrongQuestionReadable(...)`。
- repositories
  - `GradingResultRepository.findFirstByAnswerIdOrderByCreatedAtDesc(...)`
  - `WrongQuestionRepository.findFirstByAnswerIdOrderByCreatedAtDesc(...)`
- domain getters
  - 如 DTO 需要，补齐 `createdAt` getter；不改变字段。

不修改：

- `backend/pom.xml`
- migration
- frontend
- provider/RAG/vector/parser

## 5. Error Semantics

- 非 admin missing/foreign：`FORBIDDEN`，无 `data`。
- admin missing：`NOT_FOUND`。
- 不回显目标 id、learnerId、questionId 或 courseId 到错误消息。

## 6. Architecture Drift Check

| Check | Expected |
|---|---|
| Backend layering | Controller delegates to Service |
| Security | Authorization in backend service |
| Agent / RAG | No change |
| API / DB | SPEC documents new API, no schema change |
| Dependency | No new dependency |

## 7. Tests

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
