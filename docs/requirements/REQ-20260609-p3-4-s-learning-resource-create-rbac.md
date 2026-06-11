# REQ - P3-4-S LearningPath / ResourceGeneration course-bound create roles-first RBAC

## 1. Skill Selection Report

### Task Type

Bug fix / security hardening。目标是将 LearningPath 与 ResourceGeneration course-bound create 主路径从 legacy subject-name inference 收口到 roles-first RBAC。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目强制从 Project Memory 到 Acceptance / Memory Update 的闭环 |
| `auth-context-boundary` | Bearer JWT、`UserContext.roles()`、spoofed `X-User-Id`、legacy fallback 边界 |
| `object-scope-authorization` | course enrollment scope、owner-only、IDOR 与持久化前授权 |
| `test-driven-development` | 先写 RED tests，再做最小实现 |
| `security-review` | 验证 Broken Access Control / role-confusion 风险 |
| `test-generator` | 设计 Bearer admin/user subject-confusion 回归矩阵 |
| `architecture-drift-check` | 确认无 API/DB/frontend/dependency drift |
| `verification-before-completion` | 完成声明前必须有 fresh verification evidence |

### Missing Skills

无。

### GitHub Research Needed

No。项目已有 `UserContext`、roles-first controller/service overload 模式，以及 `CourseAccessService` role-aware read/manage/list 模式。

### New Project-Specific Skill To Create

暂不创建。后续可将多次 P3-4 roles-first 迁移沉淀为 “roles-first authorization migration” 项目技能。

## 2. Functional Requirements

| ID | Requirement | Priority | Acceptance |
|---|---|---|---|
| REQ-P3-4-S-01 | `LearningPathController.create` 必须从 `UserContext.roles()` 派生 role facts | P0 | HTTP 主路径不再只传 `currentUserId` |
| REQ-P3-4-S-02 | `LearningWorkflowService.createPathForUser` 必须支持 roles-first overload | P0 | explicit `currentUserAdmin` 驱动 admin 语义 |
| REQ-P3-4-S-03 | Bearer `ADMIN sub=ops_admin` 可为任意 learner 创建 course-bound learning path，并忽略 spoofed `X-User-Id` | P0 | controller test `OK` |
| REQ-P3-4-S-04 | Bearer `USER sub=admin` 不能跨 learner 或绕过 course enrollment 创建 learning path | P0 | controller test `FORBIDDEN` 且无 path/node/event |
| REQ-P3-4-S-05 | `ResourceGenerationController.create` 必须从 `UserContext.roles()` 派生 role facts | P0 | HTTP 主路径使用 roles-first service overload |
| REQ-P3-4-S-06 | ResourceGeneration create 保持 owner-only，不新增 admin/teacher 代创建能力 | P0 | Bearer `ADMIN` 为其他 learner 创建返回 `FORBIDDEN` |
| REQ-P3-4-S-07 | Bearer `USER sub=admin` 不能绕过 ResourceGeneration course enrollment | P0 | `FORBIDDEN` 且无 task/resource/review/trace/model-call 副作用 |
| REQ-P3-4-S-08 | `CourseAccessService` 必须提供 roles-first enrollment helper | P0 | HTTP 主路径不调用 legacy enrollment helper |
| REQ-P3-4-S-09 | 不新增 DB/API/frontend/dependency/formal auth 变更 | P0 | 文件审查与测试通过 |

## 3. Non-Functional Requirements

- Controller 只提取当前用户和 role facts。
- Service 层执行 owner/enrollment 授权。
- 权限失败响应不暴露 course/task/path/trace 等对象线索。
- 不写入 secrets、API keys、raw token 或敏感日志。
- legacy overload 可保留兼容，但 HTTP 主路径必须使用 roles-first overload。

