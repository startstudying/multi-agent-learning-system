# SPEC - P3-4-W CourseAccessService legacy overload cleanup

## 1. 范围

本切片只处理：

- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
- `backend/src/test/java/com/learningos/knowledge/application/CourseAccessServiceTest.java`

以及对应 workflow、evidence、acceptance、memory、changelog、TODO 文档。

## 2. 授权 API 最终形态

`CourseAccessService` 只保留显式 roles-first 公共入口：

```java
Course requireCourseRead(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String courseId)
void requireCourseManage(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, Course course)
void requireLearnerEnrolledForExistingCourse(String currentUserId, boolean currentUserAdmin, String learnerId, String courseId)
List<Course> listCoursesForUser(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher)
List<String> listActiveLearnerIds(String courseId)
List<String> listActiveCourseIdsForLearner(String learnerId)
boolean isExistingCourse(String courseId)
```

删除以下 legacy public overload：

```java
requireCourseRead(String currentUserId, String courseId)
requireCourseManage(String currentUserId, Course course)
requireLearnerEnrolledForExistingCourse(String currentUserId, String learnerId, String courseId)
listCoursesForUser(String currentUserId)
```

删除以下 subject-name inference helper：

```java
scopedCourseMissing(String currentUserId)
isAdmin(String currentUserId)
isTeacherUser(String currentUserId)
```

## 3. 行为保持

roles-first overload 行为不变：

| Case | Response / Behavior |
|---|---|
| explicit admin reads existing course | returns course |
| explicit admin reads missing course | throws `NOT_FOUND` |
| explicit teacher reads/manages own course | allowed |
| student with active enrollment reads course | allowed |
| ordinary user / foreign teacher / unenrolled student | throws `FORBIDDEN` |
| `currentUserAdmin=true` for existing course enrollment helper | bypasses enrollment only for callers that explicitly pass admin fact |

## 4. 测试策略

新增 `CourseAccessServiceTest`：

1. 反射测试：确认 legacy public overload 不存在。
2. 反射测试：确认 private subject-name inference helper 不存在。
3. 行为回归：roles-first `currentUserId="admin"` 但 `currentUserAdmin=false` 不获得 admin `NOT_FOUND` 语义。
4. 行为回归：roles-first `currentUserId="teacher_1"` 但 `currentUserTeacher=false` 不获得 teacher manage 权限。

RED 预期：新增反射测试在旧代码上失败，因为旧 overload/helper 仍存在。

## 5. Architecture Drift

| Check | Expected |
|---|---|
| Backend layering | PASS。Service 层继续执行课程授权，Controller 不变。 |
| Frontend | PASS。无 frontend 修改。 |
| Agent / RAG | PASS。不改 Agent/RAG/model runtime。 |
| Security | PASS。删除 subject-name inference API 面。 |
| API / Database | PASS。无 API/DB 变更。 |
