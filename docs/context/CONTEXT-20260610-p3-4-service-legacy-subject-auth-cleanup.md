# CONTEXT-20260610 P3-4 子任务：Service legacy subject-name authorization cleanup

## 1. Current Task Boundary

本任务只清理服务层 legacy subject-name authorization 入口，不改变用户可见产品行为。

Parent: `P3-4 权限与安全加固`

Semantic child task:

```text
P3-4 子任务：Service legacy subject-name authorization cleanup
```

## 2. Related Memory and Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/specs/SPEC-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/tasks/TASK-20260609-p3-4-learning-path-create-legacy-overload-cleanup.md`
- `docs/specs/SPEC-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`
- `docs/specs/SPEC-20260610-p3-4-sse-production-auth-strategy.md`

## 3. Selected Skills

- `feature-development-workflow`
- `subagent-driven-development`
- `test-driven-development`
- `security-review`
- `auth-context-boundary`
- `object-scope-authorization`
- `verification-before-completion`

## 4. Subagent Plan

Use subagents: Yes

Parallelism level: L1 Parallel Analysis

| Expert | Scope | File Output |
|---|---|---|
| Security Reviewer | 必须删除/保留的签名、安全边界、风险 | `docs/subagents/runs/RUN-20260610-p3-4-service-legacy-subject-auth-cleanup-security.md` |
| Test Engineer | 反射守卫、邻接矩阵、测试命令 | `docs/subagents/runs/RUN-20260610-p3-4-service-legacy-subject-auth-cleanup-test.md` |

Subagents are read-only for this task. Main Codex owns final implementation.

## 5. Files Allowed To Modify

- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `backend/src/test/java/com/learningos/knowledge/application/CourseAccessServiceTest.java`
- `backend/src/test/java/com/learningos/assessment/application/AssessmentServiceTest.java`
- `backend/src/test/java/com/learningos/assessment/application/GradingEvaluationServiceTest.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `docs/requirements/REQ-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/specs/SPEC-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/plans/PLAN-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/tasks/TASK-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/context/CONTEXT-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/subagents/runs/RUN-20260610-p3-4-service-legacy-subject-auth-cleanup-*.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/acceptance/ACCEPT-20260610-p3-4-service-legacy-subject-auth-cleanup.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 6. Files Not Allowed To Modify

- `frontend/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- Spring Security / OAuth2 / JWK 配置
- RAG parser / VectorDB / Model gateway / Review Gate / ResourceGeneration 非目标模块
- secret、credential、local env 文件

## 7. Test Commands

Focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseAccessServiceTest,AssessmentServiceTest,GradingEvaluationServiceTest test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,CourseAccessServiceTest,AssessmentControllerTest,AssessmentServiceTest,GradingEvaluationServiceTest,AnalyticsControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,RagQueryServiceTest,LearningWorkflowControllerTest test
```

Full:

```powershell
cd D:\多元agent\backend
mvn test
```

## 8. Current Evidence Before Implementation

- `rg` 显示目标 legacy overload 主要残留在目标 Service 自身。
- Controller main path 已调用 roles-first 签名。
- 最近相邻权限测试证据：204 run, 0 failures, 0 errors, 0 skipped（来自本任务前专家交接）。
- 最近 SSE focused 证据：10 run, 0 failures, 0 errors, 0 skipped。

## 9. Boundary Notes

- `GradingEvaluationService.evaluate(GradingEvaluationRequest request)` 是纯算法/兼容 metric 入口，必须保留。
- `GradingEvaluationService.evaluate(List<Double>, List<Double>, double)` 是纯算法入口，必须保留。
- P3-4 父项不在本任务关闭；仍保留 broader class/course matrix、answer-record expansion、dev/test legacy fallback cleanup、frontend production streaming client / sensitive SSE URL cleanup。
