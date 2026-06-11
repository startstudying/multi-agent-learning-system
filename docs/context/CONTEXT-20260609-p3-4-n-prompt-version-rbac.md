# CONTEXT-20260609 P3-4-N PromptVersion 管理 API RBAC 与 promptText 暴露收口

## 1. Current TASK

`TASK-20260609-p3-4-n-prompt-version-rbac.md`

目标：收口 PromptVersion 管理 API 的 roles-first RBAC，并让 `promptText` 仅对 admin 暴露。

## 2. Related Memory and Docs

- `AGENTS.md`
- `.cursor/rules/00-project-memory-rule.mdc`
- `.cursor/rules/01-skill-selection-rule.mdc`
- `.cursor/rules/02-subagent-parallel-rule.mdc`
- `.cursor/rules/04-workflow-rule.mdc`
- `.cursor/rules/10-context-pack-rule.mdc`
- `.cursor/rules/11-execution-rule.mdc`
- `.cursor/rules/12-acceptance-rule.mdc`
- `.cursor/rules/14-architecture-drift-rule.mdc`
- `.cursor/rules/16-backend-rule.mdc`
- `.cursor/rules/17-agent-rag-rule.mdc`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/specs/SPEC-20260605-prompt-version-management.md`
- `docs/specs/SPEC-20260606-model-call-prompt-metadata.md`
- `docs/acceptance/ACCEPT-20260606-model-call-prompt-metadata.md`
- `docs/specs/SPEC-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/planning/backend-architecture-todolist.md`

## 3. Selected Skills

| Skill | Reason |
|---|---|
| `feature-development-workflow` | 强制完整 Spec-first 流程 |
| `security-review` | 管理 API 鉴权与敏感 prompt 内容收口 |
| `auth-context-boundary` | Bearer roles-first、spoofed header 与 legacy inference 边界 |
| `object-scope-authorization` | 管理面访问控制与错误响应不泄漏 |
| `spring-boot-architecture` | Controller/Service 分层 |
| `api-contract-design` | 响应字段按角色脱敏 |
| `test-generator` / `test-driven-development` | RED/GREEN |
| `verification-before-completion` | 完成前验证证据 |

## 4. Subagent Plan

已完成 L1 并行分析：

- Security reviewer: `docs/subagents/runs/RUN-20260609-p3-4-n-next-security.md`
- Backend architect: `docs/subagents/runs/RUN-20260609-p3-4-n-next-backend.md`
- Test engineer: `docs/subagents/runs/RUN-20260609-p3-4-n-next-test.md`
- Integration review: `docs/subagents/runs/RUN-20260609-p3-4-n-integration-review.md`

Implementation mode: Single Codex。

## 5. Files Allowed To Modify

Production:

- `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java`
- `backend/src/main/java/com/learningos/agent/application/PromptVersionService.java`
- `backend/src/main/java/com/learningos/agent/dto/PromptVersionResponse.java`

Tests:

- `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java`
- `backend/src/test/java/com/learningos/agent/application/PromptVersionServiceTest.java`

Docs:

- `docs/product/PRD-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/subagents/runs/RUN-20260609-p3-4-n-next-backend.md`
- `docs/subagents/runs/RUN-20260609-p3-4-n-integration-review.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-n-prompt-version-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 6. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- RAG / Evaluation / Assessment / Analytics / Learning production modules

## 7. Test Commands

Focused:

```powershell
cd backend
mvn --% -Dtest=PromptVersionControllerTest test
```

Adjacent:

```powershell
cd backend
mvn --% -Dtest=PromptVersionControllerTest,PromptVersionServiceTest,DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
```

Full:

```powershell
cd backend
mvn test
```

## 8. Current Task Boundary

只修 PromptVersion HTTP 管理面：

- roles-first admin-only write。
- roles-first admin/teacher read。
- teacher metadata-only read。
- Bearer spoofing / role confusion 防回归。

不处理：

- Prompt 审批流。
- Evaluation/GradingEvaluation/RAG KB。
- formal OAuth2/JWK/Spring Security。
- DB schema 或 frontend。

