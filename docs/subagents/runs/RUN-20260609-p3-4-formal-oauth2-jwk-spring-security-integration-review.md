# RUN-20260609 P3-4 formal OAuth2/JWK/Spring Security - Integration Review

## Integrated Decision

Proceed as L-lite.

## Rationale

All expert reports agree that continuing to enhance the hand-written HS256 filter would not satisfy the P3-4 formal OAuth2/JWK/Spring Security objective. The implementation must introduce Spring Security OAuth2 Resource Server while limiting the scope to authentication boundary replacement.

## Constraints

- Do not change business object authorization.
- Do not change API/DTO/DB/frontend.
- Do not store secrets or private JWK material.
- Keep `CurrentUserService` as the stable business-facing abstraction.

