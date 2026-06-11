# SPEC - P3-4-Q Analytics student summary roles-first RBAC

## 1. 追踪

- PRD: `docs/product/PRD-20260609-p3-4-q-analytics-student-summary-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-q-analytics-student-summary-rbac.md`
- Subagent reports:
  - `docs/subagents/runs/RUN-20260609-p3-4-q-analytics-student-summary-backend.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-q-analytics-student-summary-security.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-q-analytics-student-summary-test.md`

## 2. 现状

当前 `AnalyticsController.studentSummary(...)` 已调用：

```java
analyticsService.studentSummary(
        currentUserService.currentUserId(),
        currentUserService.isAdmin(),
        currentUserService.isTeacherUser(),
        learnerId,
        courseId
);
```

`AnalyticsService.studentSummary(...)` 接收了 `currentUserAdmin/currentUserTeacher`，但课程读取仍在 `requireCourseReadForStudentSummary(...)` 中调用：

```java
courseAccessService.requireCourseRead(currentUserId, courseId);
```

该旧签名会回落到 `admin` / `teacher_*` subject-name inference，丢失 Bearer role facts。

## 3. API Contract

本切片不新增、不删除、不修改 API path、request DTO 或 response DTO。

| API | Contract Change | Authorization Change |
|---|---|---|
| `GET /api/analytics/students/{learnerId}/summary` | 无 | global summary 行为不变 |
| `GET /api/analytics/students/{learnerId}/summary?courseId=...` | 无 | course read 使用 explicit admin/teacher role facts |

## 4. Service Contract

### 4.1 目标调用

`AnalyticsService.requireCourseReadForStudentSummary(...)` 必须调用：

```java
courseAccessService.requireCourseRead(
        currentUserId,
        currentUserAdmin,
        currentUserTeacher,
        courseId
);
```

### 4.2 保留语义

- `ADMIN` role：可读取任意已有课程；missing course 返回 `NOT_FOUND`。
- `TEACHER` role：仅可读取 `Course.teacherId == currentUserId` 的课程；missing/foreign course 返回 `FORBIDDEN`。
- 普通用户/学生：仅可读取自己 active enrolled course；missing/foreign/not-enrolled course 返回 `FORBIDDEN`。
- 教师读取学生摘要仍必须满足 learner active enrolled in course。
- 不改变 student summary 聚合过滤逻辑。

## 5. Authorization Matrix

| Scenario | Expected |
|---|---|
| Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id=alice` 读取 `course_backend` 下 `alice` summary | `OK` |
| Bearer `ADMIN sub=ops_admin` 读取 missing course | `NOT_FOUND` |
| Bearer `TEACHER sub=instructor_1` 读取 `Course.teacherId=instructor_1` 且 learner active enrolled | `OK` |
| Bearer `USER sub=teacher_1` 读取 `Course.teacherId=teacher_1` 但自身未 enrollment 的 course-scoped self summary | `FORBIDDEN` |
| Bearer `TEACHER sub=instructor_1` 读取 missing course | `FORBIDDEN` |
| Bearer `TEACHER sub=instructor_1` 读取 foreign course | `FORBIDDEN` |

## 6. Persistence / Dependency / Frontend

- DB migration：无。
- 新依赖：无。
- Frontend：无变更。
- API DTO：无变更。

## 7. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 传递身份事实；Service 执行 course authorization |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime |
| Security | PASS | 无 secrets；无 dependency |
| API / Database | PASS | 无 API/DB contract change |
