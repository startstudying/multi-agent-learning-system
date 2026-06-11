# RUN-20260609 P3-4 formal OAuth2/JWK/Spring Security - Architect

## Verdict

L-lite.

## Key Guidance

- This is a security architecture and dependency change because it introduces Spring Security / OAuth2 Resource Server and changes the global authentication boundary.
- Keep the delivery small: replace authentication source, do not change business authorization semantics.
- Preserve `CurrentUserService` public API so completed roles-first controller/service work remains stable.
- Production/preview should use Spring Security for Bearer JWT validation; dev/test may retain `X-User-Id` fallback only when no Bearer is present.
- Add dependency review before modifying `pom.xml`.

## Boundaries

Allowed:

- `common/auth`
- `AuthProperties`
- `application.yml`
- auth-focused tests and representative adjacent controller tests

Forbidden:

- business permission services
- API/DTO/schema/frontend changes
- real secrets or private JWK material

