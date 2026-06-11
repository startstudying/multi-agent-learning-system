# PRD - P3-4-R Assessment / GradingEvaluation roles-first RBAC

## 1. 问题陈述

Assessment 读路径和 GradingEvaluation HTTP 主路径仍依赖 legacy subject-name inference：

- `"admin"` 被推断为管理员。
- `"teacher"` / `teacher_*` 被推断为教师。
- `CourseAccessService.requireCourseRead(currentUserId, courseId)` 旧签名会再次触发上述推断。

这会导致两个相反问题：

- Bearer `ADMIN sub=ops_admin`、Bearer `TEACHER sub=instructor_1` 这类真实 role facts 无法稳定生效。
- Bearer `USER sub=admin`、Bearer `USER sub=teacher_1` 可能因 subject 名称混淆获得越权能力。

## 2. 目标用户

| 用户 | 需求 |
|---|---|
| 管理员 | 通过 `ADMIN` role 读取 Assessment 记录和运行 grading evaluation，不依赖 `sub=admin` |
| 教师 | 通过 `TEACHER` role 读取自有课程的 enrolled learner 记录和运行课程评估，不依赖 `teacher_` 前缀 |
| 普通用户/学生 | 不能通过 subject 名称伪装成 admin/teacher |
| 安全负责人 | 非管理员 missing/foreign 对象响应不形成存在性 oracle |

## 3. MVP 范围

纳入：

- `GET /api/assessment/answers`
- `GET /api/assessment/answers/{answerId}`
- `GET /api/assessment/wrong-questions`
- `GET /api/assessment/wrong-questions/{wrongQuestionId}`
- `POST /api/assessment/grading-evaluations`
- Assessment / GradingEvaluation service roles-first overload
- Bearer spoof、teacher no-prefix、subject role-confusion 回归测试

不纳入：

- `POST /api/assessment/answers` 提交答题写入链路
- API path / request DTO / response DTO 变更
- DB schema / migration
- frontend
- 新依赖
- formal OAuth2/JWK/Spring Security
- P3-4 整体完成声明

## 4. 成功指标

| 指标 | 目标 |
|---|---|
| Bearer admin spoofed header | Assessment/Grading 读写治理入口按 `ADMIN` role 通过 |
| Bearer teacher no-prefix | 自有课程 Assessment list/detail 和 GradingEvaluation 通过 |
| `USER sub=admin` | 不获得 admin 权限 |
| `USER sub=teacher_1` | 不获得 teacher 权限 |
| 非管理员 missing/foreign | 统一安全 `FORBIDDEN`，无 `data` |
| 架构漂移 | 无 API/DB/frontend/dependency drift |
