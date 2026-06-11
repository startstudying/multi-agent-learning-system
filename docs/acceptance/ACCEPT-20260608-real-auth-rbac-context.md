# ACCEPT - P3-4-I 真实认证上下文与 RBAC 兼容层

## 1. 追踪

- PRD: `docs/product/PRD-20260608-real-auth-rbac-context.md`
- REQ: `docs/requirements/REQ-20260608-real-auth-rbac-context.md`
- SPEC: `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
- PLAN: `docs/plans/PLAN-20260608-real-auth-rbac-context.md`
- TASK: `docs/tasks/TASK-20260608-real-auth-rbac-context.md`
- Evidence: `docs/evidence/EVIDENCE-20260608-real-auth-rbac-context.md`

## 2. 验收清单

### 功能验收

- [x] `Authorization: Bearer <jwt>` 可以建立 `UserContext`。
- [x] JWT 校验覆盖 `alg=HS256`、签名、`sub`、`exp`、可选 `iss`。
- [x] token `roles` claim 可驱动 `ADMIN` / `TEACHER` 判断。
- [x] valid token 优先于 spoofed `X-User-Id`。
- [x] invalid token 返回 `UNAUTHORIZED`，不 fallback 到 `X-User-Id`。
- [x] prod/staging/production 无 token 返回 `UNAUTHORIZED`。
- [x] dev/test 无 token 时继续支持 `X-User-Id` 与 `dev_user`。
- [x] `CurrentUserService.currentUserId()` 保持现有签名兼容。
- [x] `CurrentUserService.currentUser()` 返回当前 `UserContext` 或 dev fallback。

### 非功能验收

- [x] 未新增 Maven 依赖。
- [x] 未新增 DB migration。
- [x] 未修改 frontend。
- [x] 未修改业务 Controller API DTO。
- [x] 未提交真实 secret / token。
- [x] 认证失败响应不包含 token、secret、签名片段或 raw exception。

### 架构验收

- [x] 认证上下文位于 `common/auth` filter/service 边界。
- [x] 业务对象级授权仍保留在 Service 层。
- [x] P3-4-C..H 关键回归测试通过。
- [x] 无 Agent/RAG/model provider/parser/vector 业务漂移。

### 文档验收

- [x] Evidence 已创建。
- [x] Acceptance 已创建。
- [x] PLAN / TASK 已更新。
- [x] Memory 已更新。
- [x] Changelog 已更新。
- [x] Retrospective 已创建。
- [x] Project-specific skill 已创建并注册。

## 3. 测试摘要

| 测试项 | 结果 | 备注 |
|---|---|---|
| RED 验证 | PASS | 新测试首次运行编译失败，缺少 `AuthProperties` / `currentUser()` 等新增合同。 |
| 聚焦测试 | PASS | `mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest test`，13 tests，0 failures/errors。 |
| 相邻回归 | PASS | `mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,StructuredRequestLoggingFilterTest,CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest test`，74 tests，0 failures/errors。 |
| 后端全量测试 | PASS | `mvn test`，345 tests，0 failures，0 errors，1 skipped。 |

## 4. 遗留问题

| 问题 | 严重程度 | 后续 TASK |
|---|---|---|
| 当前是无依赖 HS256 过渡 JWT 校验，不是完整 OAuth2/JWK/Spring Security 资源服务器。 | Medium | P3-4 后续正式认证 provider / Spring Security 切片，需要 dependency review。 |
| broader class/course 和全量 teacher/student/admin 渗透测试仍未完全闭环。 | Medium | P3-4 broader class/course permission matrix。 |

## 5. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过

## 6. 签字

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | Accepted |
