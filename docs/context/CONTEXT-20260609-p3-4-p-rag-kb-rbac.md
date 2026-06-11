# CONTEXT - P3-4-P RAG KB management roles-first RBAC

## 1. Related Memory and Docs

- `AGENTS.md`
- `.cursor/rules/00-project-memory-rule.mdc`
- `.cursor/rules/01-skill-selection-rule.mdc`
- `.cursor/rules/02-subagent-parallel-rule.mdc`
- `.cursor/rules/04-workflow-rule.mdc`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/rag-hybrid-retrieval.md`
- `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
- `docs/specs/SPEC-20260608-rag-document-course-scope.md`
- `docs/specs/SPEC-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/specs/SPEC-20260609-p3-4-permission-matrix.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-backend.md`
- `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-security.md`
- `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-test.md`
- `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-integration-review.md`

## 2. Selected Skills

- `feature-development-workflow`
- `auth-context-boundary`
- `object-scope-authorization`
- `rag-hybrid-retrieval`
- `spring-boot-architecture`
- `security-review`
- `test-generator`
- `subagent-driven-development`
- `verification-before-completion`

## 3. Subagent Plan

Used: Yes.

Mode: L1 parallel analysis only.

Experts:

- Backend Expert / Spec Architect
- Security & Quality
- Test Engineer
- Integration Reviewer: Main Codex

No parallel implementation.

## 4. Files Allowed To Modify

- `backend/src/main/java/com/learningos/rag/api/KnowledgeBaseController.java`
- `backend/src/main/java/com/learningos/rag/api/DocumentController.java`
- `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java`
- `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java`
- `docs/product/PRD-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- Parser/vector/index worker/storage/provider/model runtime。
- Any unrelated backend modules outside RAG KB management / auth-adjacent tests.

## 6. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest test
mvn --% -Dtest=PermissionServiceTest test
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,ChatControllerTest,RagEvaluationControllerTest test
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
mvn test
```

## 7. Current Task Boundary

Only implement P3-4-P. Do not mark P3-4 as complete. Remaining P3-4 items include broader class/course authorization, formal OAuth2/JWK/Spring Security, broader permission penetration tests, and other legacy caller migrations.

## 8. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller extracts identity facts; Service enforces authorization |
| Frontend rules | PASS | No frontend change |
| Agent / RAG rules | PASS | No Agent/RAG runtime change; management authorization only |
| Security | PASS | No secrets; no dependency |
| API / Database | PASS | No API/DB contract change |
