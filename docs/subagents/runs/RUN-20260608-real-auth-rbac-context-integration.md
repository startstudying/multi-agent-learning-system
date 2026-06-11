# RUN-20260608 P3-4-I Real Auth RBAC Context Integration

## 1. 输入报告

- RAG Parser Architect：`docs/subagents/runs/RUN-20260608-p3-next-rag-parser-architect.md`
- Model Provider Expert：`docs/subagents/runs/RUN-20260608-p3-next-model-provider.md`
- Security & Quality：`docs/subagents/runs/RUN-20260608-p3-next-security-rbac.md`

## 2. 集成决策

本轮选择 `P3-4-I 真实认证上下文与 RBAC 兼容层` 作为下一最小切片。

理由：

- `backend-architecture-todolist.md` P3-4 剩余最高风险是临时 `X-User-Id` 可伪造身份和真实 JWT/RBAC 未落地。
- P3-3 真实模型 provider 需要新增依赖，必须先做 dependency/security review，不适合直接实现。
- P3-2 OCR / 真实页码也需要 parser/OCR 依赖审查；无依赖硬化只能 best-effort，不能完成 TODO 原语义。

## 3. 实现边界

采用无新增依赖方案：

- 使用 JDK HMAC-SHA256 校验 Bearer JWT，建立可信 `UserContext`。
- `dev/test` 环境继续兼容 `X-User-Id`，避免现有测试和本地开发大面积破坏。
- `prod/staging/production` 环境禁用 `X-User-Id` 身份建立，必须提供有效 Bearer JWT。
- 如果请求带 Bearer token，则即使在 `dev/test` 也必须校验 token；校验失败返回 `UNAUTHORIZED`。
- roles 来自 token `roles` claim；`CurrentUserService.isAdmin()` / `isTeacherUser()` 优先基于 roles，dev/test 无 roles 时才兼容旧字符串规则。

## 4. 不做事项

- 不引入 Spring Security / OAuth2 / jjwt 等新依赖。
- 不新增数据库 schema。
- 不改 frontend。
- 不重写所有 Controller 或业务授权矩阵。
- 不改变 P3-4-C..H 的 missing/foreign 防枚举语义。

## 5. 验证策略

聚焦：

```powershell
cd backend
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest test
```

相邻：

```powershell
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,StructuredRequestLoggingFilterTest,CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest test
```

全量：

```powershell
mvn test
```
