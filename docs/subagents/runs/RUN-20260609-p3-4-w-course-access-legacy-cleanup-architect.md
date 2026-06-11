# RUN - P3-4-W Architect Report

## 结论

当前源码中未发现外部主路径或测试直接调用 `CourseAccessService` legacy overload。P3-4-W 的最小安全边界应聚焦删除这些 overload 及其私有字符串角色推断辅助，不应扩成全仓 RBAC 重构。

## 可删除的 legacy overload

- `requireCourseRead(String currentUserId, String courseId)`
- `requireCourseManage(String currentUserId, Course course)`
- `requireLearnerEnrolledForExistingCourse(String currentUserId, String learnerId, String courseId)`
- `listCoursesForUser(String currentUserId)`

删除上述入口后，可同步删除：

- `scopedCourseMissing(String currentUserId)`
- `isAdmin(String currentUserId)`
- `isTeacherUser(String currentUserId)`

## 调用残留分类

真风险但不纳入 P3-4-W：

- `LearningWorkflowService.getPathForUser(String currentUserId, String pathId)` 仍有本地 `admin` subject-name 判断，建议后续独立切片处理。
- `AssessmentService`、`GradingEvaluationService`、`KnowledgeCatalogService` 等仍保留自身 legacy overload，但 HTTP Controller 已传 explicit role facts，不应混入本切片。

不是当前 CourseAccessService 风险：

- `DocumentService.upload(String userId, ...)` 旧入口默认 `false,false`，不是提权路径。
- `AnalyticsService`、`AssessmentService`、`ResourceGenerationService`、`LearningWorkflowService`、`DocumentService` 对 `courseAccessService.*` 的直接调用均为 roles-first 实参形态。

## 建议边界

允许修改：

- `CourseAccessService.java`
- `CourseAccessServiceTest.java`
- P3-4-W 文档与记忆文件

禁止触碰：

- 其他 service legacy cleanup
- `CurrentUserService` / `DevAuthFilter`
- Spring Security / OAuth2 / JWK
- DB schema、frontend、依赖

## 架构漂移风险

本切片删除已迁移完成的兼容 API 面，符合 roles-first 边界。若编译失败，应把调用点迁移到 roles-first overload，而不是恢复字符串推断。
