# RUN-20260609 P3-4 formal OAuth2/JWK/Spring Security - Security

## Verdict

Proceed after dependency review and RED tests.

## Current Risks

- Current Bearer token validation is a hand-written HS256 transitional filter.
- No JWK Set URI, key rotation, standard `JwtDecoder`, audience validation, or Spring Security filter chain.
- Issuer is optional in the transitional verifier.
- Legacy service-level role inference still exists in some compatibility paths, so the new authentication boundary must not reintroduce subject-name role facts.

## Minimum Acceptance

- prod/staging no Bearer returns `UNAUTHORIZED`.
- invalid / expired / wrong issuer token returns `UNAUTHORIZED` and never falls back.
- valid Bearer ignores spoofed `X-User-Id`.
- roles derive only from verified JWT claims and project whitelist.
- JWK / Spring Security `JwtDecoder` path exists.
- 401/403 responses are sanitized envelope responses.

## Follow-up Risks Not In This Slice

- SSE production auth needs a signed stream token or cookie/session strategy.
- Broader class/course authorization matrix remains open.

