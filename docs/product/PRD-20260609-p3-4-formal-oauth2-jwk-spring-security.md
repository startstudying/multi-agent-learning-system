# PRD-20260609 P3-4 子任务：formal OAuth2/JWK/Spring Security

## 背景

P3-4 已完成大量 roles-first 业务授权切片，但认证根边界仍是过渡实现：`DevAuthFilter` 手写 HS256 JWT 校验，并用 `UserContextHolder` 建立当前用户上下文。该方案已覆盖 Bearer 优先、invalid token 不 fallback、prod/staging 禁用 `X-User-Id` 等关键风险，但它不是正式 OAuth2/JWK/Spring Security Resource Server。

Spring Security 官方文档说明，JWT Resource Server 需要 OAuth2 Resource Server 与 JOSE/JWT 支持；Spring Boot 可通过 `issuer-uri` 或 `jwk-set-uri` 自动配置 JWT 校验，并用 `SecurityFilterChain` 配置 `oauth2ResourceServer().jwt(...)`。

## 目标

引入最小生产级 Spring Security OAuth2 Resource Server JWT/JWK 认证边界，替换生产/预发环境的手写 Bearer token 校验，同时保持业务授权 API 稳定。

## 用户价值

- 生产环境认证由标准 Spring Security JWT/JWK 资源服务器处理。
- 支持 JWK key rotation 和 issuer / audience 校验。
- 保留既有 roles-first 业务授权成果，避免重新设计业务权限矩阵。
- 继续防止 `X-User-Id` spoofing 和 subject-name role confusion。

## 非目标

- 不改变 REST API path、request/response DTO。
- 不新增数据库 migration。
- 不修改前端。
- 不重做 Course/RAG/Assessment/Agent 业务对象授权矩阵。
- 不实现登录、授权码流程、Refresh Token、用户注册登录体系。
- 不在代码或文档中保存真实私钥、secret、JWK 私钥。
- 不解决 SSE 生产认证的完整 signed stream token 方案；本轮只记录后续风险。

## 成功标准

- Maven 依赖中引入 Spring Security Resource Server 能力。
- 生产/预发 Bearer JWT 由 Spring Security `JwtDecoder` / Resource Server 处理。
- `CurrentUserService` 可从 Spring Security `Authentication` 建立项目 `UserContext`。
- dev/test 仍可在无 Bearer 时使用 `X-User-Id` 兼容路径。
- invalid Bearer 不 fallback。
- no Bearer in prod/staging 返回统一 `UNAUTHORIZED` envelope。
- focused / adjacent / full backend 测试通过，或明确记录无法运行原因。

