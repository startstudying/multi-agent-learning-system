# SPEC-20260609 P3-4-M Course API / CourseAccessService roles-first overload

## 1. Scope

本规格只覆盖 Course API / Knowledge Catalog 主路径的 roles-first overload 局部迁移。

覆盖入口：

- `POST /api/courses`
- `GET /api/courses`
- `GET /api/courses/{courseId}`
- `POST /api/courses/{courseId}/chapters`
- `GET /api/courses/{courseId}/knowledge-graph`
- `POST /api/knowledge-points`
- `POST /api/knowledge-dependencies`

## 2. Current Gap

当前调用链示例：

```text
CourseController.get(courseId)
-> knowledgeCatalogService.getCourseForUser(currentUserId, courseId)
-> courseAccessService.requireCourseRead(currentUserId, courseId)
-> isAdmin(currentUserId) / isTeacherUser(currentUserId)
```

问题：

- Controller 未传 `CurrentUserService.isAdmin()` / `isTeacherUser()`。
- `CourseAccessService` 使用 `"admin"` / `teacher_` 字符串推断角色。
- `KnowledgeCatalogService.createCourse(...)` 与 write helper 仍复制 legacy 判断。

## 3. Target Behavior

目标调用链：

```text
CourseController.get(courseId)
-> knowledgeCatalogService.getCourseForUser(currentUserId, currentUserAdmin, currentUserTeacher, courseId)
-> courseAccessService.requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId)
```

`KnowledgePointController` 写路径同理传入 role facts。

## 4. CourseAccessService Overload Contract

新增：

```java
Course requireCourseRead(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String courseId)
void requireCourseManage(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, Course course)
List<Course> listCoursesForUser(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher)
```

旧签名保留：

```java
Course requireCourseRead(String currentUserId, String courseId)
void requireCourseManage(String currentUserId, Course course)
List<Course> listCoursesForUser(String currentUserId)
```

旧签名只作为 legacy compatibility，并委托到新签名。

## 5. Authorization Rules

### Read / list

- `currentUserAdmin == true`：list 返回所有课程；detail/graph 可读取 existing course；missing course 返回 `NOT_FOUND`。
- `currentUserTeacher == true`：list 返回 `teacherId == currentUserId` 的课程；detail/graph 仅允许 own course。
- 其他用户：list 返回 active enrollment courses；detail/graph 仅允许 active enrollment course。
- non-admin missing 或 foreign course 统一返回 `FORBIDDEN`。

### Manage

- `currentUserAdmin == true`：可 manage existing course。
- `currentUserTeacher == true && currentUserId == Course.teacherId`：可 manage own course。
- student/user 即使 active enrollment 也不可 manage course graph。

### Course create

- admin 可创建课程，可指定 `teacherId`；未指定时使用 current user id。
- teacher 可创建自己名下课程；若传入 `teacherId`，必须等于 current user id。
- student/user 不可创建课程。

## 6. API Contract

API path、HTTP method、request DTO、response DTO 不变。

## 7. Test Matrix

| Test | Expected |
|---|---|
| Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id: alice` list courses | 200，返回所有课程 |
| Bearer `ADMIN sub=ops_admin` detail/graph existing course | 200 |
| Bearer `ADMIN sub=ops_admin` detail/graph missing course | 404 |
| Bearer `TEACHER sub=instructor_1` owns course with `teacherId=instructor_1` | detail/graph/chapter create 200 |
| Bearer `STUDENT sub=alice` + spoofed `X-User-Id: admin` foreign course | detail/graph/chapter create 403 |

## 8. Architecture Drift Check

| Check | Expected |
|---|---|
| Controller only handles HTTP/current user extraction | PASS |
| Service owns object authorization | PASS |
| Permission in backend code, not Prompt | PASS |
| No frontend change | PASS |
| No new dependency | PASS |
| No schema drift | PASS |

## 9. Out of Scope

- Formal OAuth2/JWK/Spring Security。
- 全仓库 `CourseAccessService` 调用方迁移。
- RAG KB / PromptVersion / Evaluation / Assessment full RBAC。
- DB schema、frontend、dependency 变更。

## 10. 实施状态

已完成并通过验证。后续 broader class/course、全仓库 roles-first 迁移和 formal OAuth2/JWK/Spring Security 仍保留为独立切片。
