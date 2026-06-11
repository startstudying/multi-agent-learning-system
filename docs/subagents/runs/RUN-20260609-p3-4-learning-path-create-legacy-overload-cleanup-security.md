# RUN-20260609 P3-4 LearningPath create legacy overload cleanup - Security

## 风险评级

MEDIUM。当前 HTTP 主路径已使用 roles-first facts，没有直接可利用的 `USER sub=admin` 提权；但 public 两参 service overload 若被未来入口误用，会升级为 HIGH Broken Access Control 风险。

## 结论

首选删除两参 overload 和 `isAdmin(String)` helper。保留 public legacy surface 会给后续开发留下 subject-name role confusion 误用入口。

## 必须保护的 RBAC 行为

- explicit `ADMIN` role 可为其他 learner 创建 LearningPath。
- explicit `ADMIN` role 可绕过 course-bound enrollment。
- Bearer `USER sub=admin` 不可获得 admin 语义。
- spoofed `X-User-Id: admin` 不可覆盖 Bearer token subject/roles。
- 非 admin 只能为自己创建。
- course-bound `goalId` 仍要求 learner active enrollment，除非 explicit admin。
- template goal 继续兼容。

## 不应扩大的范围

- 不改 ResourceGeneration、Agent Trace、Review Gate、Assessment、Analytics 的其他 legacy helper。
- 不改 REST path、DTO、schema、frontend。
- 不做 OAuth2/JWK/Spring Security 正式迁移。
- 不新增依赖。
