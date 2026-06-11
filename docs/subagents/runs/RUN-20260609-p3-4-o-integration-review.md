# RUN-20260609 P3-4-O Integration Review

## 1. Inputs

- Backend Expert: 推荐 roles-first Controller -> Service 参数边界。
- Security & Quality: 标记 Broken Access Control 为 HIGH，并要求覆盖 spoof / role-confusion / IDOR oracle。
- Test Engineer: 推荐 Controller RED tests 作为主断言，复用 `PromptVersionControllerTest` JWT helper。

## 2. Conflict Resolution

| Topic | Options | Decision |
|---|---|---|
| Service 是否保留 legacy subject 推断 | 保留兼容 overload / 迁移到 role facts | HTTP 管理主路径迁移到 role facts，避免 Bearer 场景被 userId 字符串污染 |
| 是否引入 Spring Security/OAuth2 | 引入正式资源服务器 / 继续项目内兼容层 | 不引入新依赖；formal OAuth2/JWK/Spring Security 留作后续切片 |
| missing vs foreign | 保持现状 / non-admin 同码 | non-admin 同为 `FORBIDDEN`，admin missing 保留 `NOT_FOUND` |
| 测试层级 | 只测 Service / 只测 Controller / 两者结合 | Controller 测 Bearer 行为，Service 测 role facts 行为和业务规则 |

## 3. Final Scope

Included:

- Evaluation Set Controller roles-first RBAC。
- Evaluation Run Controller roles-first RBAC。
- Evaluation Set/Run Service 显式 role facts 授权。
- Bearer spoof / role-confusion / foreign/missing 防枚举测试。
- 既有 response redaction 测试保持通过。

Excluded:

- API path / DTO 变更。
- DB schema / Flyway 变更。
- Maven dependency 变更。
- RAG KB management full RBAC。
- Assessment/GradingEvaluation 其他 legacy caller 迁移。
- Formal OAuth2/JWK/Spring Security。

## 4. Architecture Drift Check Before Implementation

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只提取 HTTP/current user，授权决策仍在 Service |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime |
| Security | PASS | 权限在后端代码中执行；不新增依赖；不写 secrets |
| API / Database | PASS | API/DB contract 不变 |

## 5. Go / No-Go

Go.

Confidence: 0.95

依据：

- 已定位唯一实现路径和 legacy root cause。
- 已有 `PromptVersionController` roles-first 模式可复用。
- 不需要外部文档、OSS 参考或新增依赖。
- 风险和验证命令明确。

