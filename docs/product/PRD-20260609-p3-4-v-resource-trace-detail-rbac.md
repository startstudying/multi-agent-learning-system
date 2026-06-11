# PRD - P3-4-V ResourceGeneration / Agent Trace detail roles-first RBAC

## 背景

P3-4 已将多个 HTTP 主路径迁移到 Bearer roles-first RBAC，但 ResourceGeneration 详情与 Agent Trace detail/search 仍只向 Service 传递 `currentUserId()`，部分 Service 继续通过 `userId == "admin"` 推断管理员身份。

这会造成两个问题：

- Bearer `USER sub=admin` 可能被当成管理员读取他人的资源生成任务或 Trace。
- Bearer `ADMIN sub=ops_admin` 这类真实管理员身份无法按角色获得应有的 detail/search 访问能力。

## 目标

在不改变 API path、请求/响应 DTO、数据库 schema、前端和模型/RAG 运行时的前提下，使目标读路径只信任 `UserContext.roles()` 派生出的显式角色事实。

## 用户价值

- 关闭 `sub=admin` 角色混淆导致的越权读取。
- 保持真实 Bearer `ADMIN` 账号对治理数据的可观测能力。
- 保持非管理员 missing/foreign 对象的安全 anti-enumeration 语义。

## 非目标

- 不引入 formal OAuth2/JWK/Spring Security。
- 不修改 ResourceGeneration create/retry/review 发布状态机。
- 不开放 admin cancel 他人任务能力。
- 不清理全项目 legacy overload。
- 不新增依赖、DB migration 或 frontend 变更。
