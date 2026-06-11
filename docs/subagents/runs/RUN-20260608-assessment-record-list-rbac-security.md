# RUN-20260608 Assessment Record List RBAC / Pagination Security Review

## 1. Scope

P3-4-F 预审范围为 assessment answer / wrong-question 分页列表接口、课程/学生对象范围、IDOR 防护、列表敏感字段泄露和分页枚举风险。

## 2. Conclusion

风险等级：MEDIUM。

当前 answer / wrong-question detail RBAC 已基本到位。P3-4-F 的主要风险是 list/pagination 如果复用 `findAll()` 或 detail DTO，会扩大跨学生枚举、批量敏感字段暴露和空结果语义混乱。本切片可以推进，但必须使用 list 专用 DTO、分页上限、teacher 强制 `courseId` scope，以及 service 层授权查询。

## 3. Key Evidence

- 当前 `AssessmentController` 只有提交、详情和 grading evaluation 端点，尚无 answer/wrong-question list API。
- `AssessmentService` detail RBAC 已覆盖：
  - student owner-only。
  - teacher own-course + active enrollment learner。
  - admin global。
  - non-admin missing/foreign 统一 `FORBIDDEN`。
- `CourseAccessService` 已提供 `requireCourseRead(...)`、`listActiveLearnerIds(...)`、`listActiveCourseIdsForLearner(...)`。
- `course_enrollment` 已有 `(course_id, learner_id)` 唯一键和 learner/status、course/status 索引。

## 4. Findings

### 4.1 List API 需要把 scope 写入查询条件

Severity: HIGH。

新增 `GET /api/assessment/answers?learnerId=...` 或 `GET /api/assessment/wrong-questions?courseId=...` 时，不能先全量加载再过滤，也不能允许 student/teacher 任意指定 `learnerId`。

### 4.2 List DTO 不应复用详情 DTO

Severity: MEDIUM。

answer list 不应返回：

- `answer`
- `requestId`
- `requestHash`
- `responseJson`
- `payloadJson`
- raw provider errors

wrong-question list 不应返回内部 payload / raw response。列表只返回 summary 字段。

### 4.3 空结果语义需要明确

推荐并在本切片采纳的语义：

| 场景 | 语义 |
|---|---|
| student 默认查询自己的列表，无记录 | `OK`, `items=[]`, `totalElements=0` |
| student 显式查询 `learnerId != currentUserId` | `FORBIDDEN` |
| teacher 未传 `courseId` | `VALIDATION_ERROR` |
| teacher 查询他人课程或不存在课程 | `FORBIDDEN` |
| teacher 查询自己课程但指定未 enrolled learner | 空 page，避免 learner 存在性枚举 |
| admin 查询 missing course | `NOT_FOUND` |

## 5. Required Tests

- student answer/wrong-question list owner-only。
- student 显式跨 `learnerId` 返回 `FORBIDDEN`。
- teacher 必须带 `courseId`。
- teacher own-course active learner 有结果，foreign course 返回 `FORBIDDEN`。
- teacher 指定未 enrolled learner 返回空 page。
- admin 可全局与按 learner/course 过滤。
- `page < 0`、`size < 1`、`size > 50` 返回 `VALIDATION_ERROR`。
- list 响应不包含 `answer`、`requestId`、`requestHash`、`responseJson`、`payloadJson`。

## 6. Dependency / Schema

本切片不需要新增依赖或 migration。使用 Spring Data JPA `Pageable` 即可。
