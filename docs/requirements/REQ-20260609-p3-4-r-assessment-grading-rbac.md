# REQ - P3-4-R Assessment / GradingEvaluation roles-first RBAC

## 1. Skill Selection Report

### Task Type

Bug fix / security hardening：Assessment read paths 与 GradingEvaluation HTTP path 从 legacy subject-name inference 迁移到 roles-first RBAC。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目强制 PRD/REQ/SPEC/PLAN/TASK/CONTEXT 到 Evidence/Acceptance 的闭环 |
| `auth-context-boundary` | Bearer JWT、`UserContext.roles()`、spoofed `X-User-Id` 与 legacy fallback 边界 |
| `object-scope-authorization` | answer/wrong-question/course scope、IDOR、防枚举响应 |
| `test-driven-development` | 先写 RED 回归测试，再做最小实现 |
| `security-review` | 校验 Broken Access Control、role-confusion 和 anti-enumeration 风险 |
| `test-generator` | 设计 Bearer admin/teacher/user 的回归矩阵 |
| `architecture-drift-check` | 确认无 API/DB/frontend/dependency drift |
| `verification-before-completion` | 完成声明前必须有 fresh verification evidence |

### Missing Skills

无。

### GitHub Research Needed

No。项目已有 auth/context、CourseAccessService role-aware overload 和相邻 P3-4-M/O/P/Q 样板。

### New Project-Specific Skill To Create

暂不创建。后续多个 roles-first 切片稳定后，可沉淀“roles-first controller/service authorization migration”技能。

## 2. Functional Requirements

| ID | Requirement | Priority | Acceptance |
|---|---|---|---|
| REQ-P3-4-R-01 | `AssessmentController` 读路径和 grading evaluation 必须从 `UserContext.roles()` 派生 role facts | P0 | HTTP 主路径不只传 `currentUserId` |
| REQ-P3-4-R-02 | Bearer `ADMIN sub=ops_admin` 必须能读取 existing assessment detail/list 并运行 existing-course grading evaluation | P0 | Controller tests 返回 `OK` |
| REQ-P3-4-R-03 | Bearer `TEACHER sub=instructor_1` 必须能读取 `Course.teacherId=instructor_1` 下 active enrolled learner 的 answer/wrong-question，并运行 grading evaluation | P0 | Controller tests 返回 `OK` |
| REQ-P3-4-R-04 | Bearer `USER sub=admin` 不能获得 admin Assessment/Grading 权限 | P0 | 相关 tests 返回 `FORBIDDEN` |
| REQ-P3-4-R-05 | Bearer `USER sub=teacher_1` 不能获得 teacher Assessment/Grading 权限 | P0 | 相关 tests 返回 `FORBIDDEN` |
| REQ-P3-4-R-06 | Assessment 非管理员 missing/foreign answer/wrong-question 继续统一 `FORBIDDEN` 且无 `data` | P0 | existing/new tests 覆盖 |
| REQ-P3-4-R-07 | GradingEvaluation teacher/non-admin missing/foreign course 继续统一 `FORBIDDEN`，admin missing 保持 `NOT_FOUND` | P0 | existing/new tests 覆盖 |
| REQ-P3-4-R-08 | 不新增 DB/API/frontend/dependency 变更 | P0 | 文件变更审查通过 |

## 3. Non-Functional Requirements

- 授权逻辑位于 backend service 层。
- Controller 只负责提取当前用户和 role facts。
- 不写入真实 secret、API key、raw token 或原始敏感日志。
- legacy overload 可保留用于兼容，但 HTTP 主路径必须使用 roles-first overload。
