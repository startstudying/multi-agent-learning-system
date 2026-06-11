# REQ-20260608 P3-4-I 真实认证上下文与 RBAC 兼容层

## 1. 功能需求

| ID | Requirement |
|---|---|
| FR-01 | 后端必须支持 `Authorization: Bearer <jwt>` 建立 `UserContext`。 |
| FR-02 | JWT 必须校验签名、`sub`、`exp`，并从 `roles` claim 读取角色。 |
| FR-03 | `prod` / `production` / `staging` 环境不得从 `X-User-Id` 建立身份。 |
| FR-04 | `dev` / `test` 环境无 Bearer token 时继续支持 `X-User-Id`，缺失时回退 `dev_user`。 |
| FR-05 | 任意环境只要提供 Bearer token，就必须校验 token；invalid token 返回 `UNAUTHORIZED`，不得 fallback 到 `X-User-Id`。 |
| FR-06 | `CurrentUserService.currentUserId()` 必须返回可信上下文中的 userId。 |
| FR-07 | `CurrentUserService.isAdmin()` / `isTeacherUser()` 必须优先使用 roles；仅 dev/test 兼容模式允许旧 userId 字符串推断。 |
| FR-08 | 认证失败响应不得记录或返回 token 原文、secret、签名片段。 |

## 2. 非功能需求

| ID | Requirement |
|---|---|
| NFR-01 | 不新增 Maven 依赖。 |
| NFR-02 | 不新增 DB migration。 |
| NFR-03 | 不修改 frontend。 |
| NFR-04 | 不改变 P3-4-C..H 已有业务接口响应 DTO。 |
| NFR-05 | JWT secret 只能来自环境变量 / 配置，不得硬编码真实密钥。 |
| NFR-06 | 默认 dev/test 运行不需要外部认证服务。 |

## 3. 权限兼容矩阵

| 环境 | Bearer token | X-User-Id | 结果 |
|---|---|---|---|
| dev/test | absent | present | 兼容旧 header 身份 |
| dev/test | absent | absent | `dev_user` |
| dev/test | valid | 任意 | 使用 token 身份，忽略 header |
| dev/test | invalid | 任意 | `UNAUTHORIZED` |
| prod/staging | absent | 任意 | `UNAUTHORIZED` |
| prod/staging | valid | 任意 | 使用 token 身份，忽略 header |
| prod/staging | invalid | 任意 | `UNAUTHORIZED` |

## 4. 边界

- 本切片只替换认证上下文根，不重写业务授权服务。
- JWT 采用 HMAC-SHA256 的本地验证能力作为无依赖过渡；后续如接 OAuth2/JWK/Spring Security，必须另做依赖审查和迁移切片。
