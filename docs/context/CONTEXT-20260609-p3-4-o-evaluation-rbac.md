# CONTEXT - P3-4-O Evaluation Set / Run roles-first RBAC

## 1. Related Memory and Docs

- `AGENTS.md`
- `.cursor/rules/00-project-memory-rule.mdc`
- `.cursor/rules/01-skill-selection-rule.mdc`
- `.cursor/rules/02-subagent-parallel-rule.mdc`
- `.cursor/rules/04-workflow-rule.mdc`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
- `docs/specs/SPEC-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/specs/SPEC-20260606-evaluation-set-management.md`
- `docs/specs/SPEC-20260606-prompt-version-quality-comparison.md`
- `docs/subagents/runs/RUN-20260609-p3-4-o-evaluation-rbac.md`
- `docs/subagents/runs/RUN-20260609-p3-4-o-integration-review.md`

## 2. Selected Skills

- `feature-development-workflow`
- `test-driven-development`
- `auth-context-boundary`
- `object-scope-authorization`
- `spring-boot-architecture`
- `test-generator`
- `security-review`
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

- `backend/src/test/java/com/learningos/evaluation/api/EvaluationSetControllerTest.java`
- `backend/src/test/java/com/learningos/evaluation/api/EvaluationRunControllerTest.java`
- `backend/src/test/java/com/learningos/evaluation/application/EvaluationSetServiceTest.java`
- `backend/src/test/java/com/learningos/evaluation/application/EvaluationRunServiceTest.java`
- `backend/src/main/java/com/learningos/evaluation/api/EvaluationSetController.java`
- `backend/src/main/java/com/learningos/evaluation/api/EvaluationRunController.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationSetService.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java`
- `docs/product/PRD-20260609-p3-4-o-evaluation-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-o-evaluation-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-o-evaluation-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-o-evaluation-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-o-evaluation-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-o-evaluation-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-o-evaluation-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-o-evaluation-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-o-evaluation-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- Any unrelated backend modules outside Evaluation/auth-adjacent tests.

## 6. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,PromptVersionControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
mvn test
```

## 7. Current Task Boundary

Only implement P3-4-O. Do not mark P3-4 as complete. Remaining P3-4 items include broader class/course authorization, RAG KB management full RBAC, formal OAuth2/JWK/Spring Security, and additional legacy caller migration.

## 8. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller extracts identity; Service enforces authorization |
| Frontend rules | PASS | No frontend change |
| Agent / RAG rules | PASS | No Agent/RAG runtime change |
| Security | PASS | No secrets; no dependency |
| API / Database | PASS | No API/DB contract change |

