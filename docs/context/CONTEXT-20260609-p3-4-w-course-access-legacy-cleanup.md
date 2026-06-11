# CONTEXT - P3-4-W CourseAccessService legacy overload cleanup

## 1. Current Task Boundary

本切片只删除 `CourseAccessService` 的 legacy subject-name role inference API 面。

不处理：
- `KnowledgeCatalogService` 自身 legacy overload。
- `LearningWorkflowService.getPathForUser` 的本地 `admin` subject-name 判断。
- `AssessmentService` / `GradingEvaluationService` 等其他业务 service legacy overload。
- formal OAuth2/JWK/Spring Security。

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/specs/SPEC-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/specs/SPEC-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-v-resource-trace-detail-rbac.md`

## 3. Selected Skills

- `feature-development-workflow`
- `auth-context-boundary`
- `object-scope-authorization`
- `test-driven-development`
- `security-review`

## 4. Subagent Plan

| Role | Report |
|---|---|
| Architect | `docs/subagents/runs/RUN-20260609-p3-4-w-course-access-legacy-cleanup-architect.md` |
| Security Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-w-course-access-legacy-cleanup-security.md` |
| Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-w-course-access-legacy-cleanup-test.md` |
| Integration Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-w-course-access-legacy-cleanup-integration-review.md` |

Implementation mode: Main Codex single implementation after L1 parallel analysis.

## 5. Files Allowed To Modify

- `backend/src/test/java/com/learningos/knowledge/application/CourseAccessServiceTest.java`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
- P3-4-W workflow/evidence/acceptance/retro docs
- `docs/subagents/runs/RUN-20260609-p3-4-w-course-access-legacy-cleanup-*.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 6. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `CurrentUserService` / `DevAuthFilter`
- formal OAuth2/JWK/Spring Security config
- Agent/RAG/model provider runtime
- other service legacy cleanup outside `CourseAccessService`

## 7. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseAccessServiceTest test
mvn --% -DskipTests compile
mvn --% -Dtest=CourseAccessServiceTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,AssessmentControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,DocumentControllerTest test
mvn test
```

## 8. Boundary Notes

- `CourseAccessService` roles-first overload behavior不得改变。
- 若删除旧签名后出现编译失败，必须把调用方迁移到 roles-first，而不是恢复 legacy subject-name inference。
- P3-4-W 完成后，P3-4 仍保留 broader class/course authorization、formal OAuth2/JWK/Spring Security、broader penetration tests。
