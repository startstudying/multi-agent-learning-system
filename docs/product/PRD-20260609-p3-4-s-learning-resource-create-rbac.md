# PRD - P3-4-S LearningPath / ResourceGeneration course-bound create roles-first RBAC

## 1. 问题陈述

P3-4-D 已为 course-bound learning path 和 resource generation create 增加 active enrollment 约束；P3-4-I/M/R 等后续切片已将多条 HTTP 主路径迁移到 roles-first auth context。

当前 P3-4-S 发现两个 create 主路径仍残留 legacy subject-name inference：

- `POST /api/learning-paths` 只传 `currentUserId`，`LearningWorkflowService` 用 `userId == "admin"` 判断 admin。
- `POST /api/resources/generation-tasks` 保持 owner-only，但 course-bound enrollment helper 会把 `currentUserId == "admin"` 作为 admin bypass。

这会导致：

- Bearer `ADMIN sub=ops_admin` 的真实 admin role facts 丢失。
- Bearer `USER sub=admin` 可能被误当 admin。
- ResourceGeneration 可在 `USER sub=admin` + `learnerId=admin` 场景绕过 active enrollment，并产生 Agent/Resource/Review/Trace 等持久化副作用。

## 2. 用户价值

| 用户 | 价值 |
|---|---|
| 管理员 | 可通过显式 `ADMIN` role 创建 learner learning path，不再依赖 `sub=admin` |
| 学生 | course-bound resource generation 只允许自身且已 active enrolled，避免被 subject-name 混淆绕过 |
| 安全负责人 | 阻断 `USER sub=admin` / spoofed `X-User-Id` 带来的 Broken Access Control |
| 后端维护者 | Controller/Service 边界与 P3-4-M/R roles-first 模式一致 |

## 3. MVP 范围

纳入：

- `POST /api/learning-paths`
- `POST /api/resources/generation-tasks`
- `CourseAccessService.requireLearnerEnrolledForExistingCourse(...)` roles-first overload
- LearningPath / ResourceGeneration create controller tests
- ResourceGeneration 权限失败前置副作用回归

不纳入：

- ResourceGeneration detail / trace / cancel / learner-resource / review RBAC
- `POST /api/assessment/answers`
- DB schema / migration
- API path / request DTO / response DTO
- frontend
- 新 dependency
- formal OAuth2/JWK/Spring Security
- Agent/RAG/model provider/review gate runtime 行为调整

## 4. 成功指标

| 指标 | 目标 |
|---|---|
| LearningPath Bearer admin | `ADMIN sub=ops_admin` + spoofed header 可按 admin 语义创建 course-bound path |
| LearningPath role-confusion | `USER sub=admin` 不能跨 learner 或绕过 enrollment |
| ResourceGeneration owner-only | `ADMIN` / `TEACHER` 不新增代创建能力 |
| ResourceGeneration enrollment | `USER sub=admin` 不能绕过 course active enrollment |
| Side effects | ResourceGeneration forbidden create 不写 task/resource/review/trace/model-call |
| Drift | 无 API/DB/frontend/dependency drift |

