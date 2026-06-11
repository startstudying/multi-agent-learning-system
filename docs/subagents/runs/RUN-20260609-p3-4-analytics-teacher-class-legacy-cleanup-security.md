# RUN-20260609 P3-4 子任务：Analytics teacherClassSummary legacy cleanup - Security

## 角色

Security Reviewer / 权限与信任边界审查。

## 风险评级

MEDIUM。

## 风险摘要

`AnalyticsService.teacherClassSummary(String, String)` legacy overload 原先可通过 `currentUserId = "admin"`、`"teacher"` 或 `"teacher_*"` 推断权限。当前 HTTP path 已是 roles-first，但服务层残留入口会成为后续误用风险，属于 role confusion / broken access control 的潜伏面。

## 必须保护行为

- 只有显式 `currentUserAdmin = true` 才有 admin class summary 语义。
- 只有显式 `currentUserTeacher = true` 且 `currentUserId == Course.teacherId` 才有 teacher own-course 语义。
- `USER sub=admin`、`USER sub=teacher_1`、spoofed `X-User-Id` 不得获得 admin/teacher 语义。
- Non-admin missing/foreign course 保持安全 `FORBIDDEN`。
- 班级 learner 集合只能来自 active course enrollment，不从 learning path 等学习信号推断。

## 建议

- 删除两参 overload 与 legacy helper。
- 删除 `courseAccessService == null` 授权 fallback。
- 将 `classLearnerIds(...)` null fallback 改为 `Set.of()`。
- 增加 reflection guard 与 service-level behavior regression tests。

