# CONTEXT-20260609 P3-4-K 权限渗透测试矩阵补齐

## 1. Current Task Boundary

本切片只补齐 P3-4 权限渗透测试矩阵的当前 transitional scope。默认只新增测试；若 RED 暴露明确生产缺陷，仅允许最小修复对应文件。

不得引入正式 OAuth2/JWK/Spring Security，不做 broader class/course domain，不新增依赖，不改 schema，不改前端。

## 2. Related Memory And Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-security.md`
- `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-test.md`
- `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-architecture.md`

## 3. Selected Skills

- `feature-development-workflow`
- `security-review`
- `object-scope-authorization`
- `auth-context-boundary`
- `test-driven-development`
- `verification-before-completion`

## 4. Subagent Plan

| Subagent | Role | Output |
|---|---|---|
| Socrates | Security & Quality；第一个专家，按用户要求使用 `gpt-5.5` | `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-security.md` |
| Aristotle | Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-test.md` |
| Russell | Backend Architecture Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-architecture.md` |

Implementation mode: Single Codex implementation。

## 5. Files Allowed To Modify

Test files:

- `backend/src/test/java/com/learningos/security/api/PermissionMatrixControllerTest.java`
- `backend/src/test/java/com/learningos/common/auth/DevAuthFilterTest.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`

Conditional production file, only if RED proves current implementation violates spec:

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`

Docs:

- `docs/product/PRD-20260609-p3-4-permission-matrix.md`
- `docs/requirements/REQ-20260609-p3-4-permission-matrix.md`
- `docs/specs/SPEC-20260609-p3-4-permission-matrix.md`
- `docs/plans/PLAN-20260609-p3-4-permission-matrix.md`
- `docs/tasks/TASK-20260609-p3-4-permission-matrix.md`
- `docs/context/CONTEXT-20260609-p3-4-permission-matrix.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-permission-matrix.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-permission-matrix.md`
- `docs/retrospectives/RETRO-20260609-p3-4-permission-matrix.md`
- `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-security.md`
- `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-test.md`
- `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-architecture.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 6. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- RAG parser/model provider/vector implementation files
- Any production authorization/business file not explicitly listed above

## 7. Test Commands

```powershell
cd backend
mvn --% -Dtest=PermissionMatrixControllerTest,DevAuthFilterTest,AnalyticsControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest,DevAuthFilterTest test
mvn test
```

## 8. Current Task Boundary

当前任务只补权限矩阵测试和由 RED 证明的最小修复。不要处理：

- formal OAuth2/JWK/Spring Security；
- broader class/course；
- PromptVersion / Evaluation full RBAC；
- RAG parser/OCR/VectorDB；
- model provider；
- frontend。

## 9. Architecture Drift Checklist

| Check | Expected |
|---|---|
| Controller only handles HTTP | PASS |
| Service owns object authorization | PASS |
| No frontend LLM/API key change | PASS |
| No Agent tool repository access change | PASS |
| Retrieval permission filtering unchanged or tested | PASS |
| No new dependency | PASS |
| No schema drift | PASS |
