# RUN - P3-4-W Security Review

## 结论

当前 `main` HTTP 主路径中，已定位的 `CourseAccessService.requireCourseRead/requireCourseManage/listCoursesForUser/requireLearnerEnrolledForExistingCourse` 直接调用基本都已使用 role-aware overload。未发现仍由 Bearer `USER sub=admin` 或 `USER sub=teacher_1` 直接触发 CourseAccess admin/teacher 语义的 HTTP 主路径。

剩余风险集中在 legacy overload 本身仍按 subject 字符串推断权限；未来调用方误用即可恢复 `sub=admin/teacher_1` 提权。

## 风险

### Medium - Legacy overload 仍保留 subject-name 提权语义

受影响入口：

```java
requireCourseRead(String currentUserId, String courseId)
requireCourseManage(String currentUserId, Course course)
requireLearnerEnrolledForExistingCourse(String currentUserId, String learnerId, String courseId)
listCoursesForUser(String currentUserId)
```

问题：

- `isAdmin()` 只判断 `"admin"`。
- `isTeacherUser()` 判断 `"teacher"` 或 `teacher_` 前缀。
- 未来 HTTP/service 若误用旧签名，Bearer `roles=["USER"], sub="admin"` 会被当作 admin；`roles=["USER"], sub="teacher_1"` 会被当作 teacher。

## HTTP 路径现状

已审计的主路径基本使用 roles-first facts：

- Course / Knowledge graph
- Analytics student/class summary
- Assessment list/detail
- Grading evaluation
- LearningPath create
- ResourceGeneration create
- Orchestrator `RESOURCE_GENERATION`
- RAG document metadata scope

## 安全要求

- 新代码禁止调用 legacy overload。
- `CourseAccessService` 公共授权入口必须要求显式 `currentUserAdmin/currentUserTeacher` 参数。
- 非 admin missing/foreign course 继续折叠为 `FORBIDDEN`。
- Admin missing course 继续返回 `NOT_FOUND`。
- `X-User-Id` dev/test fallback 和 formal OAuth2/JWK 不纳入本切片。

## 验证建议

- 用 reflection 测试锁定 legacy overload/helper 不存在。
- 用 `rg` 证明源码中所有 `courseAccessService.*` 调用都是 roles-first。
- 运行 compile guard 与 controller adjacent 权限矩阵。
