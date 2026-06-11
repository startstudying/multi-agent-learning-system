# CONTEXT-20260608 后端 P3 生产化集成总控

## 1. Current Task

P3-4-C 权限矩阵安全前置：课程读取 / 知识图谱读取 / 评分评估接口权限收口。

## 2. Related Memory and Docs

- `AGENTS.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/skills/project-specific/object-scope-authorization.md`

## 3. Selected Skills

- `feature-development-workflow`
- `multi-agent-coder`
- `subagent-driven-development`
- `spring-boot-architecture`
- `api-contract-design`
- `security-review`
- `object-scope-authorization`
- `test-generator`
- `architecture-drift-check`
- `test-driven-development`
- `verification-before-completion`

## 4. Subagent Plan

| Expert | Status | Boundary |
|---|---|---|
| Security & Quality | Completed | P3-4 权限缺口与测试矩阵 |
| Integration Reviewer | Completed | 总控切片顺序与验收清单 |
| RAG Parser Expert | Completed | P3-2 后续切片 |
| Model Gateway Expert | Completed | P3-3 后续切片 |

Implementation mode：当前 TASK 由主 Codex 串行实现；不并行修改同一文件。

## 5. Files Allowed To Modify

- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/main/java/com/learningos/knowledge/repository/CourseRepository.java`
- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `docs/subagents/runs/RUN-20260608-backend-p3-productionization-*.md`
- `docs/product/PRD-20260608-*.md`
- `docs/requirements/REQ-20260608-*.md`
- `docs/specs/SPEC-20260608-*.md`
- `docs/plans/PLAN-20260608-*.md`
- `docs/tasks/TASK-20260608-*.md`
- `docs/context/CONTEXT-20260608-*.md`
- `docs/evidence/EVIDENCE-20260608-rbac-course-class-answer-matrix.md`
- `docs/acceptance/ACCEPT-20260608-rbac-course-class-answer-matrix.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`

## 6. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `backend/src/main/java/com/learningos/rag/parser/**`
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`

## 7. Test Commands

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test
mvn test
```

## 8. Boundary

当前切片只收口已有接口权限，不新增数据库 schema、不新增依赖、不实现 JWT、不新增 answer detail API、不改变前端。
