# REQ-20260608 P3-4-E Assessment Record RBAC Matrix

## 1. Functional Requirements

### REQ-1 Answer detail endpoint

新增：

```http
GET /api/assessment/answers/{answerId}
```

返回答题详情安全 DTO。

### REQ-2 Wrong question detail endpoint

新增：

```http
GET /api/assessment/wrong-questions/{wrongQuestionId}
```

返回错题详情安全 DTO，并复用 assessment record 授权语义。

### REQ-3 Student scope

普通学生只能读取 `learnerId == currentUserId` 的 answer / wrong question。

### REQ-4 Teacher scope

教师只能读取自己课程范围内、且 learner 为 active enrollment 的 answer / wrong question。

课程推导规则：

```text
questionId
-> AssessmentFeedbackService.resolveKnowledgePointId(questionId)
-> KnowledgePoint.courseId
-> CourseAccessService teacher-owned course check / active enrollment check
```

若无法推导到已有课程，教师不得读取该记录。

### REQ-5 Admin scope

`admin` 可读取任意已存在 answer / wrong question；不存在时返回 `NOT_FOUND`。

### REQ-6 Anti-enumeration

非 admin 读取 missing / foreign answerId 或 wrongQuestionId 时统一：

```json
{
  "code": "FORBIDDEN",
  "data": null
}
```

实际响应中 `data` 不存在。

### REQ-7 Safe response

响应不得包含：

- `requestHash`
- `responseJson`
- `requestId`
- `LearningEvent.payloadJson`
- 内部异常、SQL、堆栈、provider 原文错误

## 2. Non-functional Requirements

- 不新增依赖。
- 不修改前端。
- 不新增 DB migration。
- Service 层完成授权，Controller 不写对象归属规则。
- Focused / adjacent / full backend Maven 验证需写入 Evidence。

## 3. Out of Scope

- 列表接口和分页过滤。
- course-scoped grading evaluation。
- 真实 JWT/RBAC claim。
- 给 assessment 记录冗余 `courseId`。
