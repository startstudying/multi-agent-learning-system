# RUN-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion-security

## 专家角色

Security Reviewer

## 范围

- `AssessmentControllerTest`
- `AssessmentServiceTest`
- `AssessmentController`
- `AssessmentService`
- `CourseAccessService`
- answer / wrong-question repository 与 domain

## 结论

风险等级：LOW-MEDIUM。

当前生产代码未发现必须先修的 RBAC 漏洞。主要缺口是 answer record / wrong-question 的 Bearer、header spoofing、subject-name role-confusion 渗透矩阵还没有覆盖到所有高价值路径。

## 已确认的安全前提

- `AssessmentController` 从 `UserContext.roles()` 派生显式 `ADMIN` / `TEACHER` fact，再传入 service。
- `AssessmentService` 的 answer/wrong-question list/detail 已使用 roles-first 签名。
- teacher 读取依赖 `CourseAccessService.requireCourseRead(...)` 与 active enrollment。
- 非 admin 的 missing/foreign detail 路径合并为 `FORBIDDEN`，避免对象存在性枚举。
- `CourseAccessService` 只保留显式 role fact 入口，目标 legacy subject-name helper 已清理。

## 建议补齐的测试缺口

1. Answer list：Bearer admin + spoofed `X-User-Id` 成功路径。
2. Wrong-question list：Bearer teacher no-prefix 成功路径。
3. Wrong-question list：Bearer `USER sub=admin` 与 `USER sub=teacher_*` role-confusion 拒绝路径。
4. Answer detail：Bearer `USER sub=teacher_*` role-confusion 拒绝路径。
5. Wrong-question detail：Bearer admin + spoofed `X-User-Id` 成功路径。
6. Wrong-question detail：Bearer `USER sub=admin` role-confusion 拒绝路径。
7. Student Bearer + spoofed admin/teacher header：answer/wrong-question list/detail 代表路径。
8. Teacher owns course 但 learner enrollment 非 ACTIVE 时，detail 读取拒绝。

## 是否需要生产代码修复

当前不需要。若新增测试暴露失败，再升级为 M 并进行最小生产修复。

## 备注

专家指出 answer record 当前没有直接 `courseId`，仍通过 `questionId -> knowledgePointId -> courseId` 或 wrong-question persisted `knowledgePointId` 推导 scope。该设计与当前项目约定一致，不作为本切片修复目标；长期 schema 标准化可另开任务。
