# SPEC-20260608 P3-4-D Course Enrollment Scope

## 1. 数据模型

新增表：

```sql
course_enrollment(
  id varchar(80) primary key,
  course_id varchar(80) not null,
  learner_id varchar(120) not null,
  status varchar(40) not null default 'ACTIVE',
  created_at datetime(6) not null,
  updated_at datetime(6) not null,
  unique key uk_course_enrollment_course_learner(course_id, learner_id),
  key idx_course_enrollment_learner_status(learner_id, status),
  key idx_course_enrollment_course_status(course_id, status)
)
```

状态：

- `ACTIVE`
- `DROPPED`

本切片只消费状态，不提供 enrollment 管理 API。

## 2. Service 边界

新增 `CourseAccessService`：

```java
Course requireCourseRead(String currentUserId, String courseId)
void requireCourseManage(String currentUserId, Course course)
void requireLearnerEnrolledForExistingCourse(String currentUserId, String learnerId, String courseId)
List<String> listActiveLearnerIds(String courseId)
List<String> listActiveCourseIdsForLearner(String learnerId)
boolean isExistingCourse(String courseId)
```

角色规则沿用当前过渡模型：

- admin: `currentUserId == "admin"`
- teacher: `currentUserId == "teacher"` 或 `teacher_` 前缀
- student: 其他用户

## 3. API 行为

### Course read

| User | List | Detail / Graph |
|---|---|---|
| admin | 全部 | 任意存在 course；missing 为 `NOT_FOUND` |
| own teacher | 自己课程 | 自己课程 |
| foreign teacher | 不含 foreign | foreign/missing 为 `FORBIDDEN` |
| enrolled student | active enrolled course | active enrolled course |
| unenrolled student | 不含 course | foreign/missing 为 `FORBIDDEN` |

### Learning Path

当 `goalId` 是存在的 courseId：

- admin 可为任意 learner 创建。
- learner 本人必须 active enrolled。
- foreign learner owner check 仍优先拒绝。

当 `goalId` 不是 courseId：保持现有 goal template 行为。

### Resource Generation

当 `goalId` 是存在的 courseId：

- learner 本人必须 active enrolled。
- 当前 resource generation 仍不扩大 admin 代创建能力。

当 `goalId` 不是 courseId：保持现有行为。

### Teacher Class Summary

- teacher/admin 读取权限仍按 course owner/admin。
- learner set 改为 `course_enrollment.status == ACTIVE`。
- learning path 只作为 enrolled learner 的学习信号，不再决定班级成员。

## 4. 错误语义

| 场景 | ErrorCode |
|---|---|
| student 未 enrollment 读 course/detail/graph | `FORBIDDEN` |
| student 为未 enrollment course 创建 path/resource | `FORBIDDEN` |
| non-admin missing/foreign course detail/graph | `FORBIDDEN` |
| admin missing course | `NOT_FOUND` |

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 不写归属规则；Service 执行权限。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 不改 Agent Tool/RAG 检索链路。 |
| Security | PASS | 权限在后端代码中执行，不依赖 Prompt。 |
| API / Database | PASS | 新增 schema 有 migration/spec/test；不新增 undocumented endpoint。 |
