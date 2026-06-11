# RUN-20260609 P3-4 formal OAuth2/JWK/Spring Security Debug Follow-up

## Role

Build / Debug Expert.

## Scope

只读复核 `AuthProperties` 绑定失败、`SecurityConfig` 启动风险、相关测试与 Maven 配置。

## Findings

- `AuthProperties` 是 `@ConfigurationProperties` record，但保留了额外二参构造器，Spring Boot 绑定无法唯一选择构造器，回退到无参构造后失败。
- 最小修复是显式标记 canonical constructor binding，或删除便利构造器并统一测试构造入口。
- 后续风险包括：
  - HS256 secret 需要满足 Nimbus/Spring Security 的 256-bit 最小长度。
  - `jwkSetUri` 非空时应优先走 JWK decoder。
  - `audience` 字段已绑定但原实现未校验。
  - prod/staging 无 token 的 401 行为需要真实 Spring Security filter chain 测试验证。

## Integration Result

主线已处理：

- `AuthProperties` canonical constructor 加 `@ConstructorBinding`，`devHeaderFallbackEnabled` 加 `@DefaultValue("true")`。
- 测试 HS256 fixture secret 统一改为 32 字节以上。
- `SecurityConfig` 增加 audience validator、production-like 缺少 JWK/secret fail-fast、JWK 分支测试。

