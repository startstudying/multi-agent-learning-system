# Dependency Review - P3-4 formal OAuth2/JWK/Spring Security

## Dependencies

| Name | Version | Package Manager | Scope | Added By |
|---|---|---|---|---|
| `spring-boot-starter-security` | Spring Boot managed `3.5.7` | Maven | runtime | TASK-20260609-p3-4-formal-oauth2-jwk-spring-security |
| `spring-boot-starter-oauth2-resource-server` | Spring Boot managed `3.5.7` | Maven | runtime | TASK-20260609-p3-4-formal-oauth2-jwk-spring-security |
| `spring-security-test` | Spring Boot managed `3.5.7` | Maven | test | TASK-20260609-p3-4-formal-oauth2-jwk-spring-security |

## Justification

The current backend uses a transitional hand-written HS256 Bearer token filter. The P3-4 TODO explicitly leaves formal OAuth2/JWK/Spring Security as follow-up work. Spring Security OAuth2 Resource Server provides standard JWT validation, JWK Set URI support, issuer validation, authority conversion, and integration with `SecurityContext`.

Official Spring Security docs state that JWT resource server support needs OAuth2 resource server plus JOSE/JWT support, and that Spring Boot can configure a resource server using `issuer-uri` or `jwk-set-uri`.

## Alternatives Considered

| Alternative | Pros | Cons | Decision |
|---|---|---|---|
| Keep hand-written HS256 filter | No new dependency, minimal test churn | Not formal OAuth2/JWK/Spring Security; no JWK rotation; target not satisfied | Rejected |
| Direct Nimbus/JJWT/Auth0 dependency | Full manual control | Reimplements Spring Security integration; more security surface | Rejected |
| Spring Security Resource Server | Standard Spring Boot path, JWK/JWT support, maintained | Global auth chain migration and test churn | Approved |

## License

| Field | Value |
|---|---|
| License | Apache License 2.0 |
| Compatible | Yes |

## Security

- [x] No known task-blocking critical CVEs identified in selected Spring Boot managed artifacts during this review.
- [x] Maintained actively by Spring project.
- [x] Trusted publisher.
- [x] Uses Spring Boot dependency management instead of unmanaged versions.

## Impact

- Bundle size impact: moderate; adds Spring Security and OAuth2/JWT/Jose transitive libraries.
- Transitive dependencies concern: Nimbus JOSE/JWT may be pulled transitively; use Boot-managed version.
- Runtime requirement: production must configure issuer/JWK or compatible JWT decoder settings.

## Approval

| Role | Date | Status |
|---|---|---|
| Main Codex | 2026-06-09 | APPROVED for implementation |

