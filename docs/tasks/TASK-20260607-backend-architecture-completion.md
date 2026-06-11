# TASK-20260607 后端架构 TODO 完成计划

## Task 3：P3-5-A 运维告警 API

Status: Done

### Done Criteria

- [x] `GET /api/analytics/ops/alerts` 存在并返回统一 `ApiResponse`。
- [x] 非 admin 和缺失 `X-User-Id` 返回 `FORBIDDEN`。
- [x] admin 无告警时返回默认 thresholds 和空 `alerts`。
- [x] 慢 RAG 查询 `latencyMs >= slowQueryMs` 触发 `SLOW_RAG_QUERY`。
- [x] 慢模型调用 `latencyMs >= slowModelMs` 触发 `SLOW_MODEL_CALL`。
- [x] RAG 无来源数量与比例同时达标触发 `RAG_NO_SOURCE`。
- [x] 审核积压数量和年龄达标触发 `REVIEW_BACKLOG`。
- [x] 响应不包含 `prompt`、`question`、`responseJson`、`sourcesJson`、`errorMessage`、`markdownContent`、`citationCheck`、`revisionSuggestion` 或敏感 seed 内容。
- [x] 非法参数返回 `VALIDATION_ERROR`。
- [x] 不新增依赖、不改 DB schema、不改前端。
- [x] Evidence / Acceptance / Memory / Changelog 更新。

### Allowed Files

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion-alerting-observability.md`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion-alerting-security.md`
- `docs/subagents/runs/RUN-20260607-backend-architecture-completion-alerting-integration.md`
- `docs/evidence/EVIDENCE-20260607-backend-architecture-completion.md`
- `docs/acceptance/ACCEPT-20260607-backend-architecture-completion.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/ops-alerting.md`
- `docs/retrospectives/RETRO-20260607-backend-architecture-completion-ops-alerting.md`
- `docs/tasks/TASK-20260607-backend-architecture-completion.md`
- `docs/context/CONTEXT-20260607-backend-architecture-completion.md`

### Test Commands

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,StructuredRequestLoggingFilterTest,HealthServiceTest,HealthControllerTest,RagQueryServiceTest,ResourceReviewControllerTest test
mvn test
```

## Task 1：P3-4-A Course / Knowledge Catalog 权限收口

Status: Done

### Done Criteria

- [x] student 创建 course 返回 403。
- [x] teacher 创建 course 时保存 `teacherId=currentUserId`。
- [x] teacher 不可为其他 teacher 创建 course。
- [x] foreign teacher 不可新增 chapter。
- [x] foreign teacher 不可新增 knowledge point。
- [x] foreign teacher 不可新增 dependency。
- [x] admin 可管理任意 course graph。
- [x] 聚焦测试通过。
- [x] 相关学习路径回归测试通过。
- [x] Evidence / Acceptance 更新。

### Allowed Files

- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java`
- `backend/src/main/java/com/learningos/knowledge/api/KnowledgePointController.java`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `docs/evidence/EVIDENCE-20260607-backend-architecture-completion.md`
- `docs/acceptance/ACCEPT-20260607-backend-architecture-completion.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## Task 2：P3-4-B 对象详情防枚举与 scoped authorization

Status: Done

### Done Criteria

- [x] foreign path 与 missing path 对非 admin 均返回 `FORBIDDEN` 且无 `data`。
- [x] foreign resource generation task 与 missing task 对非 admin 均返回 `FORBIDDEN` 且无 `data`。
- [x] foreign agent trace 与 missing trace task 对非 admin 均返回 `FORBIDDEN` 且无 `data`。
- [x] foreign RAG document / index task 与 missing document / index task 对非 admin 均返回 `FORBIDDEN` 且无 `data`。
- [x] 聚焦测试通过。
- [x] 相邻权限/RAG/资源生成回归测试通过。
- [x] 完整后端 `mvn test` 通过。
- [x] Evidence / Acceptance 更新。

### Allowed Files

- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/agent/application/AgentTraceGovernanceService.java`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `docs/evidence/EVIDENCE-20260607-backend-architecture-completion.md`
- `docs/acceptance/ACCEPT-20260607-backend-architecture-completion.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/tasks/TASK-20260607-backend-architecture-completion.md`

## Later Tasks

- [x] Task 3：P3-5-A 慢查询/慢模型/无引用/审核积压告警。
- [x] Task 4：P3-3-A 模型网关结构化校验与日志补齐。（见 `TASK-20260608-model-gateway-boundary.md`）
- [ ] Task 5：P3-3-B Spring AI provider 接入。
- [x] Task 6：P3-2-A Embedding service / VectorDB adapter 边界（noop，无新依赖）。
- [x] Task 7：P3-2-B Hybrid retrieval / RRF / reranker fallback。（见 `TASK-20260608-rag-hybrid-retrieval.md`）
- [ ] Task 8：P3-2-C 复杂 PDF/DOCX/OCR fallback。
