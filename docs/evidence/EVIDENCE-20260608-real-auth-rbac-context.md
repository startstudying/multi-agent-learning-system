# EVIDENCE - P3-4-I 真实认证上下文与 RBAC 兼容层

## 1. 追踪

- PRD: `docs/product/PRD-20260608-real-auth-rbac-context.md`
- REQ: `docs/requirements/REQ-20260608-real-auth-rbac-context.md`
- SPEC: `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
- PLAN: `docs/plans/PLAN-20260608-real-auth-rbac-context.md`
- TASK: `docs/tasks/TASK-20260608-real-auth-rbac-context.md`
- Context Pack: `docs/context/CONTEXT-20260608-real-auth-rbac-context.md`
- 日期: 2026-06-08

## 2. 实现内容

本切片将认证上下文根从纯 `X-User-Id` 过渡为 Bearer JWT 优先：

- 新增 `AuthProperties`，通过 `learning-os.auth.jwt-secret` / `learning-os.auth.issuer` 绑定配置。
- `application.yml` 映射 `AUTH_JWT_SECRET` 和 `AUTH_JWT_ISSUER`，不保存真实 secret。
- `DevAuthFilter` 保留类名以降低集成变更，但行为升级为统一认证过滤器：
  - 有 `Authorization: Bearer` 时校验 HS256 JWT。
  - valid token 建立 `UserContext(sub, name/sub, roles)`。
  - invalid token 返回固定 `UNAUTHORIZED`，不 fallback 到 `X-User-Id`。
  - `dev/test` 无 token 时继续兼容 `X-User-Id` 或 `dev_user`。
  - `prod/staging/production` 无 token 时返回 `UNAUTHORIZED`。
- `CurrentUserService` 新增 `currentUser()`，`currentUserId()` 保持兼容。
- `isAdmin()` / `isTeacherUser()` 优先使用 roles；仅 dev/test 允许旧 userId 字符串推断。

未新增 Maven 依赖、未新增 DB migration、未修改 frontend、未修改业务对象级授权服务。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/config/AuthProperties.java` | 新增 | 认证配置绑定，包含 `jwtSecret` / `issuer` 默认值。 |
| `backend/src/main/java/com/learningos/LearningOsApplication.java` | 修改 | 注册 `AuthProperties`。 |
| `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java` | 修改 | Bearer JWT 校验、prod 禁用 header fallback、dev/test 兼容路径。 |
| `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java` | 修改 | `currentUser()` 与 roles 优先 RBAC helper。 |
| `backend/src/main/resources/application.yml` | 修改 | 新增 `learning-os.auth.*` 环境变量映射。 |
| `backend/src/test/java/com/learningos/common/auth/DevAuthFilterTest.java` | 修改 | 覆盖 Bearer 优先、invalid 不 fallback、prod header 禁用。 |
| `backend/src/test/java/com/learningos/common/auth/CurrentUserServiceTest.java` | 修改 | 覆盖 roles 驱动和 dev/prod 兼容差异。 |

## 4. TDD RED 证据

先增加测试后运行：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest test
```

首次结果：

- BUILD FAILURE
- 编译失败符合预期：
  - `AuthProperties` 不存在。
  - `CurrentUserService(AppProperties)` 构造器不存在。
  - `CurrentUserService.currentUser()` 不存在。

该 RED 证明测试确实指向本切片新增合同，而不是已有行为。

## 5. GREEN / 回归验证

### 5.1 聚焦测试

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest test
```

最终结果：

- BUILD SUCCESS
- Tests run: 13
- Failures: 0
- Errors: 0
- Skipped: 0

### 5.2 相邻回归

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,StructuredRequestLoggingFilterTest,CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest test
```

最终结果：

- BUILD SUCCESS
- Tests run: 74
- Failures: 0
- Errors: 0
- Skipped: 0

### 5.3 全量后端测试

```powershell
cd D:\多元agent\backend
mvn test
```

最终结果：

- BUILD SUCCESS
- Tests run: 345
- Failures: 0
- Errors: 0
- Skipped: 1
- 完成时间：2026-06-08 20:11 Asia/Shanghai

## 6. 安全与架构检查

- 生产环境不再通过 `X-User-Id` 建立身份。
- Bearer token 一旦出现，所有环境都必须校验；invalid token 不 fallback。
- 认证失败响应固定为 `UNAUTHORIZED`，不返回 token、secret、签名或 raw exception。
- JWT secret 仅通过配置/env 注入；代码和文档未写入真实 secret。
- 业务对象级授权矩阵未迁移到 Controller，也未重写 P3-4-C..H 服务层逻辑。
- 无新依赖、无 schema drift、无 frontend 改动。

## 7. 结论

P3-4-I 真实认证上下文与 RBAC 兼容层已完成并通过验证。
