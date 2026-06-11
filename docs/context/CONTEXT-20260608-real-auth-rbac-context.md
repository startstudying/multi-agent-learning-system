# CONTEXT-20260608 P3-4-I 真实认证上下文与 RBAC 兼容层

## 1. Current Task Boundary

本切片只替换认证上下文根：支持后端验证 Bearer JWT，在 prod/staging/production 禁用 `X-User-Id` 身份建立，在 dev/test 保持兼容。业务对象级授权矩阵不重写。

## 2. Related Memory And Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/subagents/runs/RUN-20260608-p3-next-security-rbac.md`
- `docs/subagents/runs/RUN-20260608-p3-next-rag-parser-architect.md`
- `docs/subagents/runs/RUN-20260608-p3-next-model-provider.md`
- `docs/subagents/runs/RUN-20260608-real-auth-rbac-context-integration.md`
- `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 3. Selected Skills

- `feature-development-workflow`
- `multi-agent-coder`
- `dispatching-parallel-agents`
- `security-review`
- `spring-boot-architecture`
- `object-scope-authorization`
- `test-driven-development`
- `verification-before-completion`

## 4. Subagent Plan

| Subagent | Scope | Output |
|---|---|---|
| RAG Parser Architect | P3-2 parser/OCR/page hierarchy | `RUN-20260608-p3-next-rag-parser-architect.md` |
| Model Provider Expert | P3-3 Spring AI provider | `RUN-20260608-p3-next-model-provider.md` |
| Security & Quality | P3-4 JWT/RBAC | `RUN-20260608-p3-next-security-rbac.md` |
| Integration Reviewer | Main Codex 合并 | `RUN-20260608-real-auth-rbac-context-integration.md` |

## 5. Allowed Files

- `backend/src/main/java/com/learningos/LearningOsApplication.java`
- `backend/src/main/java/com/learningos/config/AppProperties.java`
- `backend/src/main/java/com/learningos/config/AuthProperties.java`
- `backend/src/main/java/com/learningos/common/auth/**`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/learningos/common/auth/**`
- `backend/src/test/java/com/learningos/common/trace/StructuredRequestLoggingFilterTest.java`
- `docs/planning/backend-architecture-todolist.md`
- `docs/context/CONTEXT-20260608-real-auth-rbac-context.md`
- `docs/evidence/EVIDENCE-20260608-real-auth-rbac-context.md`
- `docs/acceptance/ACCEPT-20260608-real-auth-rbac-context.md`
- `docs/retrospectives/RETRO-20260608-real-auth-rbac-context.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/README.md`

## 6. Files Not Allowed

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- unrelated RAG parser / model provider / assessment / resource / course business implementation files

## 7. Test Commands

```powershell
cd backend
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest test
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,StructuredRequestLoggingFilterTest,CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest test
mvn test
```

## 8. Architecture Drift Checklist

| Check | Expected |
|---|---|
| Controller only delegates | PASS |
| Service object authorization unchanged | PASS |
| No new dependency | PASS |
| No schema drift | PASS |
| No frontend LLM/API key change | PASS |
| Production no longer trusts `X-User-Id` | PASS |
