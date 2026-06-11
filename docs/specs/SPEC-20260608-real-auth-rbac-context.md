# SPEC-20260608 P3-4-I 真实认证上下文与 RBAC 兼容层

## 1. 配置

新增配置前缀：

```yaml
learning-os:
  auth:
    jwt-secret: ${AUTH_JWT_SECRET:}
    issuer: ${AUTH_JWT_ISSUER:learning-os}
```

规则：

- `learning-os.app.environment` 为 `prod` / `production` / `staging` 时，必须使用 Bearer JWT。
- `dev` / `test` 环境允许 `X-User-Id` fallback。
- `jwt-secret` 为空时，prod/staging 的 Bearer token 统一校验失败。

## 2. JWT 合同

Header：

```json
{ "alg": "HS256", "typ": "JWT" }
```

Payload：

| Claim | Required | Notes |
|---|---:|---|
| `sub` | yes | userId |
| `name` | no | displayName |
| `roles` | no | string array，例如 `["ADMIN"]` |
| `iss` | no | 非空时必须匹配配置 issuer |
| `exp` | yes | epoch seconds |

## 3. Filter 行为

统一由认证过滤器在 request 早期建立 `UserContext`：

```text
if Authorization starts with Bearer:
  validate JWT
  set UserContext(sub, name/sub, roles)
else if environment allows dev header:
  set UserContext(X-User-Id or dev_user, same displayName, derived dev roles)
else:
  return UNAUTHORIZED
```

认证失败响应：

```json
{ "code": "UNAUTHORIZED", "message": "Unauthorized", "data": null }
```

不得返回 token、secret、签名、raw exception。

## 4. CurrentUserService

- `currentUser()` 返回当前 `UserContext`，缺失时返回 dev user 兼容对象。
- `currentUserId()` 继续兼容现有 Controller / Service 签名。
- `isAdmin()`：优先判断 roles contains `ADMIN`；dev/test 兼容模式才允许 userId 为 `admin`。
- `isTeacherUser()`：优先判断 roles contains `TEACHER`；dev/test 兼容模式才允许 userId 为 `teacher` 或 `teacher_*`。

## 5. 不变行为

- 不修改业务 Controller API。
- 不修改 `CourseAccessService` / `AssessmentService` / `DocumentService` 的对象级授权矩阵。
- 不新增数据库 schema。
- 不新增 Maven 依赖。

## 6. 测试要求

- `DevAuthFilterTest`：
  - dev header 兼容。
  - dev missing header 回退 `dev_user`。
  - valid token 优先于 spoofed header。
  - invalid token 不 fallback。
  - prod missing token 返回 401。
  - prod valid student token + `X-User-Id: admin` 使用 student。
- `CurrentUserServiceTest`：
  - roles 驱动 admin/teacher。
  - dev 兼容旧 userId 规则。
  - non-dev 不用旧 userId 规则。
- 回归 P3-4-C..H 关键测试。
