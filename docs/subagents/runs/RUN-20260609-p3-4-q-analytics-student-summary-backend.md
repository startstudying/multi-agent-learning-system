# RUN-20260609-p3-4-q-backend-analysis

## Summary

P3-4 仍未完成；当前剩余风险集中在旧 `CourseAccessService` 签名和部分业务 Service 自行用 `admin` / `teacher_*` 推断角色。最适合作为 P3-4-Q 的最小切片是：收口 `GET /api/analytics/students/{learnerId}/summary?courseId=...` 的 student summary course scope，使其把 Controller 已有的角色事实传入 `CourseAccessService` role-aware overload，不改 DB、依赖、前端或 API 合同。

## Analysis

1. `CourseAccessService` 仍保留 legacy 签名与 subject-name role inference。`requireCourseRead(String currentUserId, String courseId)` 会调用 `isAdmin(currentUserId)` / `isTeacherUser(currentUserId)` 推断角色；`requireCourseManage(String, Course)` 和 `listCoursesForUser(String)` 也一样。实际推断逻辑仍是 `"admin"` 与 `"teacher"` / `"teacher_"` 前缀。role-aware overload 已存在，能直接承接显式角色事实。

2. P3-4-M/P 已明确只完成主路径，未完成全部旧调用方。待办文档仍标记“其他旧 `CourseAccessService` 调用方、broader class/course 与正式 OAuth2/JWK/Spring Security”未完成。

3. 最小且清晰的 P3-4-Q 候选：Analytics student summary roles-first CourseAccess 调用。`AnalyticsController.studentSummary` 已经把 `currentUserId/isAdmin/isTeacherUser` 传入 Service。`AnalyticsService.studentSummary(...)` 也已接收这些 role facts。但内部 `requireCourseReadForStudentSummary(...)` 仍调用旧签名 `courseAccessService.requireCourseRead(currentUserId, courseId)`。这会让 Bearer `TEACHER sub=instructor_1` 这类非 `teacher_` subject 在已拥有 `TEACHER` role 的情况下仍被旧签名拒绝，属于 P3-4-M 后遗留的精确补口。

4. 该切片已有 API/Service 边界和测试承载点。API 边界是现有 `GET /api/analytics/students/{learnerId}/summary`，不需要新增路径或 DTO。现有测试已有 student summary course-scope 场景，但主要使用 `X-User-Id: teacher/admin` 兼容身份；同一测试类已有 JWT helper 和 Bearer role 用例基础。因此可加 RED：Bearer `TEACHER sub=instructor_1` 访问自己课程内已 enrollment 学生应 200；Bearer `USER sub=teacher_1` 或 Bearer `STUDENT sub=admin` + spoofed header 应 403。

5. 其他 legacy 调用方存在，但不适合作为本次最小 Q 首选。`LearningWorkflowService.createPathForUser` 和 `ResourceGenerationService.createTask` 会牵涉创建工作流、资源生成副作用或 path owner 语义，单次切片风险高于 analytics student summary。

6. Assessment 旧调用面更宽，不建议塞进 Q。`AssessmentService` answer/wrong-question list/detail 多处调用旧 `requireCourseRead`，并有 `"admin"` 字面调用；`GradingEvaluationService.evaluate(currentUserId, request)` 仍用 `isAdmin/isTeacherUser` 和旧 `requireCourseRead`。这些涉及答题记录矩阵和 grading API，适合单独后续切片，避免一次改动跨多条权限矩阵。

## Root Cause

根因不是缺少 centralized course access，而是 P3-4-M 只新增了 role-aware overload，并迁移了 Course/KnowledgeCatalog 主路径；若调用方仍只传 `currentUserId`，就会回落到 `CourseAccessService` 或业务 Service 内部的 legacy subject-name inference。认证层已经要求 roles-first 且 legacy 推断只应限于 dev/test 兼容，但业务服务旧签名无法表达“Bearer role facts”。

## Recommendations

1. P3-4-Q 首选：`Analytics student summary CourseAccess roles-first补口` - 小 - 高影响。修改 `AnalyticsService.requireCourseReadForStudentSummary(...)` 调用 role-aware `courseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId)`，并在 `AnalyticsControllerTest` 增加 Bearer teacher no-prefix / role-confusion 回归。文件边界清晰，无 API/DB/依赖/前端变更。

2. P3-4-R 后续：`LearningPath course-bound create roles-first enrollment补口` - 中 - 中高影响。为 `LearningPathController -> LearningWorkflowService.createPathForUser` 传入 admin/teacher facts，并给 `CourseAccessService.requireLearnerEnrolledForExistingCourse` 增加 role-aware overload。注意该路径还包含 learner owner/admin bypass 语义。

3. P3-4-S 后续：`ResourceGeneration course-bound create roles-first enrollment补口` - 中 - 中高影响。为 `ResourceGenerationController -> ResourceGenerationService.createTask/createTaskInWorkflow` 传入 role facts 或明确保持 learner-only。该路径有模型调用、任务创建和 review gate 副作用，测试要先确认权限拦截发生在 side effect 前。

## Suggested Modify / Do Not Modify

建议修改文件：

- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- 对应中文 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/EVIDENCE/ACCEPT/CHANGELOG/MEMORY 文档

禁止修改文件：

- DB migration、实体 schema、repository contract
- 前端文件
- 依赖配置、Spring Security/OAuth2/JWK 配置
- RAG retrieval runtime、parser/vector/index worker
- `CourseAccessService` 全面删除 legacy 签名

## Trade-offs

| Option | Pros | Cons |
|---|---|---|
| Q = Analytics student summary roles-first补口 | 最小、已有 role facts、测试承载成熟、无副作用 | 只能关闭一个旧调用点，不能声明 P3-4 完成 |
| Q = LearningPath/ResourceGeneration enrollment补口 | 更贴近学习闭环创建路径 | 涉及创建副作用和 owner/admin 语义，切片更宽 |
| Q = Assessment 全矩阵迁移 | 关闭最多 legacy 调用 | 范围过大，容易混入 answer list/detail/grading 多条矩阵 |
