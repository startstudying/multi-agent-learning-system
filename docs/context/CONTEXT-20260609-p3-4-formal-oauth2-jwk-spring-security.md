# CONTEXT-20260609 P3-4 子任务：formal OAuth2/JWK/Spring Security

## Related Memory and Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/security/DEPENDENCY-REVIEW-20260609-p3-4-formal-oauth2-jwk-spring-security.md`
- Spring Security official docs: `https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html`

## Selected Skills

- `feature-development-workflow`
- `subagent-driven-development`
- `security-review`
- `auth-context-boundary`
- `test-driven-development`
- `verification-before-completion`

## Subagent Plan

| Expert | Output |
|---|---|
| Architect | L-lite scope, allowed/forbidden files, dependency boundary. |
| Security Reviewer | Authentication risk matrix and minimum security acceptance. |
| Test Engineer | RED/focused/adjacent test strategy. |
| Integration Reviewer | Merge expert guidance after implementation. |

## Files Allowed To Modify

Production:

- `backend/pom.xml`
- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`
- `backend/src/main/java/com/learningos/common/auth/UserContext.java`
- `backend/src/main/java/com/learningos/common/auth/UserContextHolder.java`
- `backend/src/main/java/com/learningos/common/auth/SecurityConfig.java`
- `backend/src/main/java/com/learningos/common/auth/ApiAuthenticationEntryPoint.java`
- `backend/src/main/java/com/learningos/common/auth/ApiAccessDeniedHandler.java`
- `backend/src/main/java/com/learningos/common/auth/JwtUserContextConverter.java`
- `backend/src/main/java/com/learningos/config/AuthProperties.java`
- `backend/src/main/resources/application.yml`

Tests:

- `backend/src/test/java/com/learningos/common/auth/CurrentUserServiceTest.java`
- `backend/src/test/java/com/learningos/common/auth/DevAuthFilterTest.java`
- `backend/src/test/java/com/learningos/common/auth/SecurityJwtAuthenticationTest.java`
- `backend/src/test/java/com/learningos/common/auth/SecurityFilterChainTest.java`
- selected adjacent controller tests only if token helper migration is necessary.
- full backend regression-only MVC slice tests if Spring Security auto-configuration prevents the target controller/filter/exception logic from executing:
  - `backend/src/test/java/com/learningos/common/exception/GlobalExceptionHandlerTest.java`
  - `backend/src/test/java/com/learningos/common/trace/StructuredRequestLoggingFilterTest.java`
  - `backend/src/test/java/com/learningos/health/api/HealthControllerTest.java`

Docs:

- `docs/product/PRD-20260609-p3-4-formal-oauth2-jwk-spring-security.md`
- `docs/requirements/REQ-20260609-p3-4-formal-oauth2-jwk-spring-security.md`
- `docs/specs/SPEC-20260609-p3-4-formal-oauth2-jwk-spring-security.md`
- `docs/plans/PLAN-20260609-p3-4-formal-oauth2-jwk-spring-security.md`
- `docs/tasks/TASK-20260609-p3-4-formal-oauth2-jwk-spring-security.md`
- `docs/context/CONTEXT-20260609-p3-4-formal-oauth2-jwk-spring-security.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-formal-oauth2-jwk-spring-security.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-formal-oauth2-jwk-spring-security.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/project-specific/auth-context-boundary.md`

## Files Not Allowed To Modify

- `frontend/**`
- database migration files
- business authorization services such as `CourseAccessService`, `AssessmentService`, `DocumentService`, `LearningWorkflowService`, `ResourceGenerationService`
- Agent/RAG/model/parser/vector/index worker runtime
- API DTOs

## Current Task Boundary

Only replace the authentication root boundary. Do not change object-level authorization semantics.

## Boundary Adjustment During Verification

Full backend verification exposed `@WebMvcTest` slice regressions caused by newly added Spring Security auto-configuration intercepting exception/logging/health slice tests before their target controllers/filters executed. The fix is limited to excluding security auto-configuration in those slice tests; production `SecurityFilterChain` behavior remains covered by `SecurityFilterChainTest`.
