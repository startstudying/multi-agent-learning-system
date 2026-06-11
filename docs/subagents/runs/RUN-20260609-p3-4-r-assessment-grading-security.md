# RUN-20260609 P3-4-R Assessment / GradingEvaluation Security Review

## 范围

只读安全分析，未修改代码。

## 风险等级

HIGH。

## 摘要

`AssessmentController` 仍只向业务层传 `currentUserId`，`AssessmentService` / `GradingEvaluationService` 仍用 `"admin"`、`"teacher"`、`teacher_` 字符串推断角色。认证层已支持 Bearer 优先和 spoofed `X-User-Id` 忽略，但业务层未消费 `UserContext.roles()`，存在 Broken Access Control 与 role-confusion 风险。

## 高风险问题

### 1. Bearer roles-first 未贯穿 Assessment / GradingEvaluation

- 影响：Bearer `ADMIN sub=ops_admin` 可能被当普通用户；Bearer `TEACHER sub=instructor_1` 可能因 subject 无 `teacher_` 前缀被拒。
- 位置：`AssessmentController`、`AssessmentService`、`GradingEvaluationService`
- 修复：HTTP 主路径传入 explicit role facts。

### 2. `USER sub=admin` / `USER sub=teacher_1` 可触发 role-confusion

- 影响：普通用户可因 subject 字符串像 admin/teacher 而获得读权限或评估权限。
- 修复：HTTP 主路径不得从 subject 字符串推断角色。

### 3. missing-vs-foreign anti-enumeration 被 role-confusion 破坏

- 影响：非管理员可能通过 `404` vs `403` 探测 answerId、wrongQuestionId、courseId 是否存在。
- 修复：missing 语义使用 explicit `currentUserAdmin`，非管理员统一 `FORBIDDEN` 且无 `data`。

## 必须覆盖的 RED 测试

| 场景 | 期望 |
|---|---|
| Bearer admin + spoofed header 读 answer detail | 200 |
| Bearer teacher no-prefix 读 own-course answer/wrong-question/list | 200 |
| Bearer `USER sub=admin` 读 foreign answer/list 或 grading | 403 |
| Bearer `USER sub=teacher_1` 读 own-course teacher 对象或 grading | 403 |
| Bearer admin 运行 grading existing course | 200 |
| Bearer admin missing grading course | 404 |
| Bearer teacher missing/foreign grading course | 403 |

## 非目标

- 不引入 Spring Security / OAuth2 / JWK。
- 不修改数据库 schema、DTO 合同、前端页面或依赖。
- 不处理全局依赖 CVE；历史 dependency-check 报告中的 CVE 需独立治理。
