# PRD-20260609 P3-4-L class analytics roles-first course scope

## 1. 背景

P3-4-K 已完成当前 transitional 权限渗透矩阵，但 `backend-architecture-todolist.md` 仍保留 broader class/course 与 formal OAuth2/JWK/Spring Security 的后续项。

本切片聚焦一个可独立验收的 class/course 缺口：`GET /api/analytics/classes/{courseId}/summary` 仍只把 `currentUserId` 传入业务服务，未像 student summary 一样传递 `CurrentUserService.isAdmin()` / `isTeacherUser()`。这会导致 Bearer token 中可信的 `ADMIN` / `TEACHER` roles 在 class analytics 授权路径中丢失，并且非 admin missing course 与 foreign course 可能形成存在性探测差异。

## 2. 目标

- 让 teacher class analytics summary 使用 roles-first 授权语义。
- Bearer `ADMIN` role 可以读取任意 existing course class summary，即使 token `sub` 不是字面 `admin`。
- Bearer `TEACHER` role 只能读取 `token.sub == Course.teacherId` 的课程 class summary。
- Bearer `STUDENT` 即使 spoof `X-User-Id: admin` 也不能读取 teacher class summary。
- 非 admin missing course 与 foreign course 在 class summary 路径上统一返回安全 `FORBIDDEN`。
- admin missing course 保留 `NOT_FOUND` 运维语义。

## 3. 非目标

- 不引入 Spring Security / OAuth2 Resource Server / JWK / JWKS。
- 不新增依赖、schema、API path 或前端改动。
- 不重构完整 `CourseAccessService` RBAC 模型。
- 不设计正式班级域模型或教师-班级分配表。
- 不声明 P3-4 完全完成；broader class/course 与 formal OAuth2/JWK/Spring Security 仍为后续项。

## 4. 用户价值

- 缩小 P3-4-K 后仍残留的 class/course 权限缺口。
- 为后续正式认证迁移保留一条 roles-first 的业务端回归基线。
- 降低 class analytics 中 IDOR、header spoofing 和 courseId 枚举风险。

## 5. 验收摘要

- 新增 RED 测试覆盖 Bearer admin/teacher/student 与 missing/foreign class summary 场景。
- 最小生产修复只触及 analytics controller/service。
- 不新增依赖、schema、前端或模型/RAG 相关变更。
- Evidence / Acceptance 记录 focused、adjacent、full backend verification。
