# RUN-20260610-p3-4-resource-course-permission-matrix-expansion-security

## 角色

Security Reviewer

## 范围

只读分析 P3-4 下一切片 RBAC 测试矩阵，覆盖 Course/Knowledge Catalog、Analytics、ResourceGeneration、Review、Agent Trace。

## 结论

风险等级：MEDIUM，主要是测试矩阵缺口；未确认当前 HTTP 主路径存在必须立即修复的生产越权。

推荐大小：S。

## 已覆盖矩阵摘要

- Course/Knowledge Catalog 已覆盖 Bearer admin、spoofed `X-User-Id`、teacher no-prefix own-course、student spoof admin、`USER sub=admin` / `USER sub=teacher_*` role-confusion 等主要读写路径。
- Analytics 已覆盖 overview/token/ops admin-only、student summary、class summary、teacher own-course、ACTIVE enrollment 聚合、Bearer roles-first 与 header spoofing。
- ResourceGeneration detail/create 已覆盖 non-admin missing/foreign、Bearer admin detail spoof、`USER sub=admin` detail/create 拒绝、admin 不能代创建、forbidden create 无副作用。
- Review Gate 已覆盖 admin spoof、teacher own-course no-prefix、`USER sub=admin/teacher_*` 拒绝、teacher own-course list/decision、foreign/missing decision anti-enumeration。
- Agent Trace 已覆盖 governance search/admin spoof、`USER sub=admin` 拒绝、detail/admin spoof、detail role-confusion、admin missing `404`。

## 高价值缺口

1. ResourceGeneration `learner-resources` 缺少 Bearer foreign/missing anti-enumeration 成对测试。
2. Course/Knowledge dependency 写路径缺少 `USER sub=teacher_1` 对 `/api/knowledge-dependencies` 的 role-confusion 测试。
3. Agent task cancel 没有 roles-first/admin-spoof 回归；如果产品要求 admin/teacher 特权取消，则需要生产代码修复。若 cancel 设计为 owner-only，可先补测试。
4. Course create 缺少 `USER sub=admin` cannot create/manage 的显式测试。
5. Analytics student summary 可增加 Bearer student owner-only + spoofed admin header 测试。
6. Review list 可增加 Bearer teacher no-prefix own/foreign 混合 fixture，断言只返回 own-course review。

## 推荐最小测试

- `learnerResourcesBearerStudentCannotDistinguishMissingTaskFromForeignTask`
- `knowledgeDependencyRejectsBearerUserSubjectTeacherPrefixForSubjectOwnedCourse`
- `agentTaskCancelRejectsBearerUserSubjectAdminRoleConfusion`
- `courseCreateRejectsBearerUserSubjectAdminRoleConfusion`
- `studentSummaryBearerStudentWithSpoofedAdminHeaderRemainsOwnerOnly`
- `reviewListBearerTeacherNoPrefixRedactsForeignCourseReviews`

## 生产修复判断

本切片优先补测试，暂不建议直接改生产代码。HTTP 主路径大多已从 `UserContext.roles()` 派生权限。若新测试暴露真实越权，再升级为 M。
