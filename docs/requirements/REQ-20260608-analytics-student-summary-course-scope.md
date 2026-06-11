# REQ-20260608 学生分析摘要课程范围权限收口

## 1. Functional Requirements

| ID | Requirement | Priority |
|---|---|---|
| REQ-1 | `GET /api/analytics/students/{learnerId}/summary` 支持可选查询参数 `courseId`。 | P0 |
| REQ-2 | student/普通用户只能读取 `learnerId == currentUserId` 的摘要。 | P0 |
| REQ-3 | student/普通用户提供 `courseId` 时，必须 active enrolled 该课程。 | P0 |
| REQ-4 | teacher / `teacher_*` 读取学生摘要时必须提供 `courseId`。 | P0 |
| REQ-5 | teacher / `teacher_*` 只能读取自己课程内 active enrolled learner 的课程内摘要。 | P0 |
| REQ-6 | admin 可读取任意 learner 全局摘要；admin 提供 existing `courseId` 时读取课程内摘要。 | P0 |
| REQ-7 | course-scoped 摘要只聚合该课程内 learning path node、mastery record、wrong question 信号。 | P0 |
| REQ-8 | 越权响应不得包含目标 learner、course、knowledge point、path、wrong question 的对象细节。 | P0 |

## 2. API Contract

```http
GET /api/analytics/students/{learnerId}/summary?courseId={courseId}
```

| Parameter | Required | Notes |
|---|---|---|
| `learnerId` | Yes | path variable，目标学生。 |
| `courseId` | No | teacher 必填；student/admin 可选。非空时启用课程内摘要。 |

## 3. Authorization Matrix

| Actor | No `courseId` | Own/existing course | Foreign/missing course | Foreign learner |
|---|---|---|---|---|
| student | own global summary | own enrolled course summary | `FORBIDDEN` | `FORBIDDEN` |
| teacher / `teacher_*` | `VALIDATION_ERROR` | own-course active-enrolled learner summary | `FORBIDDEN` | `FORBIDDEN` |
| admin | any learner global summary | any existing course scoped summary | `NOT_FOUND` | allowed |

## 4. Course Scope Rules

当 `courseId` 非空：

1. 通过 `CourseAccessService.requireCourseRead(...)` 校验课程读取权限。
2. teacher 还必须确认 `learnerId` 位于 `CourseAccessService.listActiveLearnerIds(courseId)`。
3. student 通过 `requireCourseRead(currentUserId, courseId)` 间接证明 active enrollment。
4. 摘要聚合仅保留：
   - `LearningPath.goalId == courseId` 且 learner 匹配的 path node；
   - `KnowledgePoint.courseId == courseId` 对应的 mastery record；
   - `KnowledgePoint.courseId == courseId` 对应的 wrong question。

## 5. Error Semantics

| Scenario | HTTP | code | data |
|---|---:|---|---|
| student reads another learner | 403 | `FORBIDDEN` | absent |
| teacher missing `courseId` | 400 | `VALIDATION_ERROR` | absent |
| teacher foreign/missing course | 403 | `FORBIDDEN` | absent |
| teacher learner not active enrolled | 403 | `FORBIDDEN` | absent |
| admin missing course | 404 | `NOT_FOUND` | absent |

## 6. Non-functional Requirements

- 不新增依赖。
- 不新增 DB migration。
- 不修改前端。
- Controller 只读取当前用户上下文与请求参数；授权与聚合边界放在 Service。
- 测试必须覆盖 RED/GREEN 和相邻 analytics/course 权限回归。
