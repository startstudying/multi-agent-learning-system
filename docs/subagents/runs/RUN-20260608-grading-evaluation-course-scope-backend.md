# RUN-20260608 Grading Evaluation Course Scope - Backend Expert

## 1. 当前行为

- `POST /api/assessment/grading-evaluations` 由 `AssessmentController` 委托 `GradingEvaluationService.evaluate(currentUserId, request)`。
- `GradingEvaluationService` 当前只允许 `admin`、`teacher`、`teacher_*`，student 返回 `FORBIDDEN`。
- `GradingEvaluationRequest` 当前无 `courseId`，只有 `samples` 或 legacy score arrays。
- `CourseAccessService.requireCourseRead(...)` 已集中实现 admin / teacher-own-course / student active enrollment course read scope。

## 2. 最小改动方案

- 给 `GradingEvaluationRequest` 增加 `courseId`。
- HTTP service path 强制 teacher/admin 提供 `courseId`。
- 在 `GradingEvaluationService.evaluate(currentUserId, request)` 中调用 `CourseAccessService.requireCourseRead(currentUserId, courseId)`。
- 通过 `KnowledgePointRepository.findByCourseIdOrderByCreatedAtAsc(courseId)` 校验 sample `knowledgePointId` 属于请求课程。

## 3. 破坏面

- 既有 controller teacher 成功测试需补 `courseId` 和 course/knowledge seed。
- `GradingEvaluationServiceTest` 若 service 构造器变更需适配；建议保留无参构造器用于纯指标计算。
- legacy score array HTTP 调用也必须补 `courseId`。

## 4. 建议测试

- teacher own-course success。
- teacher missing `courseId` -> `VALIDATION_ERROR`。
- teacher foreign/missing course -> `FORBIDDEN`。
- admin missing course -> `NOT_FOUND`。
- student with course -> `FORBIDDEN`。
- sample KP outside course -> `VALIDATION_ERROR`。
