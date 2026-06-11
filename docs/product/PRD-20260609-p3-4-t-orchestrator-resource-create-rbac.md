# PRD - P3-4-T Orchestrator RESOURCE_GENERATION create roles-first RBAC

## 1. 问题陈述

P3-4-S 已关闭 direct `POST /api/resources/generation-tasks` 的 roles-first create 缺口，但 Orchestrator `RESOURCE_GENERATION` workflow create 仍通过 legacy service path 进入 ResourceGeneration。该路径会基于 `userId == "admin"` 推断 admin，导致 Bearer `USER sub=admin` 可能绕过 course enrollment。

## 2. 目标用户

- 学生：只能为自己在已 enrolled course 或 template goal 下触发资源生成 workflow。
- 管理员 / 教师：本切片不新增为其他 learner 代创建 ResourceGeneration workflow 的能力。
- 平台运维：越权失败时仍可通过脱敏 failed workflow evidence 排查。

## 3. MVP 范围

- `POST /api/orchestrator/workflows` 的 `RESOURCE_GENERATION` create 使用 Bearer roles-first facts。
- `POST /api/orchestrator/workflows/{workflowId}/retry` 同步使用 roles-first facts。
- Bearer `USER sub=admin` 不能绕过 course enrollment。
- Bearer `ADMIN` 不能为其他 learner 创建 ResourceGeneration workflow。
- forbidden 不产生 ResourceGeneration/model/token/citation 业务副作用。

## 4. 非目标

- 不做 broader class/course 权限矩阵。
- 不引入 formal OAuth2/JWK/Spring Security。
- 不修改 API path、request DTO、response DTO、DB schema、frontend。
- 不处理 ResourceGeneration detail/trace/cancel/review RBAC。
- 不删除所有 legacy subject-name helper。

## 5. 成功指标

- 新增 TDD RED 能在旧实现上暴露 `USER sub=admin` Orchestrator bypass。
- focused、adjacent、full backend tests 通过。
- `backend-architecture-todolist.md` 只标记 P3-4-T follow-up 完成，不误标 P3-4 整体完成。

