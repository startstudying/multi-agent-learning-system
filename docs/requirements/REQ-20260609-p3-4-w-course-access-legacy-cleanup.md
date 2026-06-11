# REQ - P3-4-W CourseAccessService legacy overload cleanup

## 功能需求

| ID | Requirement |
|---|---|
| R1 | `CourseAccessService` 不得继续暴露 `requireCourseRead(String currentUserId, String courseId)`。 |
| R2 | `CourseAccessService` 不得继续暴露 `requireCourseManage(String currentUserId, Course course)`。 |
| R3 | `CourseAccessService` 不得继续暴露 `requireLearnerEnrolledForExistingCourse(String currentUserId, String learnerId, String courseId)`。 |
| R4 | `CourseAccessService` 不得继续暴露 `listCoursesForUser(String currentUserId)`。 |
| R5 | `CourseAccessService` 不得保留私有 subject-name role inference helper：`isAdmin(String)`、`isTeacherUser(String)`、`scopedCourseMissing(String)`。 |
| R6 | 所有源码中对 `courseAccessService.requireCourseRead/requireCourseManage/requireLearnerEnrolledForExistingCourse/listCoursesForUser` 的调用必须使用 roles-first overload。 |
| R7 | 现有 HTTP 行为保持不变：Bearer `ADMIN` / `TEACHER` 仍通过 `UserContext.roles()` 授权，Bearer `USER sub=admin/teacher_1` 不得被提权。 |

## 质量需求

- 必须先写 RED 测试证明 legacy overload 仍暴露。
- 删除旧入口后必须运行 compile guard。
- 必须运行 focused、adjacent、full backend Maven 验证，或记录无法运行原因。
- 不新增依赖。
- 不修改 API path、DTO、DB schema、frontend。

## 安全需求

- 权限事实来源必须是显式 `currentUserAdmin/currentUserTeacher` 参数。
- 非 admin missing/foreign course 仍返回安全 `FORBIDDEN` 语义。
- Admin missing course 仍返回 `NOT_FOUND`。
- 不在文档或代码中写入 secret、token、私密日志。
