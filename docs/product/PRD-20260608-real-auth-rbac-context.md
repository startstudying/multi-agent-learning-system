# PRD-20260608 P3-4-I 真实认证上下文与 RBAC 兼容层

## 1. 背景

P3-4-A..H 已经逐步收口课程、答题记录、Review Gate、RAG 文档元数据等高风险对象级权限，但当前身份来源仍是 `X-User-Id` 开发态请求头。任何客户端都可以伪造 `X-User-Id: admin` 或 `teacher_*`，从认证根上绕过已完成的服务层矩阵。

## 2. 目标

- 引入后端验证过的认证上下文，避免生产环境信任 `X-User-Id`。
- 保留 dev/test 的 `X-User-Id` 兼容模式，降低现有测试和本地开发迁移成本。
- 让 `CurrentUserService` 基于 roles 判断 admin/teacher，而不是只依赖 userId 字符串。
- 保持 P3-4-C..H 既有业务授权矩阵不回退。

## 3. 用户故事

| Actor | Story |
|---|---|
| admin | 作为管理员，我的全局权限来自后端验证过的 token role，而不是可伪造请求头。 |
| teacher | 作为教师，我只能通过可信 role + userId 执行 own-course 操作。 |
| student | 作为学生，即使伪造 `X-User-Id: admin` 也不能绕过生产认证。 |
| developer | 作为开发者，我在 dev/test 仍可用 `X-User-Id` 运行现有测试和本地调试。 |

## 4. 非目标

- 不引入新 Maven 依赖。
- 不接 OAuth2 Resource Server / Spring Security。
- 不新增登录接口、刷新 token、用户密码模型或前端登录页。
- 不新增数据库 schema。
- 不重写 P3-4-C..H 业务授权矩阵。

## 5. 成功标准

- prod/staging/production 环境无 Bearer token 请求返回 `UNAUTHORIZED`。
- prod/staging/production 环境 invalid Bearer token 返回 `UNAUTHORIZED`。
- prod/staging/production 环境 valid student token + `X-User-Id: admin` 仍按 student 身份执行。
- dev/test 环境无 Bearer token 时继续支持 `X-User-Id`。
- Bearer token 有效时 roles claim 驱动 `isAdmin()` / `isTeacherUser()`。
- P3-4-C..H 关键回归测试继续通过。
