# PRD - P3-4-Q Analytics student summary roles-first RBAC

## 1. 问题陈述

`GET /api/analytics/students/{learnerId}/summary?courseId=...` 已经在 Controller 层传入当前用户的 admin/teacher role facts，但 `AnalyticsService` 内部读取课程时仍调用 legacy `CourseAccessService.requireCourseRead(currentUserId, courseId)`。

该旧签名会重新通过 `currentUserId == "admin"` 或 `teacher_*` subject 前缀推断角色，导致 Bearer JWT roles-first 语义被丢失：

- Bearer `ADMIN sub=ops_admin` 可能无法读取课程维度学生摘要。
- Bearer `TEACHER sub=instructor_1` 即使拥有 `TEACHER` role，也会因为 subject 没有 `teacher_` 前缀而被拒绝。
- Bearer `USER sub=teacher_1` 可能被 legacy subject 前缀误判为 teacher。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 管理员 | `ADMIN` | 使用 Bearer role 读取任意已有课程下学生摘要，不受 spoofed `X-User-Id` 影响 |
| 教师 | `TEACHER` | 使用 Bearer role 读取自己课程中 active enrolled learner 的学生摘要，不依赖 `teacher_` subject 命名 |
| 普通用户/学生 | `USER` / `STUDENT` | 不能通过 `sub=admin` 或 `sub=teacher_1` 获得管理/教师权限 |

## 3. 用户故事

- 作为管理员，我希望 `ADMIN` role 是权限来源，而不是 subject 必须等于 `admin`。
- 作为教师，我希望 `TEACHER sub=instructor_1` 可以读取 `Course.teacherId=instructor_1` 的课程摘要。
- 作为安全负责人，我希望 `USER sub=teacher_1` 不能通过 subject 前缀读取未 enrollment 的课程维度学生摘要。
- 作为安全负责人，我希望非管理员对 missing/foreign course 的响应保持安全 `FORBIDDEN`，不形成课程存在性 oracle。

## 4. MVP 范围

### 纳入范围

- `GET /api/analytics/students/{learnerId}/summary?courseId=...`
- `AnalyticsService.requireCourseReadForStudentSummary(...)` 调用 role-aware `CourseAccessService.requireCourseRead(...)`。
- 新增 Bearer admin spoof、teacher no-prefix、subject role-confusion、missing/foreign anti-enumeration 回归测试。
- 工作流文档、证据、验收、记忆和 changelog 更新。

### 非目标

- 不修改 API path、request DTO 或 response DTO。
- 不修改 DB schema、migration 或依赖。
- 不修改 frontend。
- 不处理 Assessment / GradingEvaluation / LearningPath / ResourceGeneration 的 legacy 调用方；这些留作后续 P3-4-R/S。
- 不引入 formal OAuth2/JWK/Spring Security。
- 不声明 P3-4 或 backend architecture TODO 整体完成。

## 5. 成功指标

| 指标 | 目标值 | 衡量方式 |
|---|---|---|
| Bearer admin student summary | 通过 | Controller test |
| Bearer teacher no-prefix own-course | 通过 | Controller test |
| `USER sub=teacher_1` role-confusion 阻断 | 通过 | Controller test |
| Non-admin missing/foreign anti-enumeration | 通过 | Controller test |
| API/DB/frontend/dependency drift | 0 | 文件变更审查 |

## 6. 发布边界

本切片仅关闭 Analytics student summary 的一个 legacy `CourseAccessService` 调用点。P3-4 仍保留 broader class/course、其他 legacy caller、formal OAuth2/JWK/Spring Security 等后续工作。
