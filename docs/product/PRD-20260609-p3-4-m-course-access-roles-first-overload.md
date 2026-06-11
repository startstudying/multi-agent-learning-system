# PRD-20260609 P3-4-M Course API / CourseAccessService roles-first overload

## 1. 背景

P3-4-I 已建立 Bearer JWT 到 `UserContext.roles()` 的 transitional auth context，P3-4-K / P3-4-L 已验证部分业务端点可以使用 roles-first 授权。但 Course API 主路径仍只把 `currentUserId()` 传入 `KnowledgeCatalogService` / `CourseAccessService`，服务层继续用 `"admin"`、`"teacher"`、`teacher_` 字符串推断角色。

这会导致 Bearer `ADMIN` subject 不是 `admin` 时被错误降权，也会让 Course API 的授权行为与认证层的 roles-first 设计脱节。

## 2. 目标

- 为 `CourseAccessService` 增加 roles-first overload，并保留旧签名兼容已验收调用方。
- 让 Course API 主路径从 Controller 向 Application Service 传入 `currentUserId + isAdmin + isTeacherUser`。
- 让 `GET /api/courses`、`GET /api/courses/{courseId}`、`GET /api/courses/{courseId}/knowledge-graph` 使用 Bearer role-derived admin/teacher facts。
- 让 course graph 写路径 `POST /api/courses/{courseId}/chapters`、`POST /api/knowledge-points`、`POST /api/knowledge-dependencies` 使用 roles-first manage scope。
- 保持 admin missing course 为 `NOT_FOUND`，non-admin missing/foreign course 为安全 `FORBIDDEN`。

## 3. 非目标

- 不引入 Spring Security / OAuth2 Resource Server / JWK / JWKS。
- 不新增依赖、DB schema、API path、DTO 或前端改动。
- 不迁移全仓库所有 `CourseAccessService` 调用方。
- 不修改 Assessment、RAG Document、Learning Workflow、Resource Generation 等已验收切片。
- 不声明 P3-4 或 broader class/course 权限矩阵完全完成。

## 4. 用户价值

- 缩小 Course API 与认证上下文之间的权限断裂。
- 为后续 formal OAuth2/JWK/Spring Security 迁移保留业务层 roles-first 回归基线。
- 降低 Course list/detail/graph/manage 路径中的 IDOR、header spoofing、角色混淆和 courseId 枚举风险。

## 5. 验收摘要

- 新增 RED 测试覆盖 Bearer admin、Bearer teacher、spoofed `X-User-Id`、missing/foreign course。
- 最小生产修复只触及 knowledge API / application service。
- Focused、adjacent、full backend Maven verification 需要记录到 Evidence / Acceptance。

## 6. 当前状态

本切片已实现并完成验证，相关 Evidence / Acceptance / Retro / Memory 已补齐。
