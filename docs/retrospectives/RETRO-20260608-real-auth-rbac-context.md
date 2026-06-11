# Retrospective - P3-4-I 真实认证上下文与 RBAC 兼容层

## 1. Feature Summary

本切片将后端认证上下文从纯开发态 `X-User-Id` 过渡为 Bearer JWT 优先：生产/预发环境不再信任 `X-User-Id`，dev/test 保留兼容 fallback，`CurrentUserService` 的 admin/teacher 判断优先使用 roles。

## 2. What Went Well

- TDD 很直接：新测试先卡住缺失的 `AuthProperties`、`currentUser()` 和环境感知角色判断，再实现最小代码。
- 没有新增 Maven 依赖，也没有引入 Spring Security/OAuth2 迁移风险。
- `DevAuthFilter` 保留类名但升级行为，减少现有 Spring Boot 测试上下文改动。
- P3-4-C..H 相邻回归和全量 Maven 测试都通过，说明对象级授权矩阵没有明显回退。

## 3. What Didn't Go Well

- `DevAuthFilter` 类名已经不再准确，短期保留是为了降低集成风险；后续正式认证切片可重命名为 `AuthContextFilter`。
- 过渡 JWT 校验代码承担了 JDK/Jackson 层面的解析细节，后续接入 OAuth2 Resource Server 或 JWK 时应替换为成熟组件。
- 初始 Context Pack 未列出 `application.yml`，补 env 映射时需要同步扩展允许文件边界。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 认证上下文边界：Bearer 优先、invalid 不 fallback、prod 禁用开发身份头、roles 优先判断。 | Yes | `docs/skills/project-specific/auth-context-boundary.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Context Pack | 初始只列 Java auth/config 文件。 | 配置类新增时同时检查 `application.yml` 是否需要纳入允许边界。 |
| TDD | RED 是编译失败，证明新增合同缺失。 | 继续接受编译级 RED，但 Evidence 中必须说明失败点和预期合同。 |
| 命名 | 保留 `DevAuthFilter` 兼容现有引用。 | 后续正式认证切片中安排重命名和迁移测试。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 创建 `auth-context-boundary` 项目技能并注册。 | Main Codex | 本切片完成 |
| 后续正式认证 provider / Spring Security / JWK 需要依赖审查。 | 后续切片 | P3-4 follow-up |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] API_MEMORY.md
- [x] CHANGELOG.md
- [x] SKILL_REGISTRY.md
