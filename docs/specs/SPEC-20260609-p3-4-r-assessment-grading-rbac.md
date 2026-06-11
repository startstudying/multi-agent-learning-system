# SPEC - P3-4-R Assessment / GradingEvaluation roles-first RBAC

## 1. Traceability

- PRD: `docs/product/PRD-20260609-p3-4-r-assessment-grading-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-r-assessment-grading-rbac.md`
- Subagent reports:
  - `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-backend.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-security.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-test.md`

## 2. Current State

`AssessmentController` 当前只调用 `currentUserService.currentUserId()`，`AssessmentService` 与 `GradingEvaluationService` 通过 subject 字符串推断 admin/teacher。

风险：

- Bearer role facts 丢失。
- `USER sub=admin/teacher_1` role-confusion。
- 非管理员 missing/foreign 防枚举语义可能被绕过。

## 3. API Contract

本切片不修改 API path、request DTO、response DTO。

| API | Contract Change | Authorization Change |
|---|---|---|
| `GET /api/assessment/answers` | 无 | roles-first list scope |
| `GET /api/assessment/answers/{answerId}` | 无 | roles-first detail scope |
| `GET /api/assessment/wrong-questions` | 无 | roles-first list scope |
| `GET /api/assessment/wrong-questions/{wrongQuestionId}` | 无 | roles-first detail scope |
| `POST /api/assessment/grading-evaluations` | 无 | roles-first admin/teacher gate and course read |

## 4. Service Contract

### 4.1 AssessmentService

新增 roles-first overload：

```java
listAnswers(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, ...)
listWrongQuestions(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, ...)
answerDetail(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String answerId)
wrongQuestionDetail(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String wrongQuestionId)
```

HTTP 主路径使用 explicit role facts。legacy overload 保留并委托：

```java
currentUserAdmin = isAdmin(currentUserId)
currentUserTeacher = isTeacherUser(currentUserId)
```

### 4.2 GradingEvaluationService

新增：

```java
evaluate(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, GradingEvaluationRequest request)
```

语义：

- `ADMIN` / `TEACHER` role 才可运行 HTTP grading evaluation。
- 普通用户先 `FORBIDDEN`，不得通过 `sub=admin/teacher_1` 提权。
- course read 使用 `CourseAccessService.requireCourseRead(currentUserId, admin, teacher, courseId)`。
- sample knowledge scope 校验保持不变。

## 5. Authorization Matrix

| Scenario | Expected |
|---|---|
| Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` 读 answer detail | `OK` |
| Bearer `USER sub=admin` 读 foreign answer detail/list | `FORBIDDEN` |
| Bearer `TEACHER sub=instructor_1` 读 own-course enrolled learner wrong-question/detail/list | `OK` |
| Bearer `USER sub=teacher_1` 读 `Course.teacherId=teacher_1` 记录 | `FORBIDDEN` |
| Bearer `ADMIN sub=ops_admin` 运行 existing-course grading evaluation | `OK` |
| Bearer `TEACHER sub=instructor_1` 运行 own-course grading evaluation | `OK` |
| Bearer `USER sub=admin` / `USER sub=teacher_1` 运行 grading evaluation | `FORBIDDEN` |
| Bearer admin missing grading course | `NOT_FOUND` |
| Bearer teacher missing/foreign grading course | `FORBIDDEN` |

## 6. Persistence / Dependency / Frontend

- DB migration：无。
- 新依赖：无。
- Frontend：无。
- API DTO：无。

## 7. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 提取身份事实，Service 执行授权 |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime |
| Security | PASS | 无 secrets；无 dependency |
| API / Database | PASS | 无 API/DB contract change |
