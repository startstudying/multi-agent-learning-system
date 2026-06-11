# SPEC-20260608 P3-4-G Grading Evaluation Course Scope

## 1. Scope

本切片收口：

```http
POST /api/assessment/grading-evaluations
```

从临时 teacher/admin 角色门禁升级为 course-scoped evaluation。

## 2. API Contract

### 2.1 Request

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

legacy score array request remains accepted only when `courseId` is present:

```json
{
  "courseId": "course_sql",
  "humanScores": [1.0, 0.7],
  "aiScores": [0.96, 0.72],
  "agreementThreshold": 0.05
}
```

### 2.2 Success response

响应指标字段沿用 `SPEC-20260606-grading-quality-evaluation.md`，不新增响应字段。

## 3. Authorization Matrix

| Actor | Missing `courseId` | Own/existing course | Foreign course | Missing course |
|---|---|---:|---:|---:|
| student | `FORBIDDEN` | `FORBIDDEN` | `FORBIDDEN` | `FORBIDDEN` |
| teacher / `teacher_*` | `VALIDATION_ERROR` | 200 | `FORBIDDEN` | `FORBIDDEN` |
| admin | `VALIDATION_ERROR` | 200 | 200 | `NOT_FOUND` |

执行顺序：

1. 先拒绝 student，避免 student 通过缺失字段或 course 状态探测接口。
2. teacher/admin 再校验 `courseId` 必填。
3. 调用 `CourseAccessService.requireCourseRead(currentUserId, courseId)`。
4. 校验 sample `knowledgePointId` 是否属于 course。
5. 计算原有指标。

## 4. Sample Course Consistency

校验规则：

```text
courseId
-> KnowledgePointRepository.findByCourseIdOrderByCreatedAtAsc(courseId)
-> allowedKnowledgePointIds
-> every non-blank sample.knowledgePointId must be in allowedKnowledgePointIds
```

- 空白 `knowledgePointId` 仍进入 `UNKNOWN` 分组。
- 非空但不在本课程知识点集合中返回 `VALIDATION_ERROR`。
- 不区分 foreign KP 与 missing KP，避免细粒度对象枚举。

## 5. Implementation Design

修改：

- `GradingEvaluationRequest`
  - 增加 `String courseId` 字段。
  - 保留既有构造器重载，避免纯指标单测破坏。
- `GradingEvaluationService`
  - 注入 `CourseAccessService` 和 `KnowledgePointRepository`。
  - `evaluate(currentUserId, request)` 负责 role gate、course read gate、sample course consistency。
  - `evaluate(request)` 继续只做指标计算。
- `AssessmentController`
  - 保持 Controller 只委托 service。
- `AssessmentControllerTest`
  - 增加 course-scoped 权限矩阵和样本混入测试。
- `GradingEvaluationServiceTest`
  - 如构造器变化，改为传 mock 或使用指标-only 构造器。

不修改：

- `backend/pom.xml`
- DB migration
- frontend
- RAG / Agent / model provider

## 6. Error Semantics

| Scenario | HTTP | `code` | `data` |
|---|---:|---|---|
| student request | 403 | `FORBIDDEN` | absent |
| teacher/admin missing `courseId` | 400 | `VALIDATION_ERROR` | absent |
| teacher foreign/missing course | 403 | `FORBIDDEN` | absent |
| admin missing course | 404 | `NOT_FOUND` | absent |
| sample KP outside course | 400 | `VALIDATION_ERROR` | absent |

## 7. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller delegates; Service owns authorization and validation. |
| Frontend rules | PASS | No frontend changes. |
| Agent / RAG rules | PASS | No Agent/RAG/model path changes. |
| Security | PASS | Permission check is backend code; no prompt-based authorization. |
| API / Database | PASS | API request contract documented; no DB schema change. |

## 8. Tests

Focused:

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest test
```

Adjacent:

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
```

Full:

```powershell
cd backend
mvn test
```
